import java.net.URL

private const val BASE_URL = "http://kotlin-book.bignerdranch.com/2e"
private const val FLIGHT_ENDPOINT = "$BASE_URL/flight"

fun main() {
    println("Started")
    val flight = fetchFlight()
    println(flight)
    println("Finished")
}

fun fetchFlight(): String = URL(FLIGHT_ENDPOINT).readText()