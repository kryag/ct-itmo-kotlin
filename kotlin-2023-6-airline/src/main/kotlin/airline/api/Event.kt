package airline.api

import kotlinx.datetime.Instant

sealed class Event

class ScheduleFlight(val flightId: String, val departureTime: Instant, val plane: Plane) : Event()

class DelayFlight(val flightId: String, val departureTime: Instant, val actualDepartureTime: Instant) : Event()

class CancelFlight(val flightId: String, val departureTime: Instant) : Event()

class SetCheckInNumber(val flightId: String, val departureTime: Instant, val checkInNumber: String) : Event()

class SetGateNumber(val flightId: String, val departureTime: Instant, val gateNumber: String) : Event()

class BuyTicket(
    val flightId: String,
    val departureTime: Instant,
    val seatNo: String,
    val passengerId: String,
    val passengerName: String,
    val passengerEmail: String,
) : Event()
