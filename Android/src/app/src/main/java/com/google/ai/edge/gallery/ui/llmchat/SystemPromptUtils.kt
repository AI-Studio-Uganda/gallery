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

import com.google.ai.edge.gallery.data.Model

////////////////////////////////////////////////////////////////////////////////////////////////////
// Shared system prompt utilities for all LLM task modules.
//
// DESIGN NOTES — WHY PROMPTS ARE SO SHORT:
// Small on-device models (1B–2B parameters) have very limited instruction-following
// ability. Long or complex system prompts cause them to "think out loud" about the
// instructions, hallucinate fake conversation context, or force responses into a
// narrow topic. The prompts here are tiered by model size:
//   - Tiny  (< 2 GB file): Ultra-minimal, 1 sentence
//   - Large (≥ 2 GB file): Short identity prompt (still concise!)
//
// DeepSeek-R1 is a chain-of-thought reasoning model that outputs visible internal
// reasoning. Its prompt tells it to answer directly.

/**
 * Full AISU Akili identity prompt — ONLY for larger Gemma models (≥ 2 GB).
 * Even this is kept short. Small models cannot handle more than ~2 sentences.
 */
internal const val AISU_SYSTEM_PROMPT_TEXT = """
You are Akili, a helpful AI assistant. Answer questions clearly and concisely. Be friendly. If you are unsure about something, say so.
"""

/**
 * Ultra-minimal prompt for tiny models (< 2 GB), both Gemma and non-Gemma.
 * ONE sentence. Anything more causes tiny models to ramble or hallucinate.
 */
private const val TINY_MODEL_PROMPT = """
You are a helpful assistant. Answer briefly and clearly.
"""

/**
 * Prompt for larger non-Gemma models (≥ 2 GB).
 */
private const val NON_GEMMA_LARGE_PROMPT = """
You are a helpful AI assistant. Answer questions clearly and concisely. If unsure, say so honestly.
"""

/**
 * Special prompt for DeepSeek-R1 reasoning models.
 * R1 outputs its chain-of-thought reasoning as visible text by default.
 * This prompt tells it to skip that and just give the answer.
 */
private const val DEEPSEEK_R1_PROMPT = """
Answer the user directly and concisely. Do not show reasoning steps.
"""

/** Models with file size below this are "tiny" and get minimal prompts. */
private const val TINY_MODEL_SIZE_THRESHOLD = 2_000_000_000L

/**
 * Returns the appropriate system prompt for a given model.
 *
 * Strategy:
 * 1. DeepSeek-R1 → special prompt suppressing chain-of-thought
 * 2. Any tiny model (< 2GB) → ultra-minimal 1-sentence prompt
 * 3. Large Gemma (≥ 2GB) → Akili identity (still short)
 * 4. Large non-Gemma (≥ 2GB) → generic helpful prompt
 */
internal fun getSystemPromptForModel(model: Model): String {
  val name = model.name

  // DeepSeek-R1 always gets the R1-specific prompt.
  if (name.contains("DeepSeek", ignoreCase = true) && name.contains("R1", ignoreCase = true)) {
    return DEEPSEEK_R1_PROMPT.trimIndent()
  }

  val isTiny = model.sizeInBytes in 1 until TINY_MODEL_SIZE_THRESHOLD

  // Tiny models all get the same ultra-minimal prompt regardless of brand.
  if (isTiny) {
    return TINY_MODEL_PROMPT.trimIndent()
  }

  // Larger models can handle slightly more instruction.
  return if (name.contains("Gemma", ignoreCase = true)) {
    AISU_SYSTEM_PROMPT_TEXT.trimIndent()
  } else {
    NON_GEMMA_LARGE_PROMPT.trimIndent()
  }
}
