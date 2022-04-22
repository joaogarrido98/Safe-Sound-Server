package safesound.interfaces

/**
 * interface to achieve total abstraction in Messages class
 */
interface MessageInterface {
    /**
     * send activation email abstraction
     */
    fun sendActivationSMS(destPhone: String, key: String)

    fun sendRecoverySMS(destPhone: String, key: String)
}