import com.euthopiar.core.provider.FourKHDHubPlugin
import com.euthopiar.core.model.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val plugin = FourKHDHubPlugin()
    val url = "https://4khdhub.one/spider-man-brand-new-day-movie-8279/"
    val mediaItem = MediaItem(id = url, title = "Spider-Man: Brand New Day", url = url, posterUrl = null, type = MediaType.MOVIE)
    val details = plugin.getDetails(mediaItem)
    println("Details title: ${details.title}")
    println("Episodes count: ${details.episodes.size}")
    for (ep in details.episodes) {
        println("  Episode data: ${ep.data}")
    }

    println("\nCalling getStreamFlow:")
    val emissions = plugin.getStreamFlow(details.episodes.first().data).toList()
    println("Emissions count: ${emissions.size}")
    for (em in emissions) {
        println("  Emission: $em")
    }
}
