package com.buspay.app.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.Environment
import com.buspay.app.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val capturedAtMillis: Long
)

interface GpsTracker {
    fun start(onLocation: (GeoLocation) -> Unit): Boolean
    fun stop()
}

class AndroidGpsTracker(context: Context) : GpsTracker {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private var locationListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    override fun start(onLocation: (GeoLocation) -> Unit): Boolean {
        stop()
        val manager = locationManager ?: return false
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
        if (providers.isEmpty()) return false

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocation(
                    GeoLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        capturedAtMillis = location.time.takeIf { it > 0 }
                            ?: System.currentTimeMillis()
                    )
                )
            }
        }
        locationListener = listener
        val registeredProviders = providers.filter { provider ->
            runCatching {
                manager.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL_MILLIS,
                    LOCATION_UPDATE_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper()
                )
                manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
            }.isSuccess
        }
        if (registeredProviders.isEmpty()) {
            locationListener = null
            return false
        }
        return true
    }

    override fun stop() {
        val listener = locationListener ?: return
        runCatching { locationManager?.removeUpdates(listener) }
        locationListener = null
    }

    private companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 3_000L
        const val LOCATION_UPDATE_DISTANCE_METERS = 10f
    }
}

data class PrinterDevice(
    val name: String,
    val address: String
)

enum class ReceiptPaperProfile(
    val widthMillimeters: Int,
    val textColumns: Int,
    val pdfWidthPoints: Int
) {
    MM58(widthMillimeters = 58, textColumns = 32, pdfWidthPoints = 164),
    MM80(widthMillimeters = 80, textColumns = 48, pdfWidthPoints = 226)
}

data class PrintableTicket(
    val ticketCode: String,
    val busPlateNumber: String,
    val routeName: String,
    val fareName: String,
    val priceCents: Int,
    val soldAtText: String,
    val operatorName: String = "Unknown operator"
)

sealed interface PrintResult {
    data class Success(val outputPath: String? = null) : PrintResult
    data class Failure(val message: String) : PrintResult
}

interface TicketPrinter {
    fun pairedPrinters(): List<PrinterDevice>
    suspend fun printTicket(printer: PrinterDevice, ticket: PrintableTicket): PrintResult
}

class BluetoothEscPosTicketPrinter(private val context: Context) : TicketPrinter {
    private val bluetoothAdapter: BluetoothAdapter? = context
        .getSystemService(BluetoothManager::class.java)
        ?.adapter

    @SuppressLint("MissingPermission")
    override fun pairedPrinters(): List<PrinterDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices
            .map { device ->
                PrinterDevice(
                    name = device.name?.takeIf(String::isNotBlank)
                        ?: context.getString(R.string.bluetooth_printer),
                    address = device.address
                )
            }
            .sortedBy(PrinterDevice::name)
    }

    @SuppressLint("MissingPermission")
    override suspend fun printTicket(
        printer: PrinterDevice,
        ticket: PrintableTicket
    ): PrintResult = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext PrintResult.Failure(
                context.getString(R.string.bluetooth_unavailable)
            )
        if (!adapter.isEnabled) {
            return@withContext PrintResult.Failure(context.getString(R.string.bluetooth_off))
        }

        val socket = try {
            adapter
                .getRemoteDevice(printer.address)
                .createRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE_UUID)
        } catch (error: Exception) {
            return@withContext PrintResult.Failure(
                error.message ?: context.getString(R.string.printer_open_failed)
            )
        }

        try {
            socket.connect()
            socket.outputStream.use { output ->
                output.write(buildEscPosTicketBytes(ticket))
                output.flush()
            }
            PrintResult.Success()
        } catch (error: Exception) {
            PrintResult.Failure(
                error.message ?: context.getString(R.string.printer_rejected_ticket)
            )
        } finally {
            runCatching { socket.close() }
        }
    }

    private companion object {
        val SERIAL_PORT_PROFILE_UUID: UUID = UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )
    }
}

