package io.shubham0204.smollmandroid.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import compose.icons.FeatherIcons
import compose.icons.feathericons.MoreVertical
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.screens.manage_tasks.TaskOptionsPopup

@Composable
fun TasksList(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    onEditTaskClick: (Task) -> Unit,
    onDeleteTaskClick: (Task) -> Unit,
    onUpdateTaskClick: (Task) -> Unit,
    enableTaskClick: Boolean,
    showTaskOptions: Boolean,
) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(
                task,
                onTaskSelected = { onTaskSelected(task) },
                onDeleteTaskClick = { onDeleteTaskClick(task) },
                onEditTaskClick = { onEditTaskClick(task) },
                onUpdateTask = { onUpdateTaskClick(it) },
                enableTaskClick,
                showTaskOptions,
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onTaskSelected: () -> Unit,
    onDeleteTaskClick: () -> Unit,
    onEditTaskClick: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    enableTaskClick: Boolean = false,
    showTaskOptions: Boolean = true,
) {
    Row(
        modifier =
            if (enableTaskClick) {
                Modifier
                    .fillMaxWidth()
                    .clickable { onTaskSelected() }
            } else {
                Modifier.fillMaxWidth()
            }.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .padding(8.dp),
        ) {
            LargeLabelText(text = task.name)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.systemPrompt,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = task.modelName,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (showTaskOptions) {
            Box {
                var showTaskOptionsPopup by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showTaskOptionsPopup = true },
                ) {
                    Icon(
                        FeatherIcons.MoreVertical,
                        contentDescription = "More Options",
                    )
                }
                if (showTaskOptionsPopup) {
                    TaskOptionsPopup(
                        task.shortcutId != null,
                        onDismiss = { showTaskOptionsPopup = false },
                        onDeleteTaskClick = {
                            task.shortcutId?.let {
                                ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(it))
                                onUpdateTask(task.copy(shortcutId = null))
                                Toast
                                    .makeText(
                                        context,
                                        "Shortcut for task '${task.name}' removed",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                            onDeleteTaskClick()
                            showTaskOptionsPopup = false
                        },
                        onEditTaskClick = {
                            onEditTaskClick()
                            showTaskOptionsPopup = false
                        },
                        onAddTaskShortcut = {
                            val shortcut =
                                ShortcutInfoCompat
                                    .Builder(context, "${task.id}")
                                    .setShortLabel(task.name)
                                    .setIcon(
                                        IconCompat.createWithResource(
                                            context,
                                            R.drawable.task_shortcut_icon,
                                        ),
                                    ).setIntent(
                                        Intent(context, ChatActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            putExtra("task_id", task.id)
                                        },
                                    ).build()
                            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
                            Toast
                                .makeText(
                                    context,
                                    "Shortcut for task '${task.name}' added",
                                    Toast.LENGTH_LONG,
                                ).show()
                            onUpdateTask(task.copy(shortcutId = shortcut.id))
                            showTaskOptionsPopup = false
                        },
                        onRemoveTaskShortcut = {
                            task.shortcutId?.let {
                                ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(it))
                                onUpdateTask(task.copy(shortcutId = null))
                                Toast
                                    .makeText(
                                        context,
                                        "Shortcut for task '${task.name}' removed",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                            showTaskOptionsPopup = false
                        },
                    )
                }
            }
        }
    }
}
