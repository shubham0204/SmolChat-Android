package io.shubham0204.smollmandroid

import androidx.compose.ui.test.hasInsertTextAtCursorAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.screens.manage_tasks.TasksActivityScreenUI
import org.junit.Rule
import org.junit.Test

class TaskActivityTests {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun clickAddTask_showsNewTask() {
        val dummyModel = LLMModel(id = 1, name = "Test Model")
        rule.setContent {
            TasksActivityScreenUI(
                tasks = emptyList(),
                availableModelsList = listOf(dummyModel),
                getModelFromId = { dummyModel },
                onAddTask = { _, _, _ -> },
                onUpdateTask = {},
                onDeleteTask = {},
            )
        }

        val taskName = "[taskName]"
        val taskSystemPrompt = "[systemPrompt]"
        rule.onNodeWithContentDescription("Add New Task").performClick()
        rule
            .onNode(
                hasInsertTextAtCursorAction()
                    and hasText("Task Name"),
            ).performTextInput(taskName)
        rule
            .onNode(
                hasInsertTextAtCursorAction()
                    and hasText("System Prompt"),
            ).performTextInput(taskSystemPrompt)
        rule.onNode(hasText("Add")).performClick()

        rule.onNode(hasText(taskName)).assertExists()
        rule.onNode(hasText(taskSystemPrompt)).assertExists()
    }
}