class PdfTicketPrinter(private val context: Context) : TicketPrinter {
    override fun pairedPrinters(): List<PrinterDevice> = listOf(
        TEST_DEVICE.copy(name = context.getString(R.string.pdf_test_printer_58mm)),
        TEST_DEVICE_80MM.copy(name = context.getString(R.string.pdf_test_printer_80mm))
    )

    override suspend fun printTicket(
        printer: PrinterDevice,
        ticket: PrintableTicket
    ): PrintResult = withContext(Dispatchers.IO) {
        val paperProfile = paperProfile(printer)
        if (paperProfile == null) {
            return@withContext PrintResult.Failure(context.getString(R.string.unknown_pdf_printer))
        }

        val outputDirectory = context
            .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?.resolve("tickets")
            ?: context.filesDir.resolve("tickets")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            return@withContext PrintResult.Failure(context.getString(R.string.pdf_folder_failed))
        }

        val safeTicketCode = ticket.ticketCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outputFile = File(
            outputDirectory,
            "ticket-$safeTicketCode-${paperProfile.widthMillimeters}mm.pdf"
        )

        try {
            writeTicketPdf(outputFile, ticket, paperProfile)
            PrintResult.Success(outputPath = outputFile.absolutePath)
        } catch (error: Exception) {
            PrintResult.Failure(error.message ?: context.getString(R.string.pdf_creation_failed))
        }
    }

    companion object {
        val TEST_DEVICE = PrinterDevice(
            name = "PDF Test Printer",
            address = "pdf://ticket-preview"
        )
        val TEST_DEVICE_80MM = PrinterDevice(
            name = "PDF Test Printer 80 mm",
            address = "pdf://ticket-preview/80mm"
        )

        fun isPdfTestDevice(printer: PrinterDevice): Boolean = paperProfile(printer) != null

        fun paperProfile(printer: PrinterDevice): ReceiptPaperProfile? = when (printer.address) {
            TEST_DEVICE.address -> ReceiptPaperProfile.MM58
            TEST_DEVICE_80MM.address -> ReceiptPaperProfile.MM80
            else -> null
        }
    }
}

