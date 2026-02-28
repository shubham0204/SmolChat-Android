package io.shubham0204.smollmandroid.ui.screens.model_download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Download
import compose.icons.feathericons.Globe
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.AppSpacer4W

@Preview
@Composable
private fun PreviewDownloadModelScreen() {
    DownloadModelScreen(
        onDownloadModelClick = {},
        onNextSectionClick = {},
        onHFModelSelectClick = {},
    )
}

@Composable
fun DownloadModelScreen(
    onHFModelSelectClick: () -> Unit,
    onNextSectionClick: () -> Unit,
    onDownloadModelClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPopularModelIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.download_model_step_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.download_model_step_des),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PopularModelsList(
            selectedModelIndex = selectedPopularModelIndex,
            onModelSelected = { selectedPopularModelIndex = it },
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPopularModelIndex != null,
            onClick = { onDownloadModelClick(selectedPopularModelIndex!!) },
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(FeatherIcons.Download, contentDescription = null)
            AppSpacer4W()
            Text(stringResource(R.string.download_model_download))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.download_model_step_hf_browse),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onHFModelSelectClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(FeatherIcons.Globe, contentDescription = null)
                AppSpacer4W()
                Text(stringResource(R.string.download_model_browse_hf))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.download_model_next_step_des),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onNextSectionClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.button_text_next))
                    AppSpacer4W()
                    Icon(FeatherIcons.ArrowRight, contentDescription = null)
                }
            }
        }
    }
}
