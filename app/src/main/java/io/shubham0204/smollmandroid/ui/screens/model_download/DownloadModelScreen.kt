package io.shubham0204.smollmandroid.ui.screens.model_download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
        onHFModelSelectClick = {}
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
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.download_model_step_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.download_model_step_des),
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        PopularModelsList(
            selectedModelIndex = selectedPopularModelIndex,
            onModelSelected = { selectedPopularModelIndex = it },
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            enabled = selectedPopularModelIndex != null,
            // selectedPopularModelIndex will never be null below
            onClick = { onDownloadModelClick(selectedPopularModelIndex!!) },
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(FeatherIcons.Download, contentDescription = "Download Selected Model")
            AppSpacer4W()
            Text(stringResource(R.string.download_model_download))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "OR",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.download_model_step_hf_browse),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onHFModelSelectClick,
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(FeatherIcons.Globe, contentDescription = "Download Selected Model")
            AppSpacer4W()
            Text(stringResource(R.string.download_model_browse_hf))
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.Bottom) {
            Text(
                text = stringResource(R.string.download_model_next_step_des),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = onNextSectionClick,
            ) {
                Icon(FeatherIcons.ArrowRight, contentDescription = null)
                Text(stringResource(R.string.button_text_next))
            }
        }
    }
}
