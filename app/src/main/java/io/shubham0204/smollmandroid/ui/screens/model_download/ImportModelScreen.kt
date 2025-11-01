package io.shubham0204.smollmandroid.ui.screens.model_download

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.createAlertDialog

@Preview
@Composable
private fun PreviewImportModelScreen() {
    ImportModelScreen(
        onPrevSectionClick = {},
        checkGGUFFile = { false },
        copyModelFile = { },
    )
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
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.import_model_step_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.import_model_step_des),
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        setType("application/octet-stream")
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            Environment
                                .getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS,
                                ).toUri(),
                        )
                    }
                launcher.launch(intent)
            },
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(stringResource(R.string.download_models_select_gguf_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            OutlinedButton(
                onClick = onPrevSectionClick,
            ) {
                Icon(FeatherIcons.ArrowLeft, contentDescription = null)
                Text(stringResource(R.string.button_text_back))
            }
        }
    }
}
