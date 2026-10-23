
package com.example.anvi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import kotlinx.coroutines.launch
import java.util.Locale

private val Cyan = Color(0xFF35DDF5)
private val DarkBackground = Color(0xFF070B12)
private val DarkPanel = Color(0xFF111A27)
private val DarkUserBubble = Color(0xFF123849)
private val LightBackground = Color(0xFFF3F8FC)
private val LightPanel = Color(0xFFFFFFFF)
private val Ink = Color(0xFF152434)

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class MainActivity : ComponentActivity() {

    private var voiceResult: ((String) -> Unit)? = null

    private val speechLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    voiceResult?.invoke(spokenText)
                }
            }
        }

    private val audioPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startSpeechRecognition()
            }
        }

    private fun startSpeechRecognition() {
        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault().toLanguageTag()
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak to ANVI"
                )
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            voiceResult?.invoke(
                "Voice recognition isn't available. Check whether a speech recognition app is installed."
            )
        }
    }

    private fun requestVoiceInput(
        onResult: (String) -> Unit
    ) {
        voiceResult = onResult

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        if (permission == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            audioPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize Firebase before using App Check or Firebase AI.
            Firebase.initialize(context = this)

            // Use the debug provider for development builds.
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )

            val model = Firebase.ai(
                backend = GenerativeBackend.googleAI()
            ).generativeModel("gemini-3.8-flash")

            setContent {
                var darkMode by remember { mutableStateOf(true) }

                MaterialTheme(
                    colorScheme = if (darkMode) {
                        darkColorScheme(
                            primary = Cyan,
                            onPrimary = Color(0xFF00151B),
                            background = DarkBackground,
                            surface = DarkPanel,
                            onSurface = Color(0xFFE7F1FA),
                            onBackground = Color(0xFFE7F1FA),
                            secondary = Cyan
                        )
                    } else {
                        lightColorScheme(
                            primary = Color(0xFF087E99),
                            background = LightBackground,
                            surface = LightPanel,
                            onSurface = Ink,
                            onBackground = Ink,
                            secondary = Color(0xFF087E99)
                        )
                    }
                ) {
                    AnviScreen(
                        darkMode = darkMode,
                        onThemeChange = { darkMode = it },
                        onVoiceRequest = { callback ->
                            requestVoiceInput(callback)
                        },
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
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "ANVI startup error: ${e.message}",
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnviScreen(
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onVoiceRequest: ((String) -> Unit) -> Unit,
    onSendMessage: (String, (String) -> Unit) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hello! I'm ANVI. Ask me anything.", false)
        )
    }

    val listState = rememberLazyListState()

    val background =
        if (darkMode) DarkBackground else LightBackground
    val panel = if (darkMode) DarkPanel else LightPanel
    val assistantBubble =
        if (darkMode) Color(0xFF111C2A) else Color(0xFFE5EEF5)
    val foreground =
        if (darkMode) Color(0xFFE7F1FA) else Ink

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
            .background(background)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = if (darkMode) {
                    Color(0xFF102B3B)
                } else {
                    Color(0xFFDDF7FC)
                },
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        color = Cyan,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ANVI",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = foreground,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "PERSONAL AI ASSISTANT",
                    fontSize = 10.sp,
                    color = if (darkMode) Cyan else Color(0xFF087E99),
                    letterSpacing = 1.5.sp
                )
            }

            TextButton(onClick = { showHistory = true }) {
                Text("History")
            }
            TextButton(onClick = { showSettings = true }) {
                Text("Settings")
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = panel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Cyan, CircleShape)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = if (loading) "ANVI IS THINKING" else "READY TO CHAT",
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = foreground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (darkMode) "DARK UI" else "LIGHT UI",
                    fontSize = 10.sp,
                    color = if (darkMode) Cyan else Color(0xFF087E99)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(messages) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (message.isUser) {
                        Alignment.End
                    } else {
                        Alignment.Start
                    }
                ) {
                    Text(
                        text = if (message.isUser) "YOU" else "ANVI",
                        color = if (darkMode) Cyan else Color(0xFF087E99),
                        fontSize = 10.sp,
                        letterSpacing = 1.3.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 5.dp,
                            vertical = 4.dp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 17.dp,
                            topEnd = 17.dp,
                            bottomStart = if (message.isUser) 17.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 17.dp
                        ),
                        color = if (message.isUser) {
                            if (darkMode) DarkUserBubble
                            else Color(0xFFDDF5FA)
                        } else {
                            assistantBubble
                        },
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = foreground,
                            modifier = Modifier.padding(14.dp),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            if (loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Cyan
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("ANVI is thinking...", color = foreground)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = panel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message ANVI...") },
                    enabled = !loading,
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(17.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { sendMessage() }
                    )
                )

                Spacer(Modifier.width(5.dp))

                FilledTonalButton(
                    onClick = {
                        onVoiceRequest { spokenText ->
                            input = spokenText
                        }
                    },
                    enabled = !loading,
                    contentPadding = PaddingValues(10.dp),
                    shape = CircleShape
                ) {
                    Text("Mic")
                }

                Spacer(Modifier.width(4.dp))

                Button(
                    onClick = { sendMessage() },
                    enabled = input.isNotBlank() && !loading,
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 15.dp
                    ),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text("Send")
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = "ANVI AI • Check important information",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
            color = if (darkMode) Color(0xFF73869A)
                    else Color(0xFF65788A),
            fontSize = 10.sp
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("ANVI Settings") },
            text = {
                Column {
                    Text("Appearance")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (darkMode) "Dark mode" else "Light mode",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = darkMode,
                            onCheckedChange = onThemeChange
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Voice input uses Android speech recognition. " +
                            "Always-listening wake word is not enabled."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Current chat") },
            text = {
                if (messages.isEmpty()) {
                    Text("No messages yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp)
                    ) {
                        items(messages) { message ->
                            Text(
                                text = (if (message.isUser) "You: " else "ANVI: ") +
                                    message.text,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) {
                    Text("Close")
                }
            }
        )
    }
}
