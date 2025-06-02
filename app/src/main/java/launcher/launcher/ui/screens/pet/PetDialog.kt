package launcher.launcher.ui.screens.pet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import launcher.launcher.R
import launcher.launcher.data.game.DialogStep
import launcher.launcher.data.game.Pet
import launcher.launcher.data.game.PetConfig
import launcher.launcher.data.game.PetDialogState

@Composable
fun PetDialog(
    petId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // Context (not used in this snippet but often useful)
    val petScript by remember { mutableStateOf(Pet.loadPetScript(petId)) }
    val dialogState = remember { Pet.petDialogState }

    // Dismiss if pet script fails to load
    if (petScript == null) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    // Holds the current logical step of the dialog
    var currentStep by remember { mutableStateOf<DialogStep?>(null) }
    // Holds the step whose UI (buttons, input) is currently displayed or animating out.
    // This helps prevent the animation bug by ensuring exit animations use the correct step's data.
    var displayedUiStep by remember { mutableStateOf<DialogStep?>(null) }

    var currentMessage by remember { mutableStateOf("") }
    val progress = remember { Animatable(0f) } // For text animation progress
    val animatedText by remember { derivedStateOf {
        currentMessage.take(progress.value.toInt())
    } }

    // Tracks if the current step's text animation is complete, controlling visibility of interactive elements.
    var isAnimationComplete by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }

    // Pet visual animation states
    val scale = remember { Animatable(1f) } // For pet's bounce scale
    val bounceHeight = petScript!!.personality.animations.bounceHeight
    val bounceSpeed = petScript!!.personality.animations.bounceSpeed

    val scroll = rememberScrollState() // For scrolling long text messages

    // Pet bounce animation
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f + (bounceHeight * 0.1f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (500 / bounceSpeed).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    // Helper function to generate the pet's spoken message, including personality.
    fun getProcessedMessage(step: DialogStep, config: PetConfig, currentDialogState: PetDialogState): String {
        var processedMessage = step.message
        // Replace placeholders with user data
        currentDialogState.userData.forEach { (key, value) ->
            processedMessage = processedMessage.replace("{$key}", value)
        }

        val style = config.personality.speakingStyle

        // Randomly add a catchphrase
        if (style.catchphrases.isNotEmpty() && Math.random() < 0.3) {
            val catchphrase = style.catchphrases.random()
            processedMessage += " $catchphrase"
        }

        // Randomly add an emoji
        if (style.emojis.isNotEmpty() && Math.random() < 0.4) {
            val emoji = style.emojis.random()
            processedMessage += " $emoji"
        }

        // Apply pet's typical punctuation style
        if (style.punctuation.isNotEmpty() && !processedMessage.endsWith(style.punctuation)) {
            // This logic aims to replace common sentence-ending punctuation or append the style.
            processedMessage = processedMessage.replace(".", style.punctuation)
                .replace("!", style.punctuation)
                .replace("?", "?${style.punctuation.replace(".", "")}") // Handles question marks specially
        }
        return processedMessage
    }


    // Function to advance to the next dialog step
    fun processNextStep() {
        val config = petScript ?: return // Should not happen due to earlier check, but good for safety
        var currentSequence = config.dialogSequences[dialogState.currentSequenceId] ?: return

        // Increment step index for the current sequence
        dialogState.currentStepIndex++

        // Check if end of current sequence is reached
        if (dialogState.currentStepIndex >= currentSequence.steps.size) {
            val nextSequenceId = currentSequence.nextSequence
            if (nextSequenceId != null && config.dialogSequences.containsKey(nextSequenceId)) {
                // Transition to the next sequence
                dialogState.currentSequenceId = nextSequenceId
                dialogState.currentStepIndex = 0
                currentSequence = config.dialogSequences[dialogState.currentSequenceId] ?: return // Update currentSequence
            } else {
                // No next sequence, dialog ends
                onDismiss()
                return
            }
        }

        // Get the new current step
        val step = config.dialogSequences[dialogState.currentSequenceId]?.steps?.getOrNull(dialogState.currentStepIndex)
        if (step == null) {
            // Should not happen if logic is correct, but dismiss if step is invalid
            onDismiss()
            return
        }

        // Evaluate conditions for this step, potentially jumping to a different step
        step.conditions?.forEach { condition ->
            val userDataValue = dialogState.userData[condition.key]
            if (userDataValue == condition.value) {
                val targetStepIndex = currentSequence.steps.indexOfFirst { it.nextStepId == condition.nextStepId }
                if (targetStepIndex != -1) {
                    dialogState.currentStepIndex = targetStepIndex // Jump to the target step
                    // Recurse or re-evaluate if needed, but current logic processes the new step index directly
                    // For simplicity, we'll let the outer logic pick up the new step index.
                    // This means the 'step' variable might be stale if a jump occurs.
                    // To handle this robustly, re-fetch 'step' after potential jumps or restructure.
                    // For now, assuming conditions lead to a step that is then processed sequentially.
                    // A more robust way: if jump happens, call processNextStep recursively or re-fetch.
                    // However, the original code seems to imply the step is processed after this loop.
                    // Let's assume original logic: conditions modify index, then the step at new index is used.
                    currentStep = currentSequence.steps.getOrNull(dialogState.currentStepIndex)
                    // If a condition matched and changed index, currentStep needs to be updated *before* message processing
                    if (currentStep == null) { onDismiss(); return } // Safety for updated index
                    // The message processing uses 'step', which might be the pre-jump step.
                    // This needs careful review based on intended conditional flow.
                    // Sticking to original structure: conditions modify index, then the outer 'step' is used.
                    // This is a potential area for subtle bugs if not handled carefully in script design.
                    // The original code did return@forEach which exits the lambda, not processNextStep.
                    // So the 'step' variable would still be the one fetched before condition evaluation.
                    // This means conditions would set the *next* step index, but the *current* step's message is shown.
                    // Let's assume the intent is to find the target step and process *that*.
                    val newStepAfterCondition = currentSequence.steps.getOrNull(targetStepIndex)
                    if (newStepAfterCondition != null) {
                        currentStep = newStepAfterCondition // Process this step instead
                        // Message processing will use this updated currentStep
                    } else {
                        onDismiss(); return // Invalid conditional jump
                    }
                    return@forEach // Exit condition loop, as one matched.
                }
            }
        }
        // If no condition caused a jump and updated currentStep, use the original 'step'
        if(currentStep != step && step.conditions?.any { dialogState.userData[it.key] == it.value } != true) {
            currentStep = step
        }


        // Process the message for the current step
        currentMessage = getProcessedMessage(currentStep!!, config, dialogState)
        // isAnimationComplete is set to false by the caller or by text animation effect
        userInput = "" // Clear previous user input
    }

    // Function to handle user input from choices or text field
    fun handleInputSubmission(input: String = userInput) {
        val step = currentStep ?: return

        // Store user input if the step expects it
        if (step.expectsInput && step.inputKey != null) {
            dialogState.userData[step.inputKey] = input.ifBlank { "User" } // Default to "User" if blank
        }

        // Handle choices, if any
        if (step.choices.isNotEmpty()) {
            val selectedChoice = step.choices.find { it.text == input }
            if (selectedChoice != null) {
                // Store the chosen value if specified
                if (selectedChoice.storeValue != null && step.inputKey != null) {
                    dialogState.userData[step.inputKey] = selectedChoice.storeValue
                }

                // If choice dictates a jump to a specific next step ID
                if (selectedChoice.nextStepId != null) {
                    val config = petScript ?: return
                    val sequence = config.dialogSequences[dialogState.currentSequenceId] ?: return
                    val targetStepIndex = sequence.steps.indexOfFirst { it.nextStepId == selectedChoice.nextStepId }

                    if (targetStepIndex >= 0) {
                        // Adjust index because processNextStep will increment it
                        dialogState.currentStepIndex = targetStepIndex - 1
                    }
                }
            }
        }
        // After handling input/choice, proceed to the next logical step
        processNextStep()
    }

    // Effect for animating text character by character
    LaunchedEffect(currentMessage) {
        if (currentMessage.isNotEmpty()) {
            val typingSpeed = petScript?.personality?.speakingStyle?.typingSpeed ?: 1f
            progress.snapTo(0f) // Reset progress
            isAnimationComplete = false // Signal that text animation is starting

            progress.animateTo(
                targetValue = currentMessage.length.toFloat(),
                animationSpec = tween(
                    durationMillis = (currentMessage.length * 50 / typingSpeed).toInt().coerceAtLeast(50),
                    easing = LinearEasing
                )
            )
            // Text animation is complete
            isAnimationComplete = true
            displayedUiStep = currentStep // Update the step for UI display now that text is ready
        } else {
            // If currentMessage is empty (e.g. start or an error), ensure states are consistent
            isAnimationComplete = true // Or false, depending on desired state for empty messages
            displayedUiStep = currentStep
        }
    }

    // Initialize the first dialog step
    LaunchedEffect(petScript) {
        // This ensures that if currentStep is null (e.g., on first launch or script change),
        // the dialog processing starts.
        if (currentStep == null) {
            // Set isAnimationComplete to false before processing the first step.
            // This ensures interactive elements wait for the first text animation.
            isAnimationComplete = false
            processNextStep()
            // Note: displayedUiStep will be set by the LaunchedEffect(currentMessage)
            // after the first message's animation completes.
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false, // User must interact with dialog
            usePlatformDefaultWidth = false // Use custom width from modifier
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), // Fill screen for dialog background/dimming
            contentAlignment = Alignment.BottomCenter // Align card to bottom
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    // Section for Pet Image and Dialog Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.pet),
                            contentDescription = "Pet Image",
                            modifier = Modifier
                                .size(120.dp)
                                .scale(scale.value) // Apply bounce animation
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp) // Fixed height for text area
                                .verticalScroll(scroll) // Scroll if text overflows
                        ) {
                            Text(
                                text = animatedText, // Display animated text
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Divider, visible when text animation is complete
                    AnimatedVisibility(
                        visible = isAnimationComplete,
                        enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 200)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = 400, delayMillis = 200),
                                    initialOffsetY = { -it / 2 }
                                ),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    // Interactive section (choices, input, continue button)
                    // This section is also visible only after text animation completes.
                    // Content uses displayedUiStep to ensure correct data during animations.
                    AnimatedVisibility(
                        visible = isAnimationComplete,
                        enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 300)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = 500, delayMillis = 300),
                                    initialOffsetY = { it / 3 }
                                ) +
                                expandVertically(animationSpec = tween(durationMillis = 500, delayMillis = 300)),
                        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically() + shrinkVertically()
                    ) {
                        // Determine what to show based on the displayedUiStep
                        when {
                            // Display choice buttons if the step has choices
                            displayedUiStep?.choices?.isNotEmpty() == true -> {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp), // Max height for choice list
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(displayedUiStep?.choices ?: emptyList()) { index, choice ->
                                        // Animation for individual button appearance (staggered)
                                        val animatedModifierFactor by animateFloatAsState(
                                            targetValue = 1f, // Animate to full visibility/scale
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                delayMillis = index * 100 // Staggered delay
                                            ),
                                            label = "choice_button_animation"
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                isAnimationComplete = false // Hide interactive elements for transition
                                                handleInputSubmission(choice.text)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .scale(animatedModifierFactor)
                                                .alpha(animatedModifierFactor),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        ) {
                                            Text(choice.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }

                            // Display input field if the step expects text input
                            displayedUiStep?.expectsInput == true -> {
                                val slideOffset by animateIntAsState(targetValue = 0, animationSpec = tween(durationMillis = 400), label = "input_slide_offset")
                                val fadeAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(durationMillis = 600), label = "input_fade_alpha")

                                Column(
                                    modifier = Modifier
                                        .offset(y = slideOffset.dp)
                                        .alpha(fadeAlpha)
                                ) {
                                    OutlinedTextField(
                                        value = userInput,
                                        onValueChange = { userInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Your response", style = MaterialTheme.typography.bodyMedium) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        val buttonScale by animateFloatAsState(targetValue = 1f, animationSpec = tween(durationMillis = 300, delayMillis = 200), label = "submit_button_scale")
                                        Button(
                                            onClick = {
                                                isAnimationComplete = false // Hide for transition
                                                handleInputSubmission() // Uses current userInput
                                            },
                                            modifier = Modifier.scale(buttonScale),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Continue", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
                                        }
                                    }
                                }
                            }

                            // Display a simple "Continue" button otherwise
                            else -> {
                                val bounceScale by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "continue_button_scale"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            isAnimationComplete = false // Hide for transition
                                            processNextStep()
                                        },
                                        modifier = Modifier.scale(bounceScale),
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Continue", style = MaterialTheme.typography.labelLarge)
                                        Spacer(modifier = Modifier.width(4.dp)) // Original spacer, check if icon was intended
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}