import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import BoardingState.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val bannedPassengers = setOf("Nogartse")

fun main() {
    runBlocking {
        println("Getting the latest flight info...")
        val flights = fetchFlights()
        val flightDescriptions = flights.joinToString {
            "${it.passengerName} (${it.flightNumber})"
        }
        println("Found flights for $flightDescriptions")

        // Mutable flow, can change. Will emit new value when changed like an observer.
        val flightsAtGate = MutableStateFlow(flights.size)
        launch {
            // Since collect is a suspending function, it needs to be called inside a coroutine that doesn't block the thread
            flightsAtGate
                .takeWhile { it > 0 } // Stop the flow once flights.size is zero or less
                .onCompletion {
                    println("Finished tracking all flights")
                }
                .collect { flightCount ->
                    println("There are $flightCount flights being tracked")
                }
        }

        launch {
            flights.forEach {
                watchFlight(it)
                // Decrement value after flight departs
                flightsAtGate.value = flightsAtGate.value - 1
            }
        }
    }
}

suspend fun watchFlight(initialFlight: FlightStatus) {
    val passengerName = initialFlight.passengerName

    // Flow for current flight, this has a set number of emissions (based on departure time)
    val currentFlight: Flow<FlightStatus> = flow {
        require(passengerName !in bannedPassengers) {
            "Cannot track $passengerName's flight. They are banned from the airport."
        }

        var flight = initialFlight
        while (flight.departureTimeInMinutes >= 0 && !flight.isFlightCanceled) { // Basically a countdown to zero
            emit(flight)
            delay(400)
            flight = flight.copy(
                departureTimeInMinutes = flight.departureTimeInMinutes - 1
            )
        }
    }

    // Collector for currentFlight (which is a flow)
    currentFlight
        .map { flight ->
            when (flight.boardingStatus) {
                FlightCanceled -> "Your flight was canceled"
                BoardingNotStarted -> "Boarding will start soon"
                WaitingToBoard -> "Other passengers are boarding"
                Boarding -> "You can now board the plane"
                BoardingEnded -> "The boarding doors have closed"
            } + " (Flight departs in ${flight.departureTimeInMinutes} minutes)"
        }
        .onCompletion {
            println("Finished tracking $passengerName's flight")
        }
        .collect { status ->
            println("$passengerName: $status")
        }
}

suspend fun fetchFlights(
    passengerNames: List<String> = listOf("Madrigal", "Polarcubis")
) = passengerNames.map {
    fetchFlight(it)
}
