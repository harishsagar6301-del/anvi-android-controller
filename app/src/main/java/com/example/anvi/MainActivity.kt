
package com.example.anvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize App Check BEFORE using Firebase AI.
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )

            // Connect to the Gemini Developer API.
            val model = Firebase.ai(
                backend = GenerativeBackend.googleAI()
            ).generativeModel("gemini-3.8-flash")

            setContent {
                MaterialTheme {
                    AnviChatScreen(
                        onSendMessage = { prompt, onReply ->
                            lifecycleScope.launch {
                                try {
                                    val response =
                                        model.generateContent(prompt)

                                    onReply(
                                        response.text
                                            ?: "ANVI couldn't generate a reply. Please try again."
                                    )
                                } catch (e: Exception) {
                                    onReply(
                                        "AI connection failed: " +
                                            (e.message
                                                ?: "Check Firebase and internet settings.")
                                    )
                                }
                            }
                        }
                    )
                }
            }
        } catch (e: Exception) {
            setContent {
                MaterialTheme {
                    Text(
                        text = "ANVI startup error: ${e.message}",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
private fun AnviChatScreen(
    onSendMessage: (
        String,
        (String) -> Unit
    ) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Hello! I'm ANVI. Ask me anything.",
                false
            )
        )
    }

    val listState = rememberLazyListState()

    fun sendMessage() {
        val question = input.trim()

        if (question.isEmpty() || loading) return

        messages.add(ChatMessage(question, true))
        input = ""
        loading = true

        val conversation = messages
            .takeLast(12)
            .joinToString("\n") { message ->
                val role = if (message.isUser) "User" else "ANVI"
                "$role: ${message.text}"
            }

        val prompt = """
            You are ANVI, a helpful personal AI assistant.
            Answer clearly, accurately, and naturally.
            Reply in Telugu when the user asks for Telugu.
            Never claim to control a phone feature unless it is implemented.

            Recent conversation:
            $conversation

            Reply to the latest user message as ANVI.
        """.trimIndent()

        onSendMessage(prompt) { reply ->
            messages.add(ChatMessage(reply, false))
            loading = false
        }
    }

    LaunchedEffect(messages.size, loading) {
        val count = messages.size + if (loading) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        Text(
            text = "ANVI",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Your Personal AI Assistant",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (loading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "ANVI is thinking...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message ANVI...")
                },
                enabled = !loading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { sendMessage() }
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { sendMessage() },
                enabled = input.isNotBlank() && !loading
            ) {
                Text("Send")
            }
        }
    }
}
