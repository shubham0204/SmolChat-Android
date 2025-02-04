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

package io.shubham0204.smollmandroid.ui.screens.chat

import android.graphics.Color
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ChatMessageText(
    message: Spanned,
    modifier: Modifier = Modifier,
    textSize: Float,
    textColor: Int,
) {
    AndroidView(
        modifier = modifier,
        factory = {
            val textView = TextView(it)
            textView.textSize = textSize
            textView.setTextColor(textColor)
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.linksClickable = true
            textView.autoLinkMask = Linkify.WEB_URLS
            textView.highlightColor = Color.YELLOW
            textView
        },
        update = { it.text = message },
    )
}
