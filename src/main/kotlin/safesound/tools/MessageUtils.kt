package safesound.tools

import com.twilio.http.TwilioRestClient
import com.twilio.rest.api.v2010.account.MessageCreator
import com.twilio.type.PhoneNumber
import safesound.interfaces.MessageInterface

object MessageUtils : MessageInterface {
    /**
     * get all information needed to send sms from environment
     */
    private val sid = System.getenv("TWILIOSID")
    private val token = System.getenv("TWILIOTOKEN")
    private val phoneNumber = System.getenv("PHONE").toString()
    private val client = TwilioRestClient.Builder(sid, token).build()


    /**
     * These two methods create an sms message and sends it, using the twilio framework
     * @param destPhone destination phone number
     * @param key holds the generated code for the activation
     */
    override fun sendActivationSMS(destPhone: String, key: String) {
        MessageCreator(
            PhoneNumber(destPhone),
            PhoneNumber(phoneNumber),
            "This is your activation link for Safe&Sound- https://safe-sound-208.herokuapp.com/user/activate_account?code=$key"
        ).create(client)
    }

    override fun sendRecoverySMS(destPhone: String, key: String) {
        MessageCreator(
            PhoneNumber(destPhone),
            PhoneNumber(phoneNumber),
            "This is your new password for Safe&Sound, when you login please change the password immediately - $key"
        ).create(client)
    }
}