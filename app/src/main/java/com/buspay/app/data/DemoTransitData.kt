package com.buspay.app.data

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

    val fareTypes = listOf(
        FareType(
            id = Ticket.STANDARD_FARE_TYPE_ID,
            name = "Standard",
            priceCents = 50
        ),
        FareType(
            id = "student",
            name = "Student",
            priceCents = 30,
            eligibility = "Valid student ID required"
        ),
        FareType(
            id = "senior",
            name = "Senior 65+",
            priceCents = 25,
            eligibility = "For passengers aged 65 or older"
        ),
        FareType(
            id = "child",
            name = "Child",
            priceCents = 20,
            eligibility = "For children aged 6 to 12"
        )
    )

    val routes = listOf(
        Route(
            id = "route-1",
            name = "Line 1 - Center to Hospital",
            stops = listOf(
                Stop("stop-1", "Central Station", 42.6629, 21.1655, 1),
                Stop("stop-2", "Mother Teresa Boulevard", 42.6608, 21.1622, 2),
                Stop("stop-3", "University Hospital", 42.6488, 21.1612, 3)
            )
        ),
        Route(
            id = "route-2",
            name = "Line 2 - Center to Sunny Hill",
            stops = listOf(
                Stop("stop-4", "Central Station", 42.6629, 21.1655, 1),
                Stop("stop-5", "City Park", 42.6551, 21.1713, 2),
                Stop("stop-6", "Sunny Hill", 42.6468, 21.1781, 3)
            )
        )
    )
}
