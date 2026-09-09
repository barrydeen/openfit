package dev.openfit.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.openfit.app.llm.ChatMessage
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.TintedIconCircle

private val ExamplePrompts = listOf(
    "What did I eat this week?",
    "How is my deadlift progress?",
    "Suggest tomorrow's workout"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: CoachViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CoachViewModel(
                    settings = container.settingsRepository,
                    tools = container.coachTools,
                    makeClient = container::chatClient,
                )
            }
        }
    )

    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var confirmReset by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size, state.isThinking, state.error) {
        val count = state.messages.size + (if (state.isThinking) 1 else 0) + (if (state.error != null) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { confirmReset = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Reset conversation")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    if (message.isSystem) {
                        WelcomeCard(onPrompt = vm::onInputChange)
                    } else {
                        MessageBubble(message)
                    }
                }
                if (state.isThinking) {
                    item(key = "typing") { ThinkingIndicator(state.activeTool) }
                }
                state.error?.let { error ->
                    item(key = "error") { ErrorBanner(error, onDismiss = vm::clearError) }
                }
            }

            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        OutlinedTextField(
                            value = state.input,
                            onValueChange = vm::onInputChange,
                            modifier = Modifier.weight(1f),
                            label = { Text("Message your coach") },
                            placeholder = { Text("What's on your mind?") },
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                        )
                        Spacer(Modifier.width(8.dp))
                        val canSend = state.input.isNotBlank() && !state.isThinking
                        Surface(
                            onClick = { vm.send() },
                            enabled = canSend,
                            shape = CircleShape,
                            color = if (canSend) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (canSend) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "AI guidance can be imperfect. Listen to your body.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset conversation?",
            text = "The chat history will be cleared. Your data is not affected.",
            confirmLabel = "Reset",
            onConfirm = {
                vm.resetConversation()
                confirmReset = false
            },
            onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun WelcomeCard(onPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        TintedIconCircle(
            icon = Icons.Filled.SupportAgent,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.height(20.dp))
        ScreenIntro(
            eyebrow = "A LITTLE GUIDANCE",
            title = "Let's find your rhythm.",
            subtitle = "Talk through your training and nutrition with a coach that can draw on your logged meals and workouts."
        )
        Spacer(Modifier.height(20.dp))
        Text("START A CONVERSATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ExamplePrompts.forEach { prompt ->
            SuggestionChip(
                onClick = { onPrompt(prompt) },
                label = { Text(prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) },
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 48.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: CoachUiMessage) {
    val isUser = message.role == ChatMessage.USER
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isUser) 32.dp else 0.dp, end = if (isUser) 0.dp else 24.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp,
                    ),
                )
                .padding(16.dp)
        ) {
            Text(
                text = if (isUser) "YOU" else "OPENFIT COACH",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) textColor else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ThinkingIndicator(activeTool: String?) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(2.dp).size(18.dp), strokeWidth = 2.dp)
        Text(
            text = if (activeTool != null) "Checking your data…" else "Thinking…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Couldn't get a reply", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss error", tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
