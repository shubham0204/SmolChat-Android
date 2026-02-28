package io.shubham0204.smollmandroid.ui.screens.model_download

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import io.shubham0204.smollmandroid.data.LLMModel

@Preview
@Composable
fun PreviewPopularModelsList() {
    PopularModelsList(selectedModelIndex = 0, onModelSelected = {})
}

@Composable
fun PopularModelsList(selectedModelIndex: Int?, onModelSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        popularModelsList.forEachIndexed { idx, model ->
            val isSelected = idx == selectedModelIndex
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModelSelected(idx) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.3f
                ),
                border = if (isSelected) BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                ) else null
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun getPopularModel(index: Int?): LLMModel? = if (index != null) popularModelsList[index] else null

/**
 * A list of models that are shown in the DownloadModelActivity for the user to quickly get started
 * by downloading a model.
 */
private val popularModelsList =
    listOf(
        LLMModel(
            name = "SmolLM2 360M Instruct GGUF",
            url =
                "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
        ),
        LLMModel(
            name = "SmolLM2 1.7B Instruct GGUF",
            url =
                "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
        ),
        LLMModel(
            name = "Qwen2.5 1.5B Q8 Instruct GGUF",
            url =
                "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q8_0.gguf",
        ),
        LLMModel(
            name = "Qwen2.5 3B Q5_K_M Instruct GGUF",
            url =
                "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q5_k_m.gguf",
        ),
        LLMModel(
            name = "Qwen2.5 Coder 3B Instruct Q5 GGUF",
            url =
                "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/qwen2.5-coder-3b-instruct-q5_0.gguf",
        ),
    )
