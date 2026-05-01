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

package com.google.ai.edge.gallery.ui.common

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// AISU Akili brand teal colour — matches the Akili icon palette.
private val AkiliTeal = Color(0xFF1DB089)

/**
 * AISU Akili thinking indicator — three teal dots that pulse sequentially.
 * Replaces the original Edge Gallery 2x2 rotating grid of Google-branded shapes.
 *
 * Shown when:
 * - A model is initialising / loading
 * - Akili is generating a response ("thinking")
 * - A model download is in progress
 */
@Composable
fun RotationalLoader(size: Dp) {
  val dotSize = size * 0.28f
  val dotSpacing = size * 0.12f

  val infiniteTransition = rememberInfiniteTransition(label = "akili-dots")

  // Each dot pulses with a phase offset so they animate in sequence (left → centre → right).
  @Composable
  fun pulseDot(delayMs: Int): Float {
    val scale by infiniteTransition.animateFloat(
      initialValue = 0.5f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
        animation = tween(600, delayMillis = delayMs, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "dot-scale-$delayMs",
    )
    return scale
  }

  val scale0 = pulseDot(0)
  val scale1 = pulseDot(180)
  val scale2 = pulseDot(360)

  Row(
    horizontalArrangement = Arrangement.spacedBy(dotSpacing),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .size(size)
      .clearAndSetSemantics {},
  ) {
    listOf(scale0, scale1, scale2).forEach { scale ->
      Box(
        modifier = Modifier
          .size(dotSize)
          .graphicsLayer { scaleX = scale; scaleY = scale }
          .clip(CircleShape)
          .background(AkiliTeal),
      )
    }
  }
}