private fun writeTicketPdf(
    outputFile: File,
    ticket: PrintableTicket,
    paperProfile: ReceiptPaperProfile
) {
    val document = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(
            paperProfile.pdfWidthPoints,
            PDF_PAGE_HEIGHT_POINTS,
            1
        ).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        var y = 24f

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("BUSPAY TRANSPORT", paperProfile.pdfWidthPoints / 2f, y, paint)
        y += 13f

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawText("TRANSPORT PUBLIK - DEMO", paperProfile.pdfWidthPoints / 2f, y, paint)
        y += 10f

        paint.strokeWidth = 1f
        canvas.drawLine(PDF_MARGIN, y, paperProfile.pdfWidthPoints - PDF_MARGIN, y, paint)
        y += 13f

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("BILETE UDHETIMI", paperProfile.pdfWidthPoints / 2f, y, paint)
        y += 12f
        paint.textSize = 9f
        canvas.drawText("KUPON TESTUES - JO FISKAL", paperProfile.pdfWidthPoints / 2f, y, paint)
        y += 14f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 7.5f
        val quantityColumnX = paperProfile.pdfWidthPoints * QUANTITY_COLUMN_RATIO
        canvas.drawText("LLOJI / FARE", PDF_MARGIN, y, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SAS.", quantityColumnX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("CMIMI", paperProfile.pdfWidthPoints - PDF_MARGIN, y, paint)
        y += 5f
        canvas.drawLine(PDF_MARGIN, y, paperProfile.pdfWidthPoints - PDF_MARGIN, y, paint)
        y += 11f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(ticket.fareName.take(19), PDF_MARGIN, y, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("1", quantityColumnX, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            formatAmountCents(ticket.priceCents),
            paperProfile.pdfWidthPoints - PDF_MARGIN,
            y,
            paint
        )
        y += 11f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        wrapPdfText(
            "Linja: ${ticket.routeName}",
            paint,
            paperProfile.pdfWidthPoints - PDF_MARGIN * 2
        )
            .forEach { wrappedLine ->
                canvas.drawText(wrappedLine, PDF_MARGIN, y, paint)
                y += 10f
            }

        y += 2f
        canvas.drawLine(PDF_MARGIN, y, paperProfile.pdfWidthPoints - PDF_MARGIN, y, paint)
        y += 18f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("TOTALI", PDF_MARGIN, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            pdfTicketPriceText(ticket),
            paperProfile.pdfWidthPoints - PDF_MARGIN,
            y,
            paint
        )
        y += 15f

        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("PARA TE GATSHME", paperProfile.pdfWidthPoints - PDF_MARGIN, y, paint)
        y += 14f

        canvas.drawLine(PDF_MARGIN, y, paperProfile.pdfWidthPoints - PDF_MARGIN, y, paint)
        y += 12f

        paint.textAlign = Paint.Align.LEFT
        pdfTicketLines(ticket).forEach { line ->
            wrapPdfText(
                line,
                paint,
                paperProfile.pdfWidthPoints - PDF_MARGIN * 2
            ).forEach { wrappedLine ->
                canvas.drawText(wrappedLine, PDF_MARGIN, y, paint)
                y += 10f
            }
        }

        y += 3f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText("FALEMINDERIT / THANK YOU", paperProfile.pdfWidthPoints / 2f, y, paint)
        y += 9f

        val qrSize = 78f
        val qrLeft = (paperProfile.pdfWidthPoints - qrSize) / 2f
        canvas.drawBitmap(
            createQrBitmap(nonFiscalQrPayload(ticket), QR_BITMAP_SIZE),
            null,
            RectF(qrLeft, y, qrLeft + qrSize, y + qrSize),
            null
        )
        y += qrSize + 9f

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 6.5f
        canvas.drawText(
            "QR TESTUES - TE DHENA LOKALE",
            paperProfile.pdfWidthPoints / 2f,
            y,
            paint
        )
        y += 9f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(
            "NUK ESHTE KUPON FISKAL",
            paperProfile.pdfWidthPoints / 2f,
            y,
            paint
        )

        document.finishPage(page)
        FileOutputStream(outputFile).use(document::writeTo)
    } finally {
        document.close()
    }
}

internal fun pdfTicketLines(ticket: PrintableTicket): List<String> = listOf(
    "Nr. biletes: ${ticket.ticketCode}",
    "Autobusi: ${ticket.busPlateNumber}",
    "Operatori: ${ticket.operatorName}",
    "Data / Ora: ${ticket.soldAtText}"
)

internal fun pdfTicketPriceText(ticket: PrintableTicket): String =
    formatEuroCents(ticket.priceCents)

internal fun nonFiscalQrPayload(ticket: PrintableTicket): String = listOf(
    "BUSPAY_TEST_NON_FISCAL",
    "ticket=${ticket.ticketCode}",
    "bus=${ticket.busPlateNumber}",
    "amount=${formatAmountCents(ticket.priceCents)}",
    "sold=${ticket.soldAtText}"
).joinToString("|")

private fun createQrBitmap(contents: String, size: Int): android.graphics.Bitmap {
    val matrix = QRCodeWriter().encode(
        contents,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 0)
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return android.graphics.Bitmap.createBitmap(
        pixels,
        size,
        size,
        android.graphics.Bitmap.Config.ARGB_8888
    )
}

private fun wrapPdfText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val words = text.split(' ')
    val lines = mutableListOf<String>()
    var currentLine = ""

    words.forEach { word ->
        val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (currentLine.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
            lines += currentLine
            currentLine = word
        } else {
            currentLine = candidate
        }
    }
    if (currentLine.isNotEmpty()) lines += currentLine
    return lines.ifEmpty { listOf("") }
}

