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
        providers.forEach { provider ->
            manager.requestLocationUpdates(
                provider,
                LOCATION_UPDATE_INTERVAL_MILLIS,
                LOCATION_UPDATE_DISTANCE_METERS,
                listener,
                Looper.getMainLooper()
            )
            manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
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

class BluetoothEscPosTicketPrinter(context: Context) : TicketPrinter {
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
                    name = device.name?.takeIf(String::isNotBlank) ?: "Bluetooth printer",
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
            ?: return@withContext PrintResult.Failure("Bluetooth is not available")
        if (!adapter.isEnabled) {
            return@withContext PrintResult.Failure("Bluetooth is turned off")
        }

        val socket = try {
            adapter
                .getRemoteDevice(printer.address)
                .createRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE_UUID)
        } catch (error: Exception) {
            return@withContext PrintResult.Failure(
                error.message ?: "Could not open the selected printer"
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
            PrintResult.Failure(error.message ?: "The printer did not accept the ticket")
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
    override fun pairedPrinters(): List<PrinterDevice> = listOf(TEST_DEVICE)

    override suspend fun printTicket(
        printer: PrinterDevice,
        ticket: PrintableTicket
    ): PrintResult = withContext(Dispatchers.IO) {
        if (printer.address != TEST_DEVICE.address) {
            return@withContext PrintResult.Failure("Unknown PDF test printer")
        }

        val outputDirectory = context
            .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?.resolve("tickets")
            ?: context.filesDir.resolve("tickets")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            return@withContext PrintResult.Failure("Could not create the PDF ticket folder")
        }

        val safeTicketCode = ticket.ticketCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outputFile = File(outputDirectory, "ticket-$safeTicketCode.pdf")

        try {
            writeTicketPdf(outputFile, ticket)
            PrintResult.Success(outputPath = outputFile.absolutePath)
        } catch (error: Exception) {
            PrintResult.Failure(error.message ?: "Could not create the ticket PDF")
        }
    }

    companion object {
        val TEST_DEVICE = PrinterDevice(
            name = "PDF Test Printer",
            address = "pdf://ticket-preview"
        )
    }
}

private fun writeTicketPdf(outputFile: File, ticket: PrintableTicket) {
    val document = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(
            PDF_PAGE_WIDTH_POINTS,
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
        canvas.drawText("BUSPAY TRANSPORT", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)
        y += 13f

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawText("PUBLIC TRANSPORT - DEMO", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)
        y += 10f

        paint.strokeWidth = 1f
        canvas.drawLine(PDF_MARGIN, y, PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 13f

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("BILETE UDHETIMI", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)
        y += 12f
        paint.textSize = 9f
        canvas.drawText("KUPON TESTUES - JO FISKAL", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)
        y += 14f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 7.5f
        canvas.drawText("LLOJI / FARE", PDF_MARGIN, y, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SAS.", 117f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("CMIMI", PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 5f
        canvas.drawLine(PDF_MARGIN, y, PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 11f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(ticket.fareName.take(19), PDF_MARGIN, y, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("1", 117f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatAmountCents(ticket.priceCents), PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 11f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        wrapPdfText("Linja: ${ticket.routeName}", paint, PDF_PAGE_WIDTH_POINTS - PDF_MARGIN * 2)
            .forEach { wrappedLine ->
                canvas.drawText(wrappedLine, PDF_MARGIN, y, paint)
                y += 10f
            }

        y += 2f
        canvas.drawLine(PDF_MARGIN, y, PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 18f

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("TOTALI", PDF_MARGIN, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(pdfTicketPriceText(ticket), PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 15f

        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("PARA TE GATSHME", PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 14f

        canvas.drawLine(PDF_MARGIN, y, PDF_PAGE_WIDTH_POINTS - PDF_MARGIN, y, paint)
        y += 12f

        paint.textAlign = Paint.Align.LEFT
        pdfTicketLines(ticket).forEach { line ->
            wrapPdfText(line, paint, PDF_PAGE_WIDTH_POINTS - PDF_MARGIN * 2).forEach { wrappedLine ->
                canvas.drawText(wrappedLine, PDF_MARGIN, y, paint)
                y += 10f
            }
        }

        y += 3f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText("FALEMINDERIT / THANK YOU", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)
        y += 9f

        val qrSize = 78f
        val qrLeft = (PDF_PAGE_WIDTH_POINTS - qrSize) / 2f
        canvas.drawBitmap(
            createQrBitmap(nonFiscalQrPayload(ticket), QR_BITMAP_SIZE),
            null,
            RectF(qrLeft, y, qrLeft + qrSize, y + qrSize),
            null
        )
        y += qrSize + 9f

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 6.5f
        canvas.drawText("QR TESTUES - TE DHENA LOKALE", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)
        y += 9f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("NUK ESHTE KUPON FISKAL", PDF_PAGE_WIDTH_POINTS / 2f, y, paint)

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

private const val PDF_PAGE_WIDTH_POINTS = 164
private const val PDF_PAGE_HEIGHT_POINTS = 420
private const val PDF_MARGIN = 12f
private const val QR_BITMAP_SIZE = 256

internal fun buildEscPosTicketBytes(ticket: PrintableTicket): ByteArray {
    val output = ByteArrayOutputStream()

    output.write(byteArrayOf(0x1B, 0x40)) // Initialize.
    output.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center alignment.
    output.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold on.
    output.write("BUSPAY TRANSPORT\n".toByteArray(StandardCharsets.UTF_8))
    output.write("TRAVEL TICKET\n".toByteArray(StandardCharsets.UTF_8))
    output.write("TEST - NOT FISCAL\n".toByteArray(StandardCharsets.UTF_8))
    output.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off.
    output.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left alignment.
    output.write("-------------------------------\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Fare       Qty          Price\n".toByteArray(StandardCharsets.UTF_8))
    output.write("${ticket.fareName}       1   ${formatEuroCents(ticket.priceCents)}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("-------------------------------\n".toByteArray(StandardCharsets.UTF_8))
    output.write("TOTAL: ${formatEuroCents(ticket.priceCents)}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Cash\n\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Ticket: ${ticket.ticketCode}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Bus: ${ticket.busPlateNumber}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Route: ${ticket.routeName}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Operator: ${ticket.operatorName}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("Sold: ${ticket.soldAtText}\n".toByteArray(StandardCharsets.UTF_8))
    output.write("\nThank you\nNOT A FISCAL RECEIPT\n\n\n".toByteArray(StandardCharsets.UTF_8))
    output.write(byteArrayOf(0x1D, 0x56, 0x00)) // Full cut when supported.

    return output.toByteArray()
}

private fun formatEuroCents(cents: Int): String =
    "EUR ${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

private fun formatAmountCents(cents: Int): String =
    "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

interface StopRequestInput {
    fun listen(onStopRequested: () -> Unit)
}
