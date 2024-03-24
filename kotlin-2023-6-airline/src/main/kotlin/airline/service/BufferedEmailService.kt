package airline.service

import airline.api.*
import kotlinx.coroutines.channels.Channel

class BufferedEmailService(private val emailService: EmailService) : EmailService {
    private val emailMessageChannel = Channel<EmailMessage>(capacity = Channel.BUFFERED)

    suspend fun sendNotification(notification: SingleNotification) {
        val ticketInfo = notification.ticketInfo
        val passengerName = ticketInfo.passengerName
        val flightId = ticketInfo.flightId
        val departureTime = ticketInfo.departureTime
        val seatNo = ticketInfo.seatNo

        val email = ticketInfo.passengerEmail
        val message = when (notification) {
            is SuccessfulPurchaseNotification ->
                "Dear $passengerName. You have successfully purchased for " +
                    "flight $flightId departing at $departureTime. Your seat number is $seatNo."

            is OccupiedSeatNotification ->
                "Dear $passengerName. We are sorry, but the seat $seatNo for " +
                    "flight $flightId departing at $departureTime is already taken."

            is ClosedFlightNotification ->
                "Dear $passengerName. We are sorry, but the flight $flightId " +
                    "departing at $departureTime is already closed."

            is IncorrectFlightNotification ->
                "Dear $passengerName. We are sorry, but the available flight $flightId " +
                    "departing at $departureTime with $seatNo seat number does not exist."
        }
        emailMessageChannel.send(EmailMessage(email = email, message = message))
    }

    suspend fun run() {
        for (notification in emailMessageChannel) {
            send(notification.email, notification.message)
        }
    }

    override suspend fun send(to: String, text: String) {
        emailService.send(to, text)
    }
}
