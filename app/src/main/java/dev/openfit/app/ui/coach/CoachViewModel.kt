package dev.openfit.app.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.coach.CoachAgent
import dev.openfit.app.coach.CoachTool
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.llm.ChatClient
import dev.openfit.app.llm.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CoachUiMessage(
    val id: Long,
    val role: String,
    val text: String,
    val isSystem: Boolean = false,
)

data class CoachUiState(
    val messages: List<CoachUiMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    val activeTool: String? = null,
    val error: String? = null,
)

class CoachViewModel(
    private val settings: SettingsRepository,
    tools: List<CoachTool>,
    private val makeClient: (MacroSettings) -> ChatClient,
) : ViewModel() {

    val macroSettings: StateFlow<MacroSettings> =
        settings.macroSettings.stateIn(viewModelScope, SharingStarted.Eagerly, MacroSettings())

    private val _state = MutableStateFlow(CoachUiState(messages = listOf(welcomeMessage())))
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    private var nextId = 1L
    private val agent = CoachAgent(tools = tools, onToolRun = { name ->
        _state.value = _state.value.copy(activeTool = name)
    })

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(input = text)
    }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.isThinking) return

        appendMessage(role = ChatMessage.USER, text = text)
        _state.value = _state.value.copy(input = "", isThinking = true, activeTool = null, error = null)

        viewModelScope.launch {
            val settings = macroSettings.value
            if (settings.apiKey.isBlank()) {
                _state.value = _state.value.copy(
                    isThinking = false,
                    activeTool = null,
                    error = "Set your API key in Settings before using the coach.",
                )
                return@launch
            }
            try {
                val client = makeClient(settings)
                val reply = agent.reply(client, text)
                appendMessage(role = ChatMessage.ASSISTANT, text = reply)
                _state.value = _state.value.copy(isThinking = false, activeTool = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isThinking = false,
                    activeTool = null,
                    error = e.message ?: "Something went wrong.",
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetConversation() {
        agent.reset()
        nextId = 1L
        _state.value = CoachUiState(messages = listOf(welcomeMessage()))
    }

    private fun appendMessage(role: String, text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + CoachUiMessage(id = nextId++, role = role, text = text)
        )
    }

    private fun welcomeMessage(): CoachUiMessage =
        CoachUiMessage(
            id = 0L,
            role = ChatMessage.ASSISTANT,
            text = "Hi! I'm your OpenFit coach. Ask me about your training or nutrition — for example, " +
                "\"What did I eat this week?\" or \"How is my deadlift progress?\"",
            isSystem = true,
        )
}
