package com.example.personalfitnessapp.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.ChatRole
import com.example.personalfitnessapp.data.local.entity.ProposalStatus
import com.example.personalfitnessapp.domain.CoachLinkParser
import com.example.personalfitnessapp.domain.CoachSegment
import com.example.personalfitnessapp.domain.RoutineProposal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachChatScreen(
    onExerciseClick: (String) -> Unit,
    viewModel: CoachChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    // Follow the conversation: new messages and the "thinking" row both extend the list.
    val itemCount = state.messages.size + if (state.isSending || state.error != null) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach") },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask your coach") },
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        viewModel.send(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && !state.isSending,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        },
    ) { padding ->
        if (state.messages.isEmpty() && !state.isSending) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Ask about your training, recovery, or what to do today. " +
                        "The coach knows your recent workouts and can draft routines for you to review.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.messages, key = { it.message.id }) { item ->
                    MessageBubble(
                        item = item,
                        onExerciseClick = onExerciseClick,
                        onSave = { proposal -> viewModel.saveProposal(item.message.id, proposal) },
                        onDismiss = { viewModel.dismissProposal(item.message.id) },
                    )
                }
                if (state.isSending) item { ThinkingRow() }
                state.error?.let { error ->
                    item {
                        ErrorRow(
                            message = error.message,
                            onRetry = if (error.retryable) viewModel::retry else null,
                            onDismiss = viewModel::dismissError,
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear chat?") },
            text = { Text("This deletes the conversation history. Routines you saved are kept.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearChat(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MessageBubble(
    item: CoachMessageUi,
    onExerciseClick: (String) -> Unit,
    onSave: (RoutineProposal) -> Unit,
    onDismiss: () -> Unit,
) {
    val isUser = item.message.role == ChatRole.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = linkedText(item.message.text, onExerciseClick),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item.proposal?.let { proposal ->
            ProposalCard(
                proposal = proposal,
                status = item.message.proposalStatus,
                onExerciseClick = onExerciseClick,
                onSave = { onSave(proposal) },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun linkedText(text: String, onExerciseClick: (String) -> Unit) = buildAnnotatedString {
    val linkStyle = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    )
    CoachLinkParser.parse(text).forEach { segment ->
        when (segment) {
            is CoachSegment.Text -> append(segment.text)
            is CoachSegment.ExerciseLink -> withLink(
                LinkAnnotation.Clickable(segment.exerciseId, linkStyle) { onExerciseClick(segment.exerciseId) }
            ) { append(segment.label) }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: RoutineProposal,
    status: ProposalStatus?,
    onExerciseClick: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(proposal.name, style = MaterialTheme.typography.titleMedium)
            if (proposal.notes.isNotBlank()) Text(proposal.notes, style = MaterialTheme.typography.bodyMedium)
            proposal.exercises.forEach { item ->
                TextButton(onClick = { onExerciseClick(item.exerciseId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${item.exerciseName}  ${item.sets} x ${item.repMin}-${item.repMax}",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            when (status) {
                ProposalStatus.SAVED -> Text("Saved to your routines", style = MaterialTheme.typography.labelLarge)
                ProposalStatus.DISMISSED -> Text("Dismissed", style = MaterialTheme.typography.labelLarge)
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save routine") }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Dismiss") }
                }
            }
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
        Text("Thinking...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: (() -> Unit)?, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onRetry != null) Button(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}
