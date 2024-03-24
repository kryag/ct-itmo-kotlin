package airline

import airline.api.*
import airline.service.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class AirlineApplication(private val config: AirlineConfig, emailService: EmailService) {
    private val events = MutableSharedFlow<Event>()

    private val flights = MutableStateFlow<List<Flight>>(emptyList())

    private val passengerNotificationService = PassengerNotificationService(
        emailService,
        CoroutineScope(Dispatchers.Default),
    )

    private val bufferedEmailService = BufferedEmailService(emailService = emailService)

    val bookingService: BookingService = object : BookingService {
        override val flightSchedule: List<FlightInfo>
            get() = flights.value.filter {
                Clock.System.now() < it.departureTime - config.ticketSaleEndTime
            }.map { flight ->
                FlightInfo(
                    flightId = flight.flightId,
                    departureTime = flight.departureTime,
                    isCancelled = flight.isCancelled,
                    actualDepartureTime = flight.actualDepartureTime,
                    checkInNumber = flight.checkInNumber,
                    gateNumber = flight.gateNumber,
                    plane = flight.plane,
                )
            }

        override fun freeSeats(flightId: String, departureTime: Instant): Set<String> {
            val currentFlight = flights.value.find {
                it.flightId == flightId && it.departureTime == departureTime
            } ?: return emptySet()

            val occupiedSeats = currentFlight.tickets.map { it.value.seatNo }.toSet()
            val allSeats = currentFlight.plane.seats

            return allSeats - occupiedSeats
        }

        override suspend fun buyTicket(
            flightId: String,
            departureTime: Instant,
            seatNo: String,
            passengerId: String,
            passengerName: String,
            passengerEmail: String,
        ) {
            events.emit(
                BuyTicket(
                    flightId = flightId,
                    departureTime = departureTime,
                    seatNo = seatNo,
                    passengerId = passengerId,
                    passengerName = passengerName,
                    passengerEmail = passengerEmail,
                ),
            )
        }
    }

    val managementService: AirlineManagementService = object : AirlineManagementService {
        override suspend fun scheduleFlight(flightId: String, departureTime: Instant, plane: Plane) {
            events.emit(ScheduleFlight(flightId = flightId, departureTime = departureTime, plane = plane))
        }

        override suspend fun delayFlight(flightId: String, departureTime: Instant, actualDepartureTime: Instant) {
            events.emit(
                DelayFlight(
                    flightId = flightId,
                    departureTime = departureTime,
                    actualDepartureTime = actualDepartureTime,
                ),
            )
        }

        override suspend fun cancelFlight(flightId: String, departureTime: Instant) {
            events.emit(
                CancelFlight(
                    flightId = flightId,
                    departureTime = departureTime,
                ),
            )
        }

        override suspend fun setCheckInNumber(flightId: String, departureTime: Instant, checkInNumber: String) {
            events.emit(
                SetCheckInNumber(
                    flightId = flightId,
                    departureTime = departureTime,
                    checkInNumber = checkInNumber,
                ),
            )
        }

        override suspend fun setGateNumber(flightId: String, departureTime: Instant, gateNumber: String) {
            events.emit(
                SetGateNumber(
                    flightId = flightId,
                    departureTime = departureTime,
                    gateNumber = gateNumber,
                ),
            )
        }
    }

    @OptIn(FlowPreview::class)
    fun airportInformationDisplay(coroutineScope: CoroutineScope): StateFlow<InformationDisplay> {
        return flights
            .map { flightsList ->
                val currentTime = Clock.System.now()
                InformationDisplay(
                    flightsList
                        .map { flight ->
                            FlightInfo(
                                flightId = flight.flightId,
                                departureTime = flight.departureTime,
                                isCancelled = flight.isCancelled,
                                actualDepartureTime = flight.actualDepartureTime,
                                checkInNumber = flight.checkInNumber,
                                gateNumber = flight.gateNumber,
                                plane = flight.plane,
                            )
                        }
                        .filter {
                            it.departureTime in currentTime..(currentTime + 1.days)
                        },
                )
            }
            .sample(config.displayUpdateInterval)
            .distinctUntilChanged()
            .stateIn(coroutineScope, SharingStarted.Eagerly, InformationDisplay(emptyList()))
    }

    val airportAudioAlerts: Flow<AudioAlerts> = flow {
        val alertPeriod: Duration = 3.minutes
        while (true) {
            flights.value.forEach { flight ->
                val currentTime = Clock.System.now()
                val actualDepartureTime = flight.actualDepartureTime

                val registrationOpen = actualDepartureTime - config.registrationOpeningTime
                val registrationClosed = actualDepartureTime - config.registrationClosingTime

                val boardingOpen = actualDepartureTime - config.boardingOpeningTime
                val boardingClosed = actualDepartureTime - config.boardingClosingTime

                if (currentTime in registrationOpen..(registrationOpen + alertPeriod)) {
                    emit(
                        AudioAlerts.RegistrationOpen(
                            flightNumber = flight.flightId,
                            checkInNumber = flight.checkInNumber ?: "",
                        ),
                    )
                }

                if (currentTime in (registrationClosed - alertPeriod)..registrationClosed) {
                    emit(
                        AudioAlerts.RegistrationClosing(
                            flightNumber = flight.flightId,
                            checkInNumber = flight.checkInNumber ?: "",
                        ),
                    )
                }

                if (currentTime in boardingOpen..(boardingOpen + alertPeriod)) {
                    emit(
                        AudioAlerts.BoardingOpened(
                            flightNumber = flight.flightId,
                            gateNumber = flight.gateNumber ?: "",
                        ),
                    )
                }

                if (currentTime in (boardingClosed - alertPeriod)..boardingClosed) {
                    emit(
                        AudioAlerts.BoardingClosing(
                            flightNumber = flight.flightId,
                            gateNumber = flight.gateNumber ?: "",
                        ),
                    )
                }
            }

            delay(config.audioAlertsInterval)
        }
    }

    private fun findFlight(flightId: String, departureTime: Instant): Flight? {
        return flights.value.find { it.flightId == flightId && it.departureTime == departureTime }
    }

    private fun changeFlight(flightId: String, departureTime: Instant, change: (Flight) -> Flight) {
        flights.update { currentFlights ->
            currentFlights.map {
                if (it.flightId == flightId && it.departureTime == departureTime) {
                    change(it)
                } else {
                    it
                }
            }
        }
    }

    suspend fun run() {
        coroutineScope {
            launch {
                bufferedEmailService.run()
            }

            events.collect { event ->
                when (event) {
                    is ScheduleFlight -> {
                        if (findFlight(event.flightId, event.departureTime) != null) {
                            return@collect
                        }

                        val newFlight = Flight(
                            flightId = event.flightId,
                            departureTime = event.departureTime,
                            plane = event.plane,
                        )

                        flights.emit(flights.value + newFlight)
                    }

                    is DelayFlight -> {
                        val delayedFlight = findFlight(event.flightId, event.departureTime) ?: return@collect

                        changeFlight(flightId = event.flightId, departureTime = event.departureTime) {
                            it.copy(actualDepartureTime = event.actualDepartureTime)
                        }

                        passengerNotificationService.sendNotification(
                            DelayNotification(
                                flight = delayedFlight,
                                actualDepartureTime = event.actualDepartureTime,
                            ),
                        )
                    }

                    is CancelFlight -> {
                        val canceledFlight = findFlight(event.flightId, event.departureTime) ?: return@collect

                        changeFlight(flightId = event.flightId, departureTime = event.departureTime) {
                            it.copy(isCancelled = true)
                        }

                        passengerNotificationService.sendNotification(
                            CancelNotification(
                                flight = canceledFlight,
                            ),
                        )
                    }

                    is SetCheckInNumber -> {
                        val changedFlight = findFlight(event.flightId, event.departureTime) ?: return@collect

                        changeFlight(flightId = event.flightId, departureTime = event.departureTime) {
                            it.copy(checkInNumber = event.checkInNumber)
                        }

                        passengerNotificationService.sendNotification(
                            SetCheckInNumberNotification(
                                flight = changedFlight,
                                checkInNumber = event.checkInNumber,
                            ),
                        )
                    }

                    is SetGateNumber -> {
                        val changedFlight = findFlight(event.flightId, event.departureTime) ?: return@collect

                        changeFlight(flightId = event.flightId, departureTime = event.departureTime) {
                            it.copy(gateNumber = event.gateNumber)
                        }

                        passengerNotificationService.sendNotification(
                            SetGateNumberNotification(
                                flight = changedFlight,
                                gateNumber = event.gateNumber,
                            ),
                        )
                    }

                    is BuyTicket -> {
                        val requiredFlight = findFlight(event.flightId, event.departureTime)

                        if (requiredFlight == null || requiredFlight.isCancelled ||
                            !requiredFlight.plane.seats.contains(event.seatNo)
                        ) {
                            bufferedEmailService.sendNotification(
                                IncorrectFlightNotification(ticketInfo = event),
                            )
                            return@collect
                        }

                        if (requiredFlight.departureTime - Clock.System.now() <= config.ticketSaleEndTime) {
                            bufferedEmailService.sendNotification(ClosedFlightNotification(ticketInfo = event))
                            return@collect
                        }

                        val seatIsFree = requiredFlight.tickets[event.seatNo] == null
                        if (!seatIsFree) {
                            bufferedEmailService.sendNotification(OccupiedSeatNotification(ticketInfo = event))
                            return@collect
                        }

                        val newTicket = Ticket(
                            flightId = event.flightId,
                            departureTime = event.departureTime,
                            seatNo = event.seatNo,
                            passengerId = event.passengerId,
                            passengerName = event.passengerName,
                            passengerEmail = event.passengerEmail,
                        )
                        changeFlight(flightId = event.flightId, departureTime = event.departureTime) {
                            it.copy(tickets = it.tickets + (event.seatNo to newTicket))
                        }
                        bufferedEmailService.sendNotification(
                            SuccessfulPurchaseNotification(ticketInfo = event),
                        )
                    }
                }
            }
        }
    }
}
