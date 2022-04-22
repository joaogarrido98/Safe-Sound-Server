package safesound.model

import java.time.LocalDateTime
import java.util.*
/**
 * Activation model data
 * */
data class Activation(
    val code: String,
    val email_of_user: String,
    val date_of_creation: LocalDateTime
)