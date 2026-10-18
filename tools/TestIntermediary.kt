import com.euthopiar.core.provider.FourKHDHubPlugin
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val plugin = FourKHDHubPlugin()
    val dlUrl = "https://greenmotors.club/?id=Q1dxTzFnbjhrQm5kMG5lL01tQzcvdEhwZ0JtZVdjNUEvbXdPRHcyWGVHV2tjV2dmZVVNVlhFN2tHMTUrU1h1Nk85a1NvaEdUR1RqK0RBTTBxaFRnN0E9PQ=="

    fun callPrivate(name: String, arg: String): Any? {
        val m = FourKHDHubPlugin::class.java.getDeclaredMethod(name, String::class.java)
        m.isAccessible = true
        return m.invoke(plugin, arg)
    }

    println("1. Testing bypassGreenmotors:")
    val bypassed = callPrivate("bypassGreenmotors", dlUrl)
    println("   Bypassed: $bypassed")

    if (bypassed is String) {
        println("2. Testing resolveHubCloud:")
        val gamerUrl = callPrivate("resolveHubCloud", bypassed)
        println("   Gamer URL: $gamerUrl")

        if (gamerUrl is String) {
            println("3. Testing resolveGamerxyt:")
            val m3 = FourKHDHubPlugin::class.java.getDeclaredMethod("resolveGamerxyt", String::class.java, java.lang.Integer::class.java)
            m3.isAccessible = true
            val candidates = m3.invoke(plugin, gamerUrl, null) as List<*>
            println("   Candidates: ${candidates.size}")
            for (c in candidates) {
                println("     $c")
            }
        }
    }
}
