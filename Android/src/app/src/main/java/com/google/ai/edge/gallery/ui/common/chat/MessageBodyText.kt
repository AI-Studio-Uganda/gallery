/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.common.chat

// import com.google.ai.edge.gallery.ui.theme.GalleryTheme
// import androidx.compose.ui.tooling.preview.Preview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.ui.common.MarkdownText
import kotlinx.coroutines.delay

/** Composable function to display the text content of a ChatMessageText. */
@Composable
fun MessageBodyText(message: ChatMessageText, inProgress: Boolean) {
  val context = LocalContext.current
  var copied by remember { mutableStateOf(false) }

  // Reset "copied" checkmark after 2 seconds.
  LaunchedEffect(copied) {
    if (copied) {
      delay(2000)
      copied = false
    }
  }

  val copyIconTint by animateColorAsState(
    targetValue =
      if (copied) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    animationSpec = tween(300),
    label = "copyTint",
  )

  SelectionContainer {
    if (message.side == ChatSide.USER) {
      MarkdownText(
        text = message.content,
        modifier = Modifier.padding(12.dp),
        textColor = Color.White,
        linkColor = Color.White,
      )
    } else if (message.side == ChatSide.AGENT) {
      val cdResponse = stringResource(R.string.cd_model_response_text)
      if (message.isMarkdown) {
        MarkdownText(
          text = message.content,
          modifier =
            Modifier.padding(12.dp).semantics(mergeDescendants = true) {
              contentDescription = cdResponse
              // Only announce when message is complete.
              if (!inProgress) {
                liveRegion = LiveRegionMode.Polite
              }
            },
        )
      } else {
        Text(
          message.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.padding(12.dp).semantics {
              contentDescription = cdResponse
              // Only announce when message is complete.
              if (!inProgress) {
                liveRegion = LiveRegionMode.Polite
              }
            },
        )
      }

      // Copy button — only shown once the response has fully streamed.
      if (!inProgress && message.content.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        ) {
          IconButton(
            onClick = {
              val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Akili response", message.content)
              clipboard.setPrimaryClip(clip)
              copied = true
            },
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = if (copied) Icons.Outlined.Done else Icons.Outlined.ContentCopy,
              contentDescription = if (copied) "Copied" else "Copy response",
              modifier = Modifier.size(16.dp),
              tint = copyIconTint,
            )
          }
        }
      }
    }
  }
}
