package airline.api

import kotlinx.datetime.Instant

sealed class Notification

sealed class MassNotification : Notification() {
    abstract val flight: Flight
}
class DelayNotification(override val flight: Flight, val actualDepartureTime: Instant) : MassNotification()
class CancelNotification(override val flight: Flight) : MassNotification()
class SetCheckInNumberNotification(override val flight: Flight, val checkInNumber: String) : MassNotification()
class SetGateNumberNotification(override val flight: Flight, val gateNumber: String) : MassNotification()

sealed class SingleNotification : Notification() {
    abstract val ticketInfo: BuyTicket
}
class SuccessfulPurchaseNotification(override val ticketInfo: BuyTicket) : SingleNotification()
class OccupiedSeatNotification(override val ticketInfo: BuyTicket) : SingleNotification()
class ClosedFlightNotification(override val ticketInfo: BuyTicket) : SingleNotification()
class IncorrectFlightNotification(override val ticketInfo: BuyTicket) : SingleNotification()
