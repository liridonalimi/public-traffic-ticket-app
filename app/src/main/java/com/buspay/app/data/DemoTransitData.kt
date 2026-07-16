package com.buspay.app.data

import android.content.Context
import com.buspay.app.R
import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.FareType
import com.buspay.app.domain.Route
import com.buspay.app.domain.Stop
import com.buspay.app.domain.Ticket

object DemoTransitData {
    val drivers = listOf(
        Driver(id = "driver-001", name = "Arben Krasniqi"),
        Driver(id = "driver-002", name = "Drita Berisha"),
        Driver(id = "driver-003", name = "Ilir Gashi")
    )

    val buses = listOf(
        Bus(id = "bus-101", plateNumber = "01-101-KS"),
        Bus(id = "bus-205", plateNumber = "01-205-KS"),
        Bus(id = "bus-318", plateNumber = "01-318-KS")
    )

    fun fareTypes(context: Context) = listOf(
        FareType(
            id = Ticket.STANDARD_FARE_TYPE_ID,
            name = context.getString(R.string.fare_standard),
            priceCents = 50
        ),
        FareType(
            id = "student",
            name = context.getString(R.string.fare_student),
            priceCents = 30,
            eligibility = context.getString(R.string.fare_student_eligibility)
        ),
        FareType(
            id = "senior",
            name = context.getString(R.string.fare_senior),
            priceCents = 25,
            eligibility = context.getString(R.string.fare_senior_eligibility)
        ),
        FareType(
            id = "child",
            name = context.getString(R.string.fare_child),
            priceCents = 20,
            eligibility = context.getString(R.string.fare_child_eligibility)
        )
    )

    fun routes(context: Context) = listOf(
        Route(
            id = "route-1",
            name = context.getString(R.string.route_one_name),
            stops = listOf(
                Stop("stop-1", context.getString(R.string.stop_central_station), 42.6629, 21.1655, 1),
                Stop("stop-2", context.getString(R.string.stop_mother_teresa), 42.6608, 21.1622, 2),
                Stop("stop-3", context.getString(R.string.stop_university_hospital), 42.6488, 21.1612, 3)
            )
        ),
        Route(
            id = "route-2",
            name = context.getString(R.string.route_two_name),
            stops = listOf(
                Stop("stop-4", context.getString(R.string.stop_central_station), 42.6629, 21.1655, 1),
                Stop("stop-5", context.getString(R.string.stop_city_park), 42.6551, 21.1713, 2),
                Stop("stop-6", context.getString(R.string.stop_sunny_hill), 42.6468, 21.1781, 3)
            )
        )
    )
}
