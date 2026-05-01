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

// AISU Akili: Agent Skills DISABLED — too complex for a beginner student app.
// The task requires tool-calling capabilities and a specific system prompt.
// To re-enable, uncomment the module block below.

package com.google.ai.edge.gallery.customtasks.agentchat

/*
import android.content.Context
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskDataForBuiltinTask
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.tool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class AgentChatTask @Inject constructor() : CustomTask {
  private val agentTools = AgentTools()

  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_AGENT_CHAT,
      label = "Agent Skills",
      category = Category.LLM,
      iconVectorResourceId = R.drawable.agent,
      newFeature = true,
      models = mutableListOf(),
      description = "Chat with on-device large language models with skills and tools",
      shortDescription = "Complete agentic tasks with chat",
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    agentTools.skillManagerViewModel.loadSkills {
      LlmChatModelHelper.initialize(
        context = context,
        model = model,
        supportImage = true,
        supportAudio = true,
        onDone = onDone,
        systemInstruction =
          if (agentTools.skillManagerViewModel.getSelectedSkills().isEmpty()) {
            null
          } else {
            agentTools.skillManagerViewModel.getSystemPrompt(task.defaultSystemPrompt)
          },
        tools = listOf(tool(agentTools)),
        enableConversationConstrainedDecoding = true,
      )
    }
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
    AgentChatScreen(
      task = task,
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      agentTools = agentTools,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal object AgentChatTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return AgentChatTask()
  }
}
*/
