package airline.service

import airline.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class PassengerNotificationService(
    private val emailService: EmailService,
    coroutineScope: CoroutineScope,
) {
    private val emailMessageChannel = Channel<EmailMessage>(capacity = Channel.BUFFERED)

    init {
        coroutineScope.launch {
            while (true) {
                for (notification in emailMessageChannel) {
                    emailService.send(notification.email, notification.message)
                }
            }
        }
    }

    suspend fun sendNotification(notification: MassNotification) {
        val flight = notification.flight
        val flightId = flight.flightId
        val departureTime = flight.departureTime

        val createMessage = { ticket: Ticket ->
            val passengerName = ticket.passengerName
            when (notification) {
                is DelayNotification ->
                    "Dear, $passengerName. Unfortunately, your flight $flightId " +
                        "delayed from $departureTime to ${notification.actualDepartureTime}."

                is CancelNotification ->
                    "Dear, $passengerName. Unfortunately, your flight $flightId " +
                        "on $departureTime has been cancelled."

                is SetCheckInNumberNotification ->
                    "Dear, $passengerName. Please note that your " +
                        "flight $flightId on $departureTime check-in number has changed to " +
                        "${notification.checkInNumber}."

                is SetGateNumberNotification ->
                    "Dear, $passengerName. Please note that your flight " +
                        "$flightId on $departureTime gate number has changed to ${notification.gateNumber}."
            }
        }

        for (ticket in flight.tickets.values) {
            val email = ticket.passengerEmail
            val message = createMessage(ticket)
            emailMessageChannel.send(EmailMessage(email = email, message = message))
        }
    }
}
