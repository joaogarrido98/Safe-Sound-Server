package safesound.tools

import io.ktor.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * singleton class used to manage the resources of database
 */
object ProjectUtils {
    /**
     * create a byte array from secret keyword
     */
    private val hashKey = System.getenv("HASH_KEY").toByteArray()

    /**
     * create a secret key from a given byteArray
     */
    private val hmacKey = SecretKeySpec(hashKey, "HmacSHA1")

    /**
     * hash a given string using SHA1
     * @param word String, string to be hashed
     * @return hashed string
     */
    fun hash(word: String) : String{
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(hmacKey)
        return hex(hmac.doFinal(word.toByteArray(Charsets.UTF_8)))
    }

    /**
     * generate a random code with 30 characters with any letter or number
     * @return a random code
     */
    fun generateRandomCode(): String{
        val allNumbersAndLetters = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        var code = ""
        for (i in 0..29){
            code += allNumbersAndLetters.random()
        }
        return code
    }
}