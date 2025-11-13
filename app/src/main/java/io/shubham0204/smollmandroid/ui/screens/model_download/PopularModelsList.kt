package io.shubham0204.smollmandroid.ui.screens.model_download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    Column(verticalArrangement = Arrangement.Center) {
        popularModelsList.forEachIndexed { idx, model ->
            Row(
                Modifier
                    .clickable { onModelSelected(idx) }
                    .fillMaxWidth()
                    .background(
                        if (idx == selectedModelIndex) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (idx == selectedModelIndex) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    color =
                        if (idx == selectedModelIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    text = model.name,
                    style = MaterialTheme.typography.bodySmall,
                )
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
