package io.shubham0204.smollmandroid.ui.screens.model_download

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.FilePlus
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.AppSpacer4W
import io.shubham0204.smollmandroid.ui.components.createAlertDialog

@Preview
@Composable
private fun PreviewImportModelScreen() {
    ImportModelScreen(onPrevSectionClick = {}, checkGGUFFile = { false }, copyModelFile = {})
}

@Composable
fun ImportModelScreen(
    onPrevSectionClick: () -> Unit,
    checkGGUFFile: (Uri) -> Boolean,
    copyModelFile: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            activityResult.data?.let {
                it.data?.let { uri ->
                    if (checkGGUFFile(uri)) {
                        copyModelFile(uri)
                    } else {
                        createAlertDialog(
                            dialogTitle = context.getString(R.string.dialog_invalid_file_title),
                            dialogText = context.getString(R.string.dialog_invalid_file_text),
                            dialogPositiveButtonText = "OK",
                            onPositiveButtonClick = {},
                            dialogNegativeButtonText = null,
                            onNegativeButtonClick = null,
                        )
                    }
                }
            }
        }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.import_model_step_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.import_model_step_des),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    FeatherIcons.FilePlus,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Select a .gguf file from your storage to import it into SmolChat.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                setType("application/octet-stream")
                                putExtra(
                                    DocumentsContract.EXTRA_INITIAL_URI,
                                    Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS
                                    )
                                        .toUri(),
                                )
                            }
                        launcher.launch(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.download_models_select_gguf_button))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = onPrevSectionClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(FeatherIcons.ArrowLeft, contentDescription = null)
                AppSpacer4W()
                Text(stringResource(R.string.button_text_back))
            }
        }
    }
}
