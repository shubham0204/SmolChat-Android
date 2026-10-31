/*
 * Copyright (C) 2024 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.ui.screens.chat.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Copy
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.screens.chat.MessageSegment
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageText(
    message: Spanned,
    modifier: Modifier = Modifier,
    textSize: Float,
    textColor: Int,
    onLongClick: () -> Unit,
) {
    AndroidView(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onLongClick),
        factory = {
            val textView = TextView(it)
            textView.textSize = textSize
            textView.setTextColor(textColor)
            textView.autoLinkMask = Linkify.WEB_URLS
            textView.highlightColor = Color.YELLOW
            textView
        },
        update = { it.text = message },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeBlockMessageText(
    segment: MessageSegment.CodeSegment,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF1E1E1E))
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = segment.language.ifBlank { "code" },
                color = androidx.compose.ui.graphics.Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as ClipboardManager
                            val clip =
                                ClipData.newPlainText("Copied code snippet", segment.rawCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                "Code snippet copied",
                                Toast.LENGTH_SHORT,
                            ).show()
                            isCopied = true
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isCopied) FeatherIcons.Check else FeatherIcons.Copy,
                    contentDescription = stringResource(R.string.chat_message_copy),
                    tint = androidx.compose.ui.graphics.Color.LightGray,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.chat_message_copy),
                    color = androidx.compose.ui.graphics.Color.LightGray,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        AndroidView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            factory = { ctx ->
                TextView(ctx).apply {
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    typeface = ResourcesCompat.getFont(ctx, R.font.jetbrains_mono)
                    setOnLongClickListener {
                        onLongClick()
                        true
                    }
                }
            },
            update = { textView ->
                textView.text = segment.highlightedCode
            },
        )
    }
}
