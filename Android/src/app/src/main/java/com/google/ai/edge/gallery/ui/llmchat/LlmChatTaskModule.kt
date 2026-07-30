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

package com.google.ai.edge.gallery.ui.llmchat
 
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskDataForBuiltinTask
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.common.chat.SendMessageTrigger
import com.google.ai.edge.litertlm.Contents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Starter prompts shown on the empty chat screen (like Claude/ChatGPT/Gemini). */
private val AKILI_STARTER_PROMPTS = listOf(
  "Help me write a professional CV",
  "Explain how AI works in simple English",
  "Give me 5 business ideas I can start today",
  "Help me understand compound interest",
  "Write an email to apply for an internship",
  "Teach me the basics of Python",
  "What are general ways to prevent malaria?",
  "What is the role of AI Studio Uganda?",
  "Give me a study plan for my exams",
)

class LlmChatTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_CHAT,
      label = "Akili Chat",
      category = Category.LLM,
      icon = Icons.Outlined.Forum,
      models = mutableListOf(),
      description = "Offline AI companion to talk to at anytime anywhere",
      shortDescription = "Chat with offline AI",
      defaultSystemPrompt = AISU_SYSTEM_PROMPT_TEXT.trimIndent(),
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = model.llmSupportImage,
      supportAudio = model.llmSupportAudio,
      systemInstruction = Contents.of(getSystemPromptForModel(model)),
      onDone = onDone,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    var sendMessageTrigger by remember { mutableStateOf<SendMessageTrigger?>(null) }

    val uiState by myData.modelManagerViewModel.uiState.collectAsState()
    val selectedModel = uiState.selectedModel
    val showImagePicker = selectedModel.llmSupportImage
    val showAudioPicker = selectedModel.llmSupportAudio

    LlmChatScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      sendMessageTrigger = sendMessageTrigger,
      showImagePicker = showImagePicker,
      showAudioPicker = showAudioPicker,
      curSystemPrompt = AISU_SYSTEM_PROMPT_TEXT.trimIndent(),
      emptyStateComposable = { model ->
        AkiliChatEmptyState(
          model = model,
          onPromptSelected = { prompt ->
            sendMessageTrigger =
              SendMessageTrigger(
                model = model,
                messages = listOf(ChatMessageText(content = prompt, side = ChatSide.USER)),
              )
          },
        )
      },
    )
  }
}

class LlmAskImageTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_ASK_IMAGE,
      label = "Image Chat",
      category = Category.LLM,
      icon = Icons.Outlined.Forum,
      models = mutableListOf(),
      description = "Chat with AI about images",
      shortDescription = "Image understanding",
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = true,
      supportAudio = model.llmSupportAudio,
      systemInstruction = Contents.of(getSystemPromptForModel(model)),
      onDone = onDone,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmAskImageScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
    )
  }
}

class LlmAskAudioTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_ASK_AUDIO,
      label = "Audio Chat",
      category = Category.LLM,
      icon = Icons.Outlined.Forum,
      models = mutableListOf(),
      description = "Chat with AI about audio clips",
      shortDescription = "Audio understanding",
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = model.llmSupportImage,
      supportAudio = true,
      systemInstruction = Contents.of(getSystemPromptForModel(model)),
      onDone = onDone,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmAskAudioScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
    )
  }
}

/** The welcome / empty state screen shown before any messages are sent. */
@Composable
fun AkiliChatEmptyState(
  model: Model,
  onPromptSelected: (String) -> Unit,
) {
  // Determine if this model is small (< 1 GB) for the hallucination banner.
  val isSmallModel = model.sizeInBytes in 1..999_999_999L
  var showDisclaimer by remember(model.name) { mutableStateOf(isSmallModel) }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.align(Alignment.Center)
          .fillMaxWidth()
          .padding(bottom = 80.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Greeting
      Text(
        text = "Welcome! How can I assist you?",
        style =
          MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      if (model.llmSupportImage || model.llmSupportAudio) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Multimodal Support (Images/Audio)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Note: This is an offline AI assistant with general knowledge. Answers may not always be accurate — please verify important information.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
      )

      // Hallucination disclaimer — only for small models, dismissible.
      if (showDisclaimer) {
        Spacer(modifier = Modifier.height(10.dp))
        HallucinationBanner(onDismiss = { showDisclaimer = false })
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Starter prompt chips — scrollable horizontal row
      Text(
        text = "Try asking...",
        style =
          MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp).fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(10.dp))

      LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(AKILI_STARTER_PROMPTS) { prompt ->
          PromptChip(
            text = prompt,
            onClick = { onPromptSelected(prompt) },
          )
        }
      }
    }
  }
}

/** Dismissible yellow banner warning users that small models may hallucinate. */
@Composable
private fun HallucinationBanner(onDismiss: () -> Unit) {
  Box(
    modifier = Modifier
      .padding(horizontal = 24.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
      .clickable(onClick = onDismiss)
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Column {
      Text(
        text = "Small model notice",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "This is a compact model. It may sometimes give inaccurate or made-up answers. Always verify important information.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Tap to dismiss",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
      )
    }
  }
}

/** Suggested follow-up prompts shown after a completed AI response. */
private val FOLLOW_UP_PROMPTS = listOf(
  "Can you explain that more simply?",
  "Give me a real-world example",
  "What are the key takeaways?",
  "Summarize this in 3 bullet points",
  "How is this relevant to students?",
  "What should I learn next?",
)

@Composable
private fun PromptChip(text: String, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
          shape = RoundedCornerShape(20.dp),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal object LlmChatTaskModule {
  @Provides
  @IntoSet
  fun provideChatTask(): CustomTask {
    return LlmChatTask()
  }

  @Provides
  @IntoSet
  fun provideAskImageTask(): CustomTask {
    return LlmAskImageTask()
  }

  @Provides
  @IntoSet
  fun provideAskAudioTask(): CustomTask {
    return LlmAskAudioTask()
  }
}
