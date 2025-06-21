package neth.iecal.questphone.data.game

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import neth.iecal.questphone.utils.json
import java.io.IOException

@Serializable
enum class EventType{
    NAVIGATION,
    NORMAL
}

// Data models for pet configuration
@Serializable
data class PetConfig(
    val id: String,
    val imageResource: String, // Resource name for the pet image
    val personality: PetPersonality,
    val dialogSequences: Map<String, DialogSequence>,
    val defaultSequence: String // The key of the default sequence to start with
)

@Serializable
data class PetPersonality(
    val speakingStyle: SpeakingStyle,
    val animations: AnimationConfig
)

@Serializable
data class SpeakingStyle(
    val typingSpeed: Float, // Multiplier for typing speed (1.0 is normal)
    val punctuation: String, // Common punctuation style ("!!!" for excited, "..." for thoughtful)
    val catchphrases: List<String>, // Random phrases the pet might add
    val emojis: List<String> // Emojis the pet might use
)

@Serializable
data class AnimationConfig(
    val bounceHeight: Float, // How high the pet bounces
    val bounceSpeed: Float, // How fast the pet bounces
    val extraAnimations: List<String> // Special animations this pet can do
)

@Serializable
data class DialogSequence(
    val steps: List<DialogStep>,
    val eventType : EventType = EventType.NORMAL,
    val eventData : String? = null,
    val conditions: Map<String, String> = emptyMap<String, String>(),
    val nextSequence: String? = null // Can chain to another sequence when complete
)

@Serializable
data class DialogStep(
    val message: String, // The message template (can contain placeholders like {name})
    val expectsInput: Boolean = false, // Whether this step expects user input
    val inputKey: String? = null, // Key to store user input under
    val choices: List<DialogChoice> = emptyList(), // Optional choices for this step
    val conditions: List<DialogCondition>? = null, // Conditions to check before showing this step
    val emotion : String = "question",
    val condition: Map<String, String> = emptyMap<String, String>()
)

@Serializable
data class DialogChoice(
    val text: String,
    val nextStepId: String? = null, // Optional next step to jump to if this choice is selected
    val storeValue: List<String>? = null // Optional value to store if this choice is selected
)

@Serializable
data class DialogCondition(
    val key: String, // The key to check in the user data
    val value: String, // The value to compare against
    val nextStepId: String // The step ID to go to if condition is met
)


@Serializable
data class PetDialogState(
    val userData: MutableMap<String, String> = mutableMapOf<String,String>(
        Pair("user",User.userInfo.getFirstName()),
        Pair("isTutorialOnGoing","true")
    ),
    var currentSequenceId: String = "",
    var currentStepIndex: Int = -1
)
// Utility for storing pet information
object Pet {

    lateinit var appContext: Context
    lateinit var petStateSp : SharedPreferences
    lateinit var petDialogState: PetDialogState

    fun saveCurrentStep(){
        petStateSp.edit(commit = true) {
            putString(
                "pet_state",
                json.encodeToString(petDialogState)
            )
        }
    }
    // Inside object Pet
    fun init(context: Context) {
        appContext = context.applicationContext
        val petInfo = appContext.getSharedPreferences("selected_pet_info", Context.MODE_PRIVATE)
        val currentPetId = petInfo.getString("current_pet_id", "fluffy") ?: "fluffy" // Ensure non-null

        petStateSp = appContext.getSharedPreferences(currentPetId, Context.MODE_PRIVATE)
        val petStateJson = petStateSp.getString("pet_state", null)

        if (petStateJson != null) {
            try {
                petDialogState = json.decodeFromString(petStateJson)
                Log.d("loaded pet state : ",petDialogState.toString())
                // Further validation of petDialogState.currentSequenceId and currentStepIndex
                // against the actual loaded pet script will happen in PetDialog's LaunchedEffect.
            } catch (e: Exception) {
                // Log error, initialize to a fresh state
                Log.e("PetInit", "Failed to decode pet_state JSON: ${e.message}. Initializing fresh state.")
                petDialogState = PetDialogState(
                    currentSequenceId = "", // To be set by PetDialog from script's defaultSequence
                    currentStepIndex = -1
                )
            }
        } else {
            petDialogState = PetDialogState(
                currentSequenceId = "", // Will be populated by PetDialog using the script's defaultSequence
                currentStepIndex = -1  // Indicates to start from the beginning
            )
        }
    }
    fun loadPetScript( petId: String): PetConfig? {
        return try {
            val inputStream = appContext.assets.open("pets/$petId.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}
