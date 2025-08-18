package neth.iecal.questphone.utils

import android.util.Base64
import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object QrProofUtils {
    // Global secret for QR proof HMAC. Replace with your secret before release.
    const val GLOBAL_SECRET: String = "biVBUlpScNGkG2JIwYR8SpzUlHze3L"
    data class QrPayload(val token: String, val sig: String)

    // Validates QR JSON: {"token":"uuid:timestamp","sig":"base64(hmac_sha256(token, secretKey))"}
    // Returns true if valid and timestamp within maxSkewSeconds.
    fun validate(json: String, secretKey: String, nowMillis: Long = System.currentTimeMillis(), maxSkewSeconds: Long = 120): Boolean {
        return try {
            val payload = Gson().fromJson(json.trim(), QrPayload::class.java)
            if (payload?.token.isNullOrBlank() || payload.sig.isBlank()) return false
            val parts = payload.token.split(":")
            if (parts.size != 2) return false
            val tsRaw = parts[1].toLongOrNull() ?: return false
            // Accept either seconds or milliseconds epoch. If value looks like seconds (< 10^12), convert to ms.
            val tsMillis = if (tsRaw < 1_000_000_000_000L) tsRaw * 1000L else tsRaw
            val ageSeconds = kotlin.math.abs(nowMillis - tsMillis) / 1000
            if (ageSeconds > maxSkewSeconds) return false
            val computed = hmacSha256Base64(payload.token, secretKey)
            // Timing-safe-ish compare by length and per-char
            constantTimeEquals(computed, payload.sig)
        } catch (_: Exception) {
            false
        }
    }

    // Convenience: validate using the global hardcoded secret
    fun validateWithGlobalSecret(json: String, nowMillis: Long = System.currentTimeMillis(), maxSkewSeconds: Long = 120): Boolean {
        return validate(json, GLOBAL_SECRET, nowMillis, maxSkewSeconds)
    }

    private fun hmacSha256Base64(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val raw = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
