/*
 * Copyright 2025 AI Studio Uganda
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

package com.google.ai.edge.gallery

import android.os.Bundle

/**
 * No-op analytics stub for AISU Akili.
 *
 * All AI processing runs 100% on-device. No usage data is collected or sent anywhere.
 * This class satisfies all firebaseAnalytics?.logEvent(...) call-sites throughout the codebase
 * without importing any Firebase dependencies.
 */
class AisuAnalytics {
  /** No-op: logs nothing. Satisfies all call-sites that previously used FirebaseAnalytics. */
  fun logEvent(name: String, params: Bundle?) {
    // intentionally empty — AISU does not collect analytics
  }
}

/**
 * Global analytics instance. Non-null so that `?.logEvent(...)` safe-calls compile cleanly.
 * The underlying implementation is a no-op.
 */
val firebaseAnalytics: AisuAnalytics = AisuAnalytics()

enum class GalleryEvent(val id: String) {
  CAPABILITY_SELECT(id = "capability_select"),
  MODEL_DOWNLOAD(id = "model_download"),
  GENERATE_ACTION(id = "generate_action"),
  BUTTON_CLICKED(id = "button_clicked"),
  SKILL_MANAGEMENT(id = "skill_management"),
  SKILL_EXECUTION(id = "skill_execution"),
}
