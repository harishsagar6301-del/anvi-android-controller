
package com.example.anvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.8-flash")

        setContent {
            MaterialTheme {
                AnviChatScreen(
                    onSendMessage = { prompt, onReply ->
                        lifecycleScope.launch {
                            try {
                                val response = model.generateContent(prompt)
                                onReply(
                                    response.text
                                        ?: "I couldn't generate a text reply. Please try again."
                                )
                            } catch (error: Exception) {
                                onReply(
                                    "AI connection failed: " +
                                        (error.message
                                            ?: "Check your internet and Firebase setup.")
                                )
                            }
                        }
                    }
                )
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
                "Hello! I'm ANVI. My AI brain is ready to connect. What can I help you with?",
                false
            )
        )
    }

    val listState = rememberLazyListState()

    fun sendMessage() {
        val question = input.trim()

        if (question.isBlank() || loading) return

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
            Answer accurately, clearly, and naturally.
            You may reply in Telugu when the user requests it.
            Never claim to have performed phone actions unless
            those actions are actually implemented.

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
            text = "Personal AI Assistant",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.padding(top = 12.dp))

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
                        }
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (loading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = " ANVI is thinking...",
                            modifier = Modifier.padding(8.dp)
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
                placeholder = { Text("Ask ANVI anything...") },
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
