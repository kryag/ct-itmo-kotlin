package airline

import airline.api.AirlineConfig
import airline.api.AudioAlerts
import airline.api.Plane
import airline.service.EmailService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MyTest {
    private val timeout: Duration = 50.milliseconds

    @Test
    fun mainTest() {
        val emailService = InChannelEmailService()
        val config = AirlineConfig(
            audioAlertsInterval = 1.seconds,
            displayUpdateInterval = 10.milliseconds,
            ticketSaleEndTime = 30.minutes,
        )
        val airlineApplication = AirlineApplication(config, emailService)
        val seats = setOf("1A", "1B", "2A", "2B")
        val plane = Plane("B737", seats)
        val flightId = "111"
        val flightTime = Clock.System.now() + 1.hours

        testAndCancel {
            launch { airlineApplication.run() }
            sleep()

            val booking = airlineApplication.bookingService
            val management = airlineApplication.managementService
            val display = airlineApplication.airportInformationDisplay(this)

            repeat(2) {
                management.scheduleFlight(flightId, flightTime, plane)
                sleep()

                Assertions.assertEquals(1, display.value.departing.size)
                Assertions.assertEquals(flightId, display.value.departing[0].flightId)

                Assertions.assertEquals(1, booking.flightSchedule.size)
                Assertions.assertEquals(flightId, booking.flightSchedule[0].flightId)
            }

            val pickedSeatNo = "1A"
            booking.buyTicket(
                flightId = flightId,
                departureTime = flightTime,
                seatNo = pickedSeatNo,
                passengerId = "1",
                passengerName = "Georgiy Korneev",
                passengerEmail = "kgeorgiy@kgeorgiy.info",
            )
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("kgeorgiy@kgeorgiy.info", email)
                Assertions.assertTrue("successfully purchased" in text)
                Assertions.assertTrue(flightId in text)
                Assertions.assertTrue(pickedSeatNo in text)
            }
            Assertions.assertEquals(seats - pickedSeatNo, booking.freeSeats(flightId, flightTime))

            booking.buyTicket(
                flightId = flightId,
                departureTime = flightTime,
                seatNo = pickedSeatNo,
                passengerId = "2",
                passengerName = "Mike Perveev",
                passengerEmail = "perveev_m@mail.ru",
            )
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("perveev_m@mail.ru", email)
                Assertions.assertTrue("already taken" in text)
                Assertions.assertTrue(flightId in text)
                Assertions.assertTrue(pickedSeatNo in text)
            }

            val fakeFlightId = "999"
            booking.buyTicket(
                flightId = fakeFlightId,
                departureTime = flightTime,
                seatNo = "2A",
                passengerId = "3",
                passengerName = "Konstantin Bats",
                passengerEmail = "kbats@itmo.ru",
            )
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("kbats@itmo.ru", email)
                Assertions.assertTrue("does not exist" in text)
                Assertions.assertTrue(fakeFlightId in text)
            }

            val newCheckInNumber = "checkin1"
            management.setCheckInNumber(flightId, flightTime, newCheckInNumber)
            sleep()
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("kgeorgiy@kgeorgiy.info", email)
                Assertions.assertTrue("check-in number has changed" in text)
                Assertions.assertTrue(newCheckInNumber in text)
            }
            Assertions.assertEquals(1, display.value.departing.size)
            Assertions.assertEquals(flightId, display.value.departing[0].flightId)
            Assertions.assertEquals(newCheckInNumber, display.value.departing[0].checkInNumber)

            val newGateNumber = "D64"
            management.setGateNumber(flightId, flightTime, newGateNumber)
            sleep()
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("kgeorgiy@kgeorgiy.info", email)
                Assertions.assertTrue("gate number has changed" in text)
                Assertions.assertTrue(newGateNumber in text)
            }
            Assertions.assertEquals(1, display.value.departing.size)
            Assertions.assertEquals(flightId, display.value.departing[0].flightId)
            Assertions.assertEquals(newGateNumber, display.value.departing[0].gateNumber)

            val actualDepartureTime = flightTime + 1.hours
            management.delayFlight(flightId, flightTime, actualDepartureTime)
            sleep()
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("kgeorgiy@kgeorgiy.info", email)
                Assertions.assertTrue("delayed" in text)
                Assertions.assertTrue("$actualDepartureTime" in text)
            }
            Assertions.assertEquals(1, display.value.departing.size)
            Assertions.assertEquals(flightTime, display.value.departing[0].departureTime)
            Assertions.assertEquals(actualDepartureTime, display.value.departing[0].actualDepartureTime)

            management.cancelFlight(flightId, flightTime)
            sleep()
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("kgeorgiy@kgeorgiy.info", email)
                Assertions.assertTrue("cancelled" in text)
            }
            Assertions.assertEquals(1, display.value.departing.size)
            Assertions.assertEquals(flightTime, display.value.departing[0].departureTime)
            Assertions.assertEquals(true, display.value.departing[0].isCancelled)

            val nextDepartureTime = Clock.System.now() + 20.minutes
            management.scheduleFlight(
                flightId = "222",
                departureTime = nextDepartureTime,
                plane = Plane("A310", setOf("1A", "1B")),
            )
            sleep()

            booking.buyTicket(
                flightId = "222",
                departureTime = nextDepartureTime,
                seatNo = "1B",
                passengerId = "4",
                passengerName = "Grigoriy Khlytin",
                passengerEmail = "xlootin@gmail.com",
            )
            withTimeout(timeout) {
                val (email, text) = emailService.messages.receive()
                Assertions.assertEquals("xlootin@gmail.com", email)
                Assertions.assertTrue("already closed" in text)
            }
            Assertions.assertEquals(2, display.value.departing.size)
        }
    }

    @Test
    fun audioAlertsTest() {
        val emailService = InChannelEmailService()
        val config = AirlineConfig(
            audioAlertsInterval = 1.minutes,
            registrationOpeningTime = 1.hours,
            registrationClosingTime = 57.minutes,
            boardingOpeningTime = 1.hours,
            boardingClosingTime = 57.minutes,
        )
        val airlineApplication = AirlineApplication(config, emailService)
        val seats = setOf("1A", "1B", "2A", "2B")
        val plane = Plane("A320", seats)
        val flightId = "165"
        val flightTime = Clock.System.now() + 1.hours

        testAndCancel {
            launch { airlineApplication.run() }
            sleep()

            val management = airlineApplication.managementService
            val audioAlerts = airlineApplication.airportAudioAlerts

            management.scheduleFlight(flightId, flightTime, plane)
            sleep()

            val audioAlertsList = audioAlerts.take(4).toList()
            Assertions.assertEquals(4, audioAlertsList.size)

            Assertions.assertTrue(audioAlertsList.any { it is AudioAlerts.RegistrationOpen })
            Assertions.assertTrue(audioAlertsList.any { it is AudioAlerts.RegistrationClosing })
            Assertions.assertTrue(audioAlertsList.any { it is AudioAlerts.BoardingOpened })
            Assertions.assertTrue(audioAlertsList.any { it is AudioAlerts.BoardingClosing })
        }
    }

    private fun testAndCancel(block: suspend CoroutineScope.() -> Unit) {
        try {
            runBlocking {
                block()
                cancel()
            }
        } catch (ignore: CancellationException) {
        }
    }

    private suspend fun sleep() {
        delay(timeout)
    }

    private class InChannelEmailService : EmailService {
        val messages = Channel<Pair<String, String>>()

        override suspend fun send(to: String, text: String) {
            messages.send(to to text)
        }
    }
}