private const val PDF_PAGE_HEIGHT_POINTS = 420
private const val PDF_MARGIN = 12f
private const val QR_BITMAP_SIZE = 256
private const val QUANTITY_COLUMN_RATIO = 0.71f

internal fun buildEscPosTicketBytes(
    ticket: PrintableTicket,
    paperProfile: ReceiptPaperProfile = ReceiptPaperProfile.MM58
): ByteArray {
    val output = ByteArrayOutputStream()

    output.write(byteArrayOf(0x1B, 0x40)) // Initialize.
    output.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center alignment.
    output.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold on.
    output.write("BUSPAY TRANSPORT\n".toByteArray(StandardCharsets.UTF_8))
    output.write("BILETE UDHETIMI\n".toByteArray(StandardCharsets.UTF_8))
    output.write("TEST - JO FISKAL\n".toByteArray(StandardCharsets.UTF_8))
    output.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off.
    output.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left alignment.
    output.write(escPosTicketText(ticket, paperProfile).toByteArray(StandardCharsets.UTF_8))
    output.write(byteArrayOf(0x1D, 0x56, 0x00)) // Full cut when supported.

    return output.toByteArray()
}

internal fun escPosTicketText(
    ticket: PrintableTicket,
    paperProfile: ReceiptPaperProfile
): String {
    val divider = "-".repeat(paperProfile.textColumns)
    val details = listOf(
        "Nr. biletes: ${ticket.ticketCode}",
        "Autobusi: ${ticket.busPlateNumber}",
        "Linja: ${ticket.routeName}",
        "Operatori: ${ticket.operatorName}",
        "Data / Ora: ${ticket.soldAtText}"
    ).flatMap { wrapReceiptText(it, paperProfile.textColumns) }
    val fareAndPrice = alignedReceiptRow(
        left = ticket.fareName,
        right = formatEuroCents(ticket.priceCents),
        columns = paperProfile.textColumns
    )
    val total = alignedReceiptRow(
        left = "TOTALI",
        right = formatEuroCents(ticket.priceCents),
        columns = paperProfile.textColumns
    )

    return buildString {
        appendLine(divider)
        appendLine(fareAndPrice)
        appendLine(divider)
        appendLine(total)
        appendLine("PARA TE GATSHME")
        appendLine()
        details.forEach(::appendLine)
        appendLine()
        appendLine("FALEMINDERIT")
        appendLine("NUK ESHTE KUPON FISKAL")
        appendLine()
        appendLine()
    }
}

internal fun alignedReceiptRow(left: String, right: String, columns: Int): String {
    require(columns > right.length + 1)
    val safeLeft = left.take(columns - right.length - 1)
    val spaces = " ".repeat(columns - safeLeft.length - right.length)
    return safeLeft + spaces + right
}

internal fun wrapReceiptText(text: String, columns: Int): List<String> {
    require(columns > 0)
    if (text.isBlank()) return listOf("")
    val lines = mutableListOf<String>()
    var remaining = text.trim()
    while (remaining.length > columns) {
        val candidate = remaining.take(columns + 1)
        val breakAt = candidate.lastIndexOf(' ').takeIf { it > 0 } ?: columns
        lines += remaining.take(breakAt).trimEnd()
        remaining = remaining.drop(breakAt).trimStart()
    }
    if (remaining.isNotEmpty()) lines += remaining
    return lines
}

private fun formatEuroCents(cents: Int): String =
    "EUR ${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

private fun formatAmountCents(cents: Int): String =
    "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

interface StopRequestInput {
    fun start(onStopRequested: () -> Unit)
    fun stop()
}

class DemoStopRequestInput : StopRequestInput {
    private var listener: (() -> Unit)? = null

    override fun start(onStopRequested: () -> Unit) {
        listener = onStopRequested
    }

    override fun stop() {
        listener = null
    }

    fun trigger() {
        listener?.invoke()
    }
}
