package safesound.model

/**
 * Response model data
 * */
data class SimpleResponse(
    val success: Boolean,
    val message: String,
    val generic: Any? = null,
)

