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

package com.google.ai.edge.gallery.common

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration

/**
 * HuggingFace OAuth configuration for AISU Akili.
 *
 * The app uses HF OAuth to authenticate model downloads from litert-community
 * (public, non-gated repos). You need to create a HuggingFace OAuth app at:
 * https://huggingface.co/settings/applications
 *
 * Set the redirect URI to: ug.aistudio.learn://oauth/callback
 *
 * TODO: Replace clientId with your actual HF OAuth App client ID.
 */
object ProjectConfig {
  // Hugging Face OAuth App Client ID
  // Create yours at: https://huggingface.co/settings/applications
  const val clientId = "38d6986f-25a6-442c-8456-ce520b4834b4"

  // Redirect URI — must match what you registered in your HF OAuth App
  // and the appAuthRedirectScheme in build.gradle.kts
  const val redirectUri = "ug.aistudio.learn://oauth/callback"

  // OAuth 2.0 Endpoints
  private const val authEndpoint = "https://huggingface.co/oauth/authorize"
  private const val tokenEndpoint = "https://huggingface.co/oauth/token"

  val authServiceConfig =
    AuthorizationServiceConfiguration(
      authEndpoint.toUri(),
      tokenEndpoint.toUri(),
    )
}
