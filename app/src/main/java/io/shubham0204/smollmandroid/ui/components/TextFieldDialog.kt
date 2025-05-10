package io.shubham0204.smollmandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.shubham0204.smollmandroid.R

private var title = ""
private var defaultText = ""
private var placeholder = ""
private var buttonText = ""
private lateinit var buttonOnClick: ((String) -> Unit)
private val textFieldDialogShowStatus = mutableStateOf(false)

@Composable
fun TextFieldDialog() {
    var visible by remember { textFieldDialogShowStatus }
    if (visible) {
        Surface {
            Dialog(onDismissRequest = { visible = false }) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(8.dp),
                            ).padding(16.dp),
                ) {
                    var text by remember { mutableStateOf(defaultText) }
                    var label by remember { mutableStateOf("") }
                    var isError by remember { mutableStateOf(false) }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(placeholder) },
                        isError = isError,
                        label = { Text(label) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        Button(onClick = {
                            if (text.trim().isEmpty()) {
                                label = "The field cannot be empty"
                                isError = true
                            } else {
                                buttonOnClick(text)
                                visible = false
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            Text(buttonText)
                        }
                        Button(onClick = { visible = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Text(stringResource(R.string.dialog_err_close))
                        }
                    }
                }
            }
        }
    }
}

fun createTextFieldDialog(
    dialogTitle: String,
    dialogDefaultText: String,
    dialogPlaceholder: String,
    dialogButtonText: String,
    onButtonClick: ((String) -> Unit),
) {
    title = dialogTitle
    defaultText = dialogDefaultText
    placeholder = dialogPlaceholder
    buttonOnClick = onButtonClick
    buttonText = dialogButtonText
    textFieldDialogShowStatus.value = true
}
