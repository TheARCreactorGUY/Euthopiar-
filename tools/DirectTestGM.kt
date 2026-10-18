import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.Base64

fun main() {
    val url = "https://greenmotors.club/?id=Q1dxTzFnbjhrQm5kMG5lL01tQzcvdEhwZ0JtZVdjNUEvbXdPRHcyWGVHV2tjV2dmZVVNVlhFN2tHMTUrU1h1Nk85a1NvaEdUR1RqK0RBTTBxaFRnN0E9PQ=="
    val client = OkHttpClient()
    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36").build()
    val html = client.newCall(req).execute().body?.string().orEmpty()
    println("HTML length: ${html.length}")
    
    val payloadMatch = Regex("""s\(\s*['"]o['"]\s*,\s*['"]([^'"]+)['"]\s*,\s*\d+""").find(html)?.groupValues?.get(1)
    println("Payload match: $payloadMatch")
    if (payloadMatch != null) {
        val d1 = Base64.getDecoder().decode(payloadMatch)
        val s1 = String(d1)
        println("S1: $s1")
        val d2 = Base64.getDecoder().decode(s1)
        val s2 = String(d2)
        println("S2: $s2")
    }
}
