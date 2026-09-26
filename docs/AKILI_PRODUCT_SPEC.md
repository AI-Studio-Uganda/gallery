# Akili — Product Specification

**Offline AI for Schools, by AI Studio Uganda**

| | |
|---|---|
| Status | Draft v0.1, for team review |
| Branch | `marv_dev` (based on `akili`) |
| Source concept | *Offline AI for Schools: Market and Product Concept* (AI Studio Uganda) |
| Scope | The complete, real-world product: not only the first release |

---

## Contents

1. [Purpose of this document](#1-purpose-of-this-document)
2. [Where Akili is today](#2-where-akili-is-today)
3. [Product vision](#3-product-vision)
4. [Users and roles](#4-users-and-roles)
5. [System overview: the three tiers](#5-system-overview-the-three-tiers)
6. [Module catalogue](#6-module-catalogue)
7. [Features by user](#7-features-by-user)
8. [Workflows](#8-workflows)
9. [The answer pipeline and learning modes](#9-the-answer-pipeline-and-learning-modes)
10. [School Learning Packs](#10-school-learning-packs)
11. [Policies and safety](#11-policies-and-safety)
12. [Models and device tiers](#12-models-and-device-tiers)
13. [Data model](#13-data-model)
14. [Technical architecture](#14-technical-architecture)
15. [Security, privacy and compliance](#15-security-privacy-and-compliance)
16. [Operations: how Akili is managed](#16-operations-how-akili-is-managed)
17. [Quality and evaluation](#17-quality-and-evaluation)
18. [Editions, pricing and go-to-market](#18-editions-pricing-and-go-to-market)
19. [Build roadmap](#19-build-roadmap)
20. [Risks](#20-risks)
21. [Open decisions](#21-open-decisions)
22. [Immediate next steps](#22-immediate-next-steps)

---

## 1. Purpose of this document

This is the reference description of what Akili is meant to become: every module, feature and workflow of a product that a real school can buy, deploy and rely on every day. It is written so that:

- **Leadership** can see the whole product and make the decisions in [§21](#21-open-decisions).
- **Engineers and interns** know what each module does, how modules depend on each other, and what already exists in the code.
- **Sales, training and support** understand how the product is deployed and run in schools.

Each module is labelled with its status in the code:

| Label | Meaning |
|---|---|
| **EXISTS** | Already in the `akili` codebase and usable |
| **EXTEND** | Partly exists (usually from Google's Edge Gallery); needs significant work |
| **NEW** | Must be built |

---

## 2. Where Akili is today

Akili is a fork of Google's open-source **AI Edge Gallery**. The `akili` branch (4 commits, Apr–Jul 2026) turned it into an AI Studio Uganda app. This section records what that work did and what it left, so everyone starts from the same picture.

### 2.1 What Akili already has

| Area | Current state |
|---|---|
| Identity | App name **Akili by AI Studio Uganda**, package `ug.aistudio.learn`, version 1.0.0, new icon, splash screen, colours and welcome dialog |
| Privacy | Firebase analytics and push messaging removed; analytics is a no-op stub (`Analytics.kt`); push permissions commented out |
| Models | A model list **bundled inside the app** (`assets/model_allowlist_full.json`), so the list loads offline. Seven models: Gemma 3 1B, Qwen 2.5 1.5B, DeepSeek-R1-Distill 1.5B, Gemma 3n E2B/E4B, Gemma 4 E2B/E4B |
| Prompts | Short system prompts sized to each model (`ui/llmchat/SystemPromptUtils.kt`), identifying the assistant as "Akili" |
| Features on | AI Chat, Prompt Lab ("one-off" tasks), Ask Image, Ask Audio |
| Features off | Agent Skills, Mobile Actions and Tiny Garden are commented out |
| Images | Photos are resized to 512 px and sent as JPEG, which is faster on low-end phones |
| Chat UI | Copy button on answers, three random follow-up suggestion chips, capability badges on model cards |
| Hugging Face | Real OAuth client ID and redirect `ug.aistudio.learn://oauth/callback` filled in |

### 2.2 Issues found in the current code

These should be fixed before any new feature work (see [§22](#22-immediate-next-steps)).

| # | Issue | Where | Impact |
|---|---|---|---|
| 1 | `org.gradle.java.home` is hard-coded to a **Windows** JDK path | `Android/src/gradle.properties` | The build fails on every other machine, including CI (Linux) and Mac/Linux developers |
| 2 | Lint is disabled (`abortOnError = false`, `checkReleaseBuilds = false`) | `app/build.gradle.kts` | Real problems will ship unnoticed |
| 3 | `akili_icon.png` is **4.6 MB** | `res/drawable/` | Inflates the APK for an icon; should be under 100 KB |
| 4 | Small models carry `minDeviceMemoryInGb: 6` | `model_allowlist_full.json` | Gemma 3 1B (0.58 GB) shows a memory warning on the 3–4 GB phones it is ideal for |
| 5 | No `estimatedPeakMemoryInBytes` or `llmMaxContextLength` values for any model | same | The app cannot judge fit or context size per device |
| 6 | Fallback model-list URL still points to **Google's** repository | `ModelManagerViewModel.kt` | If the bundled list fails, the app silently follows Google's list |
| 7 | Follow-up suggestions are random, not based on the answer | `ChatPanel.kt` | Looks generic; can suggest irrelevant follow-ups |
| 8 | The prompt is a general assistant: no curriculum, no pedagogy, no grounding | `SystemPromptUtils.kt` | Answers are unverified and not tied to the syllabus |
| 9 | Release build signed with the **debug key**, no code shrinking | `app/build.gradle.kts` | Cannot be published to the Play Store as is |
| 10 | CI builds only on pushes to `main` | `.github/workflows/build_android.yaml` | Nothing checks `akili`/`marv_dev` |
| 11 | No automated tests | whole project | Every change risks regressions |
| 12 | Package names and copyright headers still say `com.google.ai.edge.gallery` / Google LLC in most files | whole project | Fine for now; the fork must keep Google's Apache-2.0 notices but add our own |

### 2.3 What this means

Akili today is a **branded offline chatbot**. That is a real achievement and the right foundation, but it is not yet a school product. Everything that makes it trustworthy and useful for a school (curriculum content, grounded answers, roles, teacher tools, assessment, deployment and management) is still to be built. The rest of this document describes that product.

---

## 3. Product vision

> **Akili is a school's own offline AI system.** Every learner gets a patient, curriculum-grounded tutor, and every teacher gets an assistant that saves time. It runs on the phones, tablets and computers schools already have, works without internet, and is controlled by the school.

### 3.1 Principles

1. **Offline first.** Every daily task works with no internet. Connectivity is only used for updates and optional reporting.
2. **The curriculum decides, the AI explains.** Facts, answers and marks come from approved content, answer keys and tools. The model's job is to explain, guide and adapt, never to be the source of truth.
3. **Teachers stay in charge.** Teachers review content, approve marks and decide when answers are available. Akili gives teachers time back; it does not replace them.
4. **Learning, not answer delivery.** The default is to guide learners to reason, not to hand out answers.
5. **Works on real hardware.** Designed for 3–4 GB phones, shared tablets, unreliable power and no IT staff.
6. **Configured, not rebuilt.** Each school gets its own branding, content, roles and policies through configuration: one codebase for every customer.
7. **Private by default.** Learner data stays on the device or the school hub unless the school decides otherwise.

### 3.2 What Akili is not

- Not a general internet chatbot.
- Not a replacement for teachers, counsellors, clinicians or safeguarding staff.
- Not an exam-cheating tool: assessment modes are locked and teacher-controlled.

---

## 4. Users and roles

| Role | Who | Main goal | Typical device |
|---|---|---|---|
| **Learner** | Pupils and students (P5–S6, TVET trainees, university students) | Understand, practise, prepare for exams | Shared school tablet, family phone, own phone |
| **Teacher** | Subject and class teachers | Plan lessons, create exercises, mark, find who needs help | Own phone, school laptop |
| **Head of Department** | Senior subject teacher | Approve content and assessments for the department | Own phone or laptop |
| **School Administrator** | Head teacher, deputy, bursar, secretary | Policies, notices, school information, oversight | Laptop, phone |
| **Counsellor** | Guidance and counselling staff | Careers and wellbeing information, referrals | Phone or laptop |
| **IT Champion** | Trained teacher or technician at the school | Hub, devices, updates, first-line support | Laptop + hub |
| **Parent/Guardian** | Family of learners | Understand progress, support learning at home | Own phone (receives summaries through the school) |
| **AI Studio staff** | Content, support, deployment, engineering | Build packs, deploy, support, maintain | Cloud console |
| **Content partner** | Publishers, NCDC-aligned authors, NGOs | Distribute licensed content | Cloud console (Pack Studio) |

A person can hold more than one role (e.g. a teacher who is also the IT Champion). Permissions are the union of their roles, limited by school policy.

---

## 5. System overview: the three tiers

```
┌──────────────────────────────────────────────────────────────────────┐
│  TIER 3 — AKILI CLOUD (AI Studio Uganda)        online, used rarely   │
│  Pack Studio · Tenant & Licences · Model Lab · Fleet · Marketplace ·  │
│  Aggregated analytics · Support desk                                  │
└──────────────────────────────▲───────────────────────────────────────┘
                               │ internet when available,
                               │ or USB / SD card / courier
┌──────────────────────────────┴───────────────────────────────────────┐
│  TIER 2 — AKILI HUB (one per school)             offline, school LAN  │
│  Wi-Fi hotspot · pack & model cache · rosters · teacher dashboard ·   │
│  exam controller · backups · optional larger "hub model"              │
└──────────────────────────────▲───────────────────────────────────────┘
                               │ school Wi-Fi (no internet needed)
┌──────────────────────────────┴───────────────────────────────────────┐
│  TIER 1 — AKILI APP (Android phones & tablets; later laptops)         │
│  Learner · Teacher · Admin · Counsellor · IT experiences              │
│  On-device model · packs · policies · encrypted learner records       │
└──────────────────────────────────────────────────────────────────────┘
```

**Why three tiers.** The app alone cannot do what a school needs:

- **Distribution.** A 2–4 GB model downloaded 200 times is impossible on school bandwidth. The hub downloads it once (or receives it on USB) and serves it over the LAN.
- **Teacher insight.** Teachers need to see a class's progress. The hub collects it locally without the internet.
- **Assessment.** Starting, timing and collecting a test across a room needs a coordinator.
- **Quality on cheap devices.** The hub can run a larger, more accurate model that low-end phones use over Wi-Fi, which is still fully offline from the internet.
- **Management.** One place for the school to see devices, versions and licences.

The **app must still work alone.** A learner at home with no hub and no internet can use every learner feature with the content already on their device. The hub adds features; it is never required for daily learning.

---

## 6. Module catalogue

### 6.1 Module map

```
AKILI APP
├── Core platform
│   ├── C1  AI Runtime ...................... EXTEND
│   ├── C2  Knowledge Engine ................ NEW
│   ├── C3  Pack Manager .................... NEW
│   ├── C4  Identity & Roles ................ NEW
│   ├── C5  Policy & Safety Engine .......... NEW
│   ├── C6  Learner Record & Mastery ........ NEW
│   ├── C7  Sync Agent ...................... NEW
│   ├── C8  Device & Licence ................ NEW
│   ├── C9  Tenant Config & Branding ........ EXTEND
│   ├── C10 Accessibility & Language ........ EXTEND
│   ├── C11 Tool Layer ...................... EXTEND
│   ├── C12 Quality Feedback ................ NEW
│   └── C13 Notifications & Inbox ........... NEW
├── Learner features ........................ F1–F10
├── Teacher features ........................ T1–T8
├── Administration features ................. A1–A5
└── Specialised editions .................... E1–E4

AKILI HUB ................................... H1–H9 (NEW)
AKILI CLOUD ................................. S1–S9 (NEW)
```

### 6.2 Core platform modules (inside the app)

Every feature is built on these. None of them has its own screen except where noted; they are services used by the features.

#### C1 — AI Runtime · EXTEND

**Purpose.** Load and run language, vision and speech models on the device, and fall back to the hub model when appropriate.

**Exists today.** `LlmModelHelper` interface and LiteRT-LM implementation, model download (`DownloadWorker`), model import, CPU/GPU selection, image and audio input, streaming responses, benchmark screen.

**To build.**
- **Model profiles** (low / medium / high) chosen automatically from device RAM, storage and a quick benchmark ([§12](#12-models-and-device-tiers)).
- **Hub inference client.** When on school Wi-Fi and policy allows, send the prompt to the hub's larger model; fall back to on-device if the hub is unavailable.
- **Context budget manager.** Share the model's context window between system prompt, retrieved passages, conversation history and answer. Summarise or drop old turns first.
- **Session isolation.** Clear conversation state when a user signs out on a shared device.
- **Performance guardrails.** Stop generation on thermal throttling or low battery; resume later.

#### C2 — Knowledge Engine · NEW

**Purpose.** Find the right passages in installed learning packs so answers are grounded in approved content, and return them with source references.

**Features.**
- Stores pack content as small passages (150–300 words) tagged with subject, level, topic, curriculum outcome, source and licence.
- **Hybrid search:** keyword search (SQLite FTS5) plus meaning-based search (a small on-device embedding model with a vector index).
- **Filters** by the learner's class, subject, enabled packs and policy (e.g. hide teacher-only content from learners).
- Returns the top 2–4 passages with **citations** (pack, chapter, page/section).
- **"Nothing found" signal**, so the tutor can say the topic is not in the notes instead of guessing.
- Indexing runs when a pack is installed; packs can ship with pre-built indexes to save device time.

**Depends on.** C3 (content), C5 (what the user may see).

#### C3 — Pack Manager · NEW

**Purpose.** Install, verify, update and remove School Learning Packs ([§10](#10-school-learning-packs)) and models.

**Features.**
- Install from the hub, a cloud download, a USB drive/SD card or a local file.
- **Signature check** (packs are signed by AI Studio or an approved publisher) and checksum check before install.
- **Compatibility check:** app version, storage, device tier, required model capabilities.
- **Delta updates:** apply a small patch instead of re-downloading a pack.
- **Version pinning:** the school decides when updates apply (e.g. not during exams).
- **Licence check:** a pack only unlocks if the school's licence includes it ([C8](#c8--device--licence--new)).
- Rollback to the previous version on failure.
- Storage manager showing space used by models, packs and learner data.

**Reuses.** Edge Gallery's download worker and import dialog.

#### C4 — Identity & Roles · NEW

**Purpose.** Know who is using the device and what they may do, without internet.

**Features.**
- **Local accounts** created from the school roster (synced from the hub) or by a teacher on the device.
- **Sign-in:** pick your class, pick your name, enter a 4–6 digit PIN. Optional QR card sign-in for young learners.
- **Shared-device sessions:** each user's history, progress and settings are kept separate and encrypted; auto sign-out after inactivity.
- **Personal-device mode:** one learner, stays signed in.
- **Roles and permissions** ([§4](#4-users-and-roles)), including multiple roles per person.
- PIN reset by a teacher or the IT Champion; lock-out after repeated wrong PINs.
- **Guest mode** (optional per school): tutor only, nothing saved.

#### C5 — Policy & Safety Engine · NEW

**Purpose.** Enforce the school's rules on every interaction ([§11](#11-policies-and-safety)).

**Features.**
- **Input checks:** blocked topics, personal-information detection, self-harm and abuse signals, jailbreak attempts.
- **Output checks:** age-appropriateness, blocked content, answer-leak prevention in Guide Me and assessment modes.
- **Mode rules:** which learning modes are allowed per role, class, subject and time.
- **Assessment lock:** disables hints, tutor and other features during a test.
- **Escalation:** fixed, school-approved messages that point to a trusted adult; optional confidential flag to the counsellor (per school policy).
- **Audit log** of policy events (what was blocked and why, never the full conversation unless policy says so).

#### C6 — Learner Record & Mastery · NEW

**Purpose.** Remember what each learner has done and how well they know each curriculum outcome.

**Features.**
- **Activity log:** questions practised, attempts, hints used, time spent, modes used.
- **Mastery model:** an estimate (0–100%) per curriculum outcome, updated after every practice item (a simple Bayesian knowledge-tracing or Elo-style model).
- **Spaced repetition:** schedules review of topics the learner is forgetting.
- **Weak-topic list** feeding the Study Planner and teacher insights.
- **Encrypted local storage** (SQLCipher or Android Keystore-backed encryption), per-user separation.
- **Retention rules:** delete old conversation text after N days while keeping mastery scores.
- **Export and deletion** on request.

#### C7 — Sync Agent · NEW

**Purpose.** Exchange approved data with the hub (and through it, the cloud) whenever a connection exists.

**Features.**
- Automatic discovery of the school hub on Wi-Fi (mDNS/Bonjour) or by scanning a QR code.
- **Outbox:** changes wait in a queue while offline and are sent later.
- **Downloads:** roster, assignments, packs, model updates, policy updates, licence renewals, notices.
- **Uploads (policy-controlled):** mastery summaries, assignment results, flags, device status. Never full conversations unless the school explicitly enables it.
- Conflict handling (last-writer-wins for settings; merge for progress).
- Bandwidth-aware: large files only on Wi-Fi and when charging, if configured.

#### C8 — Device & Licence · NEW

**Purpose.** Register devices, validate licences offline and keep devices manageable.

**Features.**
- **Device enrolment** to a school (QR code from the hub or an enrolment code).
- **Offline licence file** signed by AI Studio: school ID, edition, packs, device limit, expiry, **grace period** (e.g. 30 days after expiry, never during an exam window).
- **Kiosk / lock-task mode** for school-owned tablets during assessments (Android Lock Task API, or Android Enterprise device-owner mode where available).
- **Diagnostics:** app/model/pack versions, free storage, battery health, crash reports, stored locally and sent via the hub.
- **Lost device:** remote wipe on next contact with the hub; data is encrypted meanwhile.

#### C9 — Tenant Config & Branding · EXTEND

**Purpose.** Make each school's deployment look and behave like its own product, **with no code changes**.

**Exists today.** Akili branding (colours, icon, strings) is hard-coded.

**To build.** A signed **tenant configuration file** delivered with the licence:
- School name, logo, colours, welcome message, contacts, motto.
- Academic structure: curriculum, levels, classes, streams, subjects, terms, exam pathway.
- Enabled features and editions.
- Default languages and reading levels.
- Policies ([§11](#11-policies-and-safety)).

The Akili brand remains ("School Name, powered by Akili").

#### C10 — Accessibility & Language · EXTEND

**Exists today.** Audio input (Ask Audio), image input.

**To build.**
- **Text-to-speech** for every answer (Android TTS; later better offline voices).
- **Speech-to-text** questions for learners who struggle to type.
- **Reading levels:** simple / standard / advanced explanations.
- Large-text and high-contrast themes; screen-reader labels throughout.
- **Language packs:** UI translation and glossary per language (English first; Luganda, Swahili and others as model quality allows, reviewed by language specialists).
- Picture description for visually impaired learners (vision models).

#### C11 — Tool Layer · EXTEND

**Purpose.** Give the model reliable, deterministic tools so it does not have to "guess" facts or arithmetic.

**Exists today.** Function calling and JavaScript skills (`run_js`) from Edge Gallery's Agent Skills (currently disabled in Akili).

**To build.** A curated, education-focused tool set:
- **Calculator** and **equation checker** (verify a learner's step or a model's answer symbolically).
- **Unit converter**, **periodic table**, **formula sheet** lookup.
- **Answer-key lookup** for question-bank items.
- **Glossary / dictionary** from packs.
- **Diagram viewer** (show a pack diagram instead of describing it).
- **Graph plotter** for functions.

Community skills stay available only in editions/policies that allow them.

#### C12 — Quality Feedback · NEW

**Purpose.** Catch wrong or unhelpful answers and improve over time.

**Features.**
- **Flag this answer** (wrong / confusing / inappropriate), with an optional comment.
- **Teacher correction:** teachers can mark an answer wrong and write the correct explanation; it becomes a reviewed item for the pack.
- Local quality counters (flags per topic, "nothing found" rates), sent in aggregate to the Model Lab.

#### C13 — Notifications & Inbox · NEW

**Purpose.** A simple in-app inbox for assignments, notices, study reminders and update messages. Local notifications only; no cloud push dependency.

### 6.3 Learner features

| ID | Feature | Status | Summary |
|---|---|---|---|
| F1 | **My Subjects** (home) | NEW | Learner home: subjects, today's tasks, continue where you left off, streak |
| F2 | **Ask Tutor** | EXTEND (chat exists) | Curriculum-grounded tutor with modes: Teach Me, Guide Me, Test Me, Explain My Mistake |
| F3 | **Practice** | NEW | Adaptive practice from the question bank, instant feedback, spaced review |
| F4 | **Scan a Question** | EXTEND (Ask Image exists) | Photograph a textbook or exercise question, then get guided help |
| F5 | **Assignments** | NEW | Work set by the teacher, due dates, submission |
| F6 | **Study Planner** | NEW | Exam countdown, weekly plan built from weak topics |
| F7 | **My Progress** | NEW | Mastery per topic, badges, history |
| F8 | **Library** | NEW | Ask across notes and references; always shows sources |
| F9 | **Exam Prep** | NEW | Past-paper style practice for PLE, UCE and UACE, timed mock papers |
| F10 | **Guidance** | NEW | Careers and subject-choice information; wellbeing information with signposting to humans |

### 6.4 Teacher features

| ID | Feature | Status | Summary |
|---|---|---|---|
| T1 | **Teacher Home** | NEW | Classes, alerts ("12 learners stuck on fractions"), shortcuts |
| T2 | **Lesson Builder** | EXTEND (Prompt Lab) | Lesson plans from the syllabus: objectives, activities, examples, differentiation, exit quiz |
| T3 | **Exercise & Test Generator** | NEW | Build worksheets and tests from the question bank plus drafted items (teacher approves) |
| T4 | **Assign & Distribute** | NEW | Send practice, readings or tests to a class or group; schedule them |
| T5 | **Marking Assistant** | NEW | Rubric-based marking of structured and short answers; teacher approves each mark |
| T6 | **Class Insights** | NEW | Mastery heat-map by outcome, learners needing help, assignment completion |
| T7 | **Content Contribution** | NEW | Upload notes and questions; after HoD review they become part of the school pack |
| T8 | **Answer Review** | NEW | Queue of flagged answers for the teacher's subject; correct and feed back |

### 6.5 Administration features

| ID | Feature | Status | Summary |
|---|---|---|---|
| A1 | **School Assistant** | NEW | Q&A over the school's policies, calendar, handbook, fees structure (approved documents only) |
| A2 | **Notice Drafting** | EXTEND (Prompt Lab) | Draft circulars, letters and announcements in the school's tone |
| A3 | **School Dashboard** | NEW | Usage, adoption, learning trends (aggregated, no individual chats) |
| A4 | **Policy Settings** | NEW | Choose and approve safety, data and assessment policies |
| A5 | **Device & Pack Status** | NEW | Devices, versions, licence status, storage, alerts |

### 6.6 Specialised editions

The same platform, with different packs, tools and home screens:

| ID | Edition | Adds |
|---|---|---|
| E1 | **TVET** | Step-by-step procedures, safety checklists, tool identification by camera, fault-finding trees, oral revision |
| E2 | **Coding Lab** | Guided programming exercises, error explanation, offline code challenges |
| E3 | **Literacy** | Reading practice with speech, vocabulary, comprehension, pronunciation feedback |
| E4 | **University & Library** | Research questions, summarising references, citation help, departmental knowledge |

### 6.7 Akili Hub modules (per school)

**Hardware:** a low-power mini-PC (8–16 GB RAM, 512 GB–1 TB SSD) with a Wi-Fi access point and UPS or solar backup. Optionally a GPU-less box able to run a 4B-class model for the whole school.

| ID | Module | Summary |
|---|---|---|
| H1 | **Local Distribution Server** | Serves app updates, models and packs to devices over Wi-Fi; resumable downloads; staged roll-outs |
| H2 | **Roster & Enrolment** | Classes, learners, staff, roles; import from spreadsheet or school MIS; device enrolment QR codes |
| H3 | **Teacher Dashboard** | Web app served on the LAN (open from any phone browser): Class Insights, assignments, marking queue |
| H4 | **Exam Controller** | Start, pause and end locked assessments; live progress; collect and back up answers |
| H5 | **Hub Model Service** | Runs a larger model for devices on Wi-Fi; queueing and fair use per class |
| H6 | **Backup** | Nightly backup of rosters, results and school content; restore to a new device |
| H7 | **Uplink** | When internet is available: fetch updates from the cloud, send approved aggregates, receive licences |
| H8 | **Offline Update Import** | Apply update bundles from USB/SD for schools with no internet |
| H9 | **Hub Health** | Storage, temperature, power, connected devices; alerts to the IT Champion |

### 6.8 Akili Cloud modules (AI Studio Uganda)

| ID | Module | Summary |
|---|---|---|
| S1 | **Pack Studio** | Build packs: ingest documents, split into passages, tag to curriculum outcomes, draft questions, review, sign, publish ([§8.12](#812-pack-authoring-and-publishing)) |
| S2 | **Tenant & Licence Management** | Schools, contracts, editions, device counts, licence files, renewals |
| S3 | **Fleet Management** | Every hub and device version, health and alerts; staged roll-outs; update bundles for USB |
| S4 | **Model Lab** | Test models on real target devices against curriculum question sets and safety tests; publish the certified model list |
| S5 | **Curriculum Registry** | Master copy of curricula (levels, subjects, topics, outcomes); every pack maps to it |
| S6 | **Marketplace** | Publishers list licensed packs; schools buy; revenue share |
| S7 | **Analytics** | Aggregated, anonymised learning and usage trends; pilot and impact reports |
| S8 | **Support Desk** | Tickets, knowledge base, diagnostics received from hubs |
| S9 | **Configuration Builder** | Create tenant configs and policies with a form instead of editing files |

---

## 7. Features by user

This section describes what each user sees. Screens are indicative; final design follows user testing.

### 7.1 Learner

**Home (My Subjects).**
- Greeting with the learner's name and school branding.
- "Today": assignments due, planned revision, streak.
- Subject tiles (Maths, Biology, English, …) showing mastery rings.
- Big buttons: **Ask Tutor**, **Scan a Question**, **Practice**.

**Ask Tutor.** Choose subject and topic (or let Akili detect it). Choose a mode:

| Mode | Behaviour |
|---|---|
| **Teach Me** | Explains the concept at the learner's reading level, with an example from the pack and a quick check question |
| **Guide Me** | Helps solve the learner's problem step by step; gives hints; never gives the final answer until the learner has tried (hint limit set by policy) |
| **Test Me** | Asks questions on the topic from the question bank; marks each; adapts difficulty |
| **Explain My Mistake** | Learner enters their working; Akili finds the first wrong step and explains why |

Every answer shows: **source** (e.g. *S2 Maths Pack · Topic 4.2*), **Listen** (TTS), **Copy**, **Flag**, and follow-up chips **based on the answer** (e.g. "Try a similar question", "Explain step 2 again").

**Practice.** Topic or "mixed review", 5–10 questions per set, instant marking, worked solutions after each attempt, mastery update, "retry wrong ones".

**Scan a Question.** Camera, crop, confirm the recognised text (editable), then open in Guide Me. Works for printed text; handwriting is best-effort.

**Assignments.** List with due dates; open, complete, submit (stored locally, sent to the hub on next Wi-Fi contact); see teacher feedback.

**Study Planner.** Exam date(s), then a weekly plan of topics weighted by weakness and the syllabus; daily reminders.

**My Progress.** Mastery per topic, time spent, badges; "what to learn next".

**Library.** Search or ask across pack notes; answers always quote and link the passage.

**Guidance.** Subject combinations and career paths; wellbeing tips; "Talk to someone": school counsellor details and national helplines approved by the school.

### 7.2 Teacher

**Teacher Home.** Class list; alerts; shortcuts to Lesson Builder, Generator, Marking.

**Lesson Builder.** Pick class, subject, topic and lesson length; add constraints ("60 learners, no projector, mixed ability"). Output: objectives, starter, main activities, examples, differentiation for strong and struggling learners, exit quiz, homework. **Edit, save, share** (as PDF, or send to learners as a reading).

**Exercise & Test Generator.** Pick outcomes, number of items, difficulty mix and format (MCQ, short answer, structured). Items come from the question bank first; new drafts are clearly marked **"AI draft: check before use"**. Export to PDF for printing or assign digitally.

**Assign & Distribute.** Choose class/group, due date, attempts, whether hints are allowed; send via the hub.

**Marking Assistant.** For each submission: learner answer, rubric, suggested mark and reason. Teacher **accepts, edits or rejects**. Nothing is released to learners without teacher approval.

**Class Insights.** Heat-map of outcomes against learners; "who needs help"; common wrong answers; assignment completion. One tap: "create a remedial set for these learners".

**Answer Review.** Flagged answers in the teacher's subject; correct them and optionally add the correction to the school pack.

### 7.3 Head of Department

Everything a teacher has, plus: **approve** teacher-contributed content and AI-drafted items before they enter the school pack; approve department tests; see department-wide insights.

### 7.4 Administrator

School Assistant, Notice Drafting, School Dashboard, Policy Settings, Device & Pack Status, user management (with the IT Champion), licence status and renewal.

### 7.5 Counsellor

Approved guidance library; careers and subject-choice tools; referral directory; confidential flags from learners (if the school enables them).

### 7.6 IT Champion

Hub dashboard, device enrolment, roster import, update scheduling, backups, diagnostics, support ticket creation.

### 7.7 Parent / Guardian

Through the school, not directly from the learner's device: termly or monthly **learning summaries** (topics covered, strengths, areas to work on, suggested home activities). Delivered as printouts, by the school's own SMS/WhatsApp channels, or a parent view on a family phone. Akili never contacts parents on its own.

---

## 8. Workflows

Each workflow lists the actors, steps and what happens when things go wrong.

### 8.1 School onboarding

**Actors:** AI Studio deployment team, school leadership, IT Champion.

1. **Site assessment:** count and test devices (RAM, storage, Android version), check power, Wi-Fi coverage, internet availability, classes and subjects.
2. **Contract and licence:** edition, number of devices/learners, packs, term dates.
3. **Configuration** in the Configuration Builder (S9): branding, academic structure, roles, policies, enabled features.
4. **Hub installation:** mount, connect power backup, set up Wi-Fi, load models and packs (from the cloud or a USB bundle).
5. **Roster import:** classes, learners and staff from a spreadsheet or the school's MIS.
6. **Device enrolment** ([§8.2](#82-device-setup-and-enrolment)).
7. **Teacher training** (1 day) and a **2-week teacher-only period**: teachers use and review before learners get access.
8. **Learner launch:** class-by-class introduction, acceptable-use talk, PINs issued.
9. **30-day review:** usage, issues, adjustments.

### 8.2 Device setup and enrolment

1. Install Akili from the Play Store, the hub (APK over Wi-Fi) or a managed-device tool.
2. First launch: **Join a school.** Scan the enrolment QR on the hub dashboard, or enter a school code.
3. Device receives: tenant config, licence, policy, roster for its classes.
4. **Compatibility check** assigns a device tier and the right model ([§12](#12-models-and-device-tiers)).
5. Models and packs download from the hub (resumable; only on Wi-Fi; can be scheduled overnight while charging).
6. Device marked **Ready** on the hub dashboard.

**Failure cases:** not enough storage (app suggests removing packs or using a smaller model); download interrupted (resumes); device too weak (flagged "hub model only" or "not supported").

### 8.3 Learner sign-in on a shared device

1. Tap your **class**, then your **name** (or scan your QR card).
2. Enter your **PIN**.
3. Your session opens (history, progress, settings).
4. Auto sign-out after N minutes idle or on "Sign out"; conversation state is cleared from memory.

**Forgotten PIN:** teacher or IT Champion resets it on their device or the hub.

### 8.4 Asking the tutor (grounded answer)

1. Learner opens **Ask Tutor**, picks a subject and a mode, asks a question (type, speak or scan).
2. **Policy check** (C5): allowed? age-appropriate? assessment lock?
3. **Retrieval** (C2): find 2–4 relevant passages from installed packs.
4. **Prompt assembly:** mode template + passages + learner's reading level + short history, within the context budget.
5. **Generation** on-device (or on the hub model if connected and allowed).
6. **Verification** (C11): numbers and final answers checked with tools or the answer key where possible.
7. **Output check** (C5).
8. Show the answer with **source**, **Listen**, **Flag**, and contextual follow-ups.
9. **Record** (C6): topic, mode, success signals; update mastery if a check question was answered.

**If nothing relevant is found:** "This doesn't seem to be in your notes. Here's what I can say in general…" (only if policy allows), or "Ask your teacher about this."

### 8.5 Guide Me: homework help

1. Learner enters or scans the problem.
2. Akili identifies the topic and restates the problem.
3. Asks: *"What do you think the first step is?"*
4. Learner answers. Akili checks the step (tool or reasoning):
   - Correct: move on.
   - Wrong: give **Hint 1** (a nudge), then **Hint 2** (more specific), then **worked step** after the hint limit.
5. Repeat until solved.
6. Offer a **similar practice question**.
7. The final answer is only shown after the learner has attempted it, unless policy allows "show answer".

### 8.6 Scan a Question

1. Camera; learner frames the question; crop.
2. Text and diagram recognition (vision model; on-device OCR as backup).
3. Learner confirms or edits the recognised text.
4. Opens in Guide Me (default) or Teach Me.

### 8.7 Practice and mastery

1. Learner chooses a topic or "Recommended for me".
2. Akili selects items from the question bank near the learner's level.
3. Learner answers; instant marking with worked solution.
4. Mastery updated per outcome; wrongly answered items scheduled for review.
5. End-of-set summary and next suggestion.

### 8.8 Teacher: planning a lesson

1. Teacher opens **Lesson Builder**, picks class, topic and length, adds constraints.
2. Akili retrieves the syllabus outcomes and notes for the topic.
3. Generates the plan in a fixed structure.
4. Teacher edits; saves to "My lessons"; exports as PDF or shares with learners as a reading.

### 8.9 Teacher: assigning work

1. Teacher creates practice or a test (T3) or picks an existing set.
2. Sets class/group, due date, attempts, hints on or off.
3. Sends. The assignment goes to the hub, then to learners' devices at next Wi-Fi contact (or immediately if connected).
4. Learners complete offline; results return via the hub.
5. Teacher sees completion and results in Class Insights.

### 8.10 Locked assessment (in class)

1. Teacher prepares a test (T3) and HoD approves it if required.
2. On the day, the teacher opens **Exam Controller** on the hub dashboard and selects the class and test.
3. School tablets enter **lock mode** (kiosk): only the test is available; tutor, hints, camera and other apps are blocked. Personal phones are not used for locked tests.
4. Timer runs; answers autosave locally every few seconds.
5. Teacher sees live progress (started / in progress / submitted).
6. At the end, answers are collected by the hub, even if some devices lost Wi-Fi (they upload when back).
7. Objective items auto-marked; structured items go to the **Marking Assistant**.
8. Teacher reviews, approves and releases results.

**Failure cases:** device battery dies (answers are saved; learner continues on another device from the last save); hub fails (devices keep answers and upload later).

### 8.11 Marking with the Marking Assistant

1. Teacher opens the marking queue.
2. For each answer: rubric, suggested mark and a short reason.
3. Teacher accepts, adjusts or rejects; can add a comment.
4. When all are done, **Release** sends marks and feedback to learners.
5. Adjustments are logged (used to improve marking quality in the Model Lab, in aggregate).

### 8.12 Pack authoring and publishing

**Actors:** AI Studio content team or publisher; subject specialists; reviewer.

1. **Rights check:** confirm licence for every source (owner, terms, territory, expiry).
2. **Ingest** into Pack Studio: syllabus, notes, textbooks (licensed), past papers, marking guides.
3. **Structure:** split into passages; tag to curriculum outcomes from the Curriculum Registry.
4. **Question bank:** import existing questions; draft new ones with AI; every item has an answer and a worked solution.
5. **Tutor policy:** tone, hint rules and depth for the target level.
6. **Review:** a subject specialist checks every passage and question; a second reviewer samples.
7. **Automatic tests:** retrieval coverage, answer-key checks, safety scan, model trial on all device tiers.
8. **Sign and version** (semantic version, release notes).
9. **Publish** to the catalogue; schools with the right licence receive it.

### 8.13 Distributing updates (packs, models, app)

1. AI Studio publishes an update in the cloud; Fleet Management targets schools (staged: pilot schools first).
2. Hubs with internet download it; schools without internet receive a **USB/SD update bundle** (by courier or during a support visit) and the IT Champion imports it (H8).
3. The hub applies its own policy: e.g. "install packs immediately; install model updates only in holidays; never during exam windows".
4. Devices update from the hub over Wi-Fi, preferably overnight while charging.
5. Fleet Management shows adoption; problems trigger an automatic rollback.

### 8.14 School-created content

1. Teacher uploads notes or questions (T7).
2. HoD reviews and approves.
3. The hub adds them to the **school pack** (a small pack only this school uses), indexes and distributes them.
4. Optionally, with the school's permission, AI Studio considers them for national packs (with credit and licence agreement).

### 8.15 Flag and correction loop

1. Learner or teacher flags an answer.
2. The flag goes to the subject teacher's **Answer Review** queue (via the hub).
3. Teacher marks it correct or wrong and writes a correction.
4. Correction can be added to the school pack immediately (teacher/HoD approval).
5. Aggregated flags go to AI Studio for pack fixes and model evaluation.

### 8.16 Safeguarding escalation

1. Learner message contains a safeguarding signal (self-harm, abuse, danger).
2. Policy Engine stops normal generation and shows the **school-approved message**: support is available, who to talk to, helpline numbers.
3. If the school enabled it: a **confidential alert** (no chat content, only "a learner in S2B may need support", or named if the school's policy and consent allow) goes to the counsellor via the hub.
4. The counsellor follows the school's safeguarding procedure. Akili does not diagnose or counsel.

### 8.17 Licence renewal

1. Licence nears expiry; admin and hub show reminders from 60 days.
2. School renews with AI Studio; a new signed licence is issued.
3. The hub receives it online or by USB; devices get it via the hub.
4. If not renewed: a **grace period** (e.g. 30 days) with warnings; then premium packs lock. Learner records are **never** deleted or locked because of a licence.

### 8.18 Lost or stolen device

1. IT Champion marks the device lost on the hub.
2. Data on the device is already encrypted and PIN-protected.
3. If the device connects to the hub or cloud again, it is wiped.
4. The licence seat is freed for a replacement device.

### 8.19 Learner leaves the school / data deletion

1. Admin marks the learner as left.
2. The learner's data is exported (if requested) and deleted from devices at next sync and from the hub.
3. Aggregated, anonymous statistics remain.

### 8.20 Support

1. User reports a problem to the IT Champion.
2. IT Champion checks the hub dashboard and the knowledge base.
3. If unresolved, a support ticket is created from the hub with **diagnostics attached**.
4. AI Studio support responds (remote if the hub is online; phone or visit if not).

---

## 9. The answer pipeline and learning modes

### 9.1 Pipeline

```
Input (text / voice / photo)
  → C4  Who is asking? (role, class, reading level)
  → C5  Is this allowed now? (policy, mode, assessment lock)
  → C2  Find passages in installed packs (filtered by class and policy)
  → C1  Build prompt: mode template + passages + level + history (fits context)
  → C1  Generate (device model, or hub model on Wi-Fi)
  → C11 Verify: calculator / equation checker / answer key
  → C5  Output check (safety, answer-leak in Guide Me)
  → UI  Answer + source + Listen + Flag + contextual follow-ups
  → C6  Record activity, update mastery
```

### 9.2 Modes are controlled in code

Small models are poor at following long instructions (the Akili team has already seen this; see `SystemPromptUtils.kt`). Therefore **the app, not the prompt, enforces the mode:**

- **Guide Me** is a state machine: `restate → ask for step → check → hint 1 → hint 2 → worked step → next step → done`. The model is asked one small thing at each state ("Is this step correct? Reply YES or NO and one sentence why").
- **Test Me** picks items from the question bank in code; the model only phrases feedback.
- **Explain My Mistake** compares the learner's steps against the worked solution; the model explains the first difference.

This keeps quality acceptable even with 1–2B models.

### 9.3 Prompt templates

Each mode has a short template per model tier, stored in the pack's tutor policy (so content teams can tune them without an app release), e.g.:

```
[Teach Me · small models]
You are Akili, a friendly {subject} tutor for {level} learners in Uganda.
Use ONLY the notes below. Explain simply in under 120 words. End with one check question.
NOTES:
{passages}
QUESTION: {question}
```

---

## 10. School Learning Packs

A pack is the deployable unit of content. It is a signed ZIP file.

### 10.1 Structure

```
s2-maths-ncdc-2026.akpack
├── manifest.json            # identity, version, compatibility, licence, signature
├── curriculum.json          # levels, topics, outcomes, prerequisites
├── content/
│   ├── passages.jsonl       # one passage per line, tagged to outcomes
│   └── media/               # diagrams, images (compressed)
├── questions/
│   ├── items.jsonl          # question bank: stem, type, answer, solution, rubric, difficulty
│   └── papers/              # mock papers (teacher-only)
├── glossary.jsonl
├── tutor_policy.json        # mode templates, hint rules, tone, reading levels
├── safety_profile.json      # topic rules for this age group
├── index/                   # optional pre-built search indexes (FTS + vectors)
└── SIGNATURE
```

### 10.2 Manifest example

```json
{
  "packId": "ug.aistudio.pack.s2-maths",
  "title": "Senior 2 Mathematics",
  "version": "1.3.0",
  "curriculum": "NCDC Lower Secondary (Competency-Based)",
  "level": "S2",
  "subject": "Mathematics",
  "languages": ["en"],
  "publisher": "AI Studio Uganda",
  "licence": { "type": "school-annual", "sources": ["sources.json"] },
  "requires": { "appVersion": ">=2.0.0", "modelCapabilities": ["text"], "storageMb": 120 },
  "audience": { "learnerContent": true, "teacherContent": true },
  "checksum": "sha256:…",
  "releaseNotes": "Added 40 questions on simultaneous equations; fixed 3 errors in Topic 4."
}
```

### 10.3 Pack types

| Type | Made by | Example |
|---|---|---|
| National subject pack | AI Studio / partners | S2 Mathematics |
| Exam pack | AI Studio / publishers | UCE Biology past-paper practice |
| School pack | The school (T7, reviewed) | School policies, teacher notes, calendar |
| Language pack | AI Studio + language specialists | Luganda UI + glossary |
| Edition pack | AI Studio | TVET Electrical Installation |
| Publisher pack | Publisher via Marketplace | Licensed textbook companion |

---

## 11. Policies and safety

### 11.1 Policy layers

1. **AI Studio baseline** (cannot be relaxed): child safety, illegal content, self-harm handling, personal data.
2. **Edition policy** (primary / secondary / TVET / university).
3. **School policy:** chosen and approved by school leadership.
4. **Class/assignment policy:** set by the teacher (e.g. hints off for this homework).

The strictest applicable rule wins.

### 11.2 Example school policy

```json
{
  "ageBand": "13-17",
  "modes": { "teachMe": true, "guideMe": true, "testMe": true, "explainMistake": true },
  "guideMe": { "hintsBeforeAnswer": 2, "allowShowAnswer": false },
  "generalKnowledgeOutsidePacks": "limited",
  "blockedTopics": ["dating", "gambling"],
  "communitySkills": false,
  "hubModel": "allowed",
  "conversationRetentionDays": 14,
  "syncToHub": ["mastery", "assignments", "flags", "deviceStatus"],
  "safeguardingAlerts": "anonymous",
  "examWindows": [{ "from": "2026-11-02", "to": "2026-11-20" }]
}
```

### 11.3 Safety testing

Every model and pack release is tested with a fixed **red-team set** (age-inappropriate requests, self-harm phrases in English and local languages, jailbreaks, cheating attempts) on every device tier. Release is blocked if unsafe-response rate exceeds the threshold ([§17](#17-quality-and-evaluation)).

---

## 12. Models and device tiers

### 12.1 Device tiers

| Tier | Typical device | RAM | On-device model (default) | Features |
|---|---|---|---|---|
| **Low** | Entry phones, older tablets | 3–4 GB | ~1B text model (e.g. Gemma 3 1B, 0.6 GB) | Tutor (text), practice, library; images and audio via hub model when on Wi-Fi |
| **Medium** | Mid-range phones, school tablets | 6–8 GB | ~2B multimodal (e.g. Gemma 4 E2B, 2.6 GB) | All learner features incl. Scan a Question and voice |
| **High** | Newer phones, laptops | 8–12 GB+ | ~4B multimodal (e.g. Gemma 4 E4B, 3.7 GB) | All features; best quality; teacher tools on-device |
| **Hub** | School mini-PC | 16 GB+ | Best model that runs at acceptable speed | Serves low-tier devices and heavy teacher tasks on the LAN |

Model names are examples from the current Akili list; the **Model Lab** (S4) certifies the actual choice each term.

### 12.2 Selection rules

- On first run, measure RAM, free storage, Android version and a 10-second benchmark; assign a tier.
- Use each model's **`estimatedPeakMemoryInBytes`** and a realistic `minDeviceMemoryInGb` (see issues 4–5 in [§2.2](#22-issues-found-in-the-current-code)).
- Teachers' devices get the best model they can run, because teacher tools need more reasoning.
- One model per device by default, to save storage.

### 12.3 Certification criteria (Model Lab)

A model is certified for a tier only if, on real devices of that tier, it meets: accuracy on the curriculum question set, grounded-answer faithfulness, safety thresholds, first-token latency under 3 s, and at least 8 tokens/s, with no crashes over a 30-minute session and acceptable heat and battery use.

### 12.4 Licences

Check and pass on each model's licence and use policy (e.g. Gemma Terms of Use and Prohibited Use Policy; Apache-2.0 for Qwen). Keep a model-licence register in the Model Lab.

---

## 13. Data model

Core entities (simplified):

| Entity | Key fields | Stored on |
|---|---|---|
| School (Tenant) | id, name, branding, curriculum, edition, licence | Cloud, hub, device |
| User | id, name, roles[], class ids, PIN hash, reading level, language | Hub, device |
| Class | id, level, stream, subjects, teachers | Hub, device |
| Device | id, school, tier, model, app/pack versions, status | Cloud, hub |
| Pack | id, version, type, licence, checksum | Cloud, hub, device |
| Passage | id, pack, outcome ids, text, source | Device, hub |
| Outcome | id, curriculum, subject, level, topic, description, prerequisites | Cloud, device |
| Item (question) | id, pack, outcome ids, type, stem, answer, solution, rubric, difficulty | Device, hub |
| Attempt | user, item, answer, correct, hints used, time | Device → hub (summary) |
| Mastery | user, outcome, score, last practised, next review | Device → hub |
| Session | user, mode, subject, start/end, (conversation text, retention-limited) | Device only (default) |
| Assignment | id, class, items, due date, settings | Hub → device |
| Submission | assignment, user, answers, marks, feedback, status | Device → hub |
| Flag | user, message ref, reason, comment, status, correction | Device → hub → cloud (aggregate) |
| Policy | scope, rules, version, approved by | Cloud, hub, device |
| Licence | school, edition, packs, seats, expiry, grace, signature | Cloud, hub, device |

---

## 14. Technical architecture

### 14.1 Android app

Keep Google's Edge Gallery code as the **runtime foundation** and add our modules beside it, so future Google fixes can still be merged with few conflicts.

```
Android/src/
├── app/                     # existing Edge Gallery + Akili branding (shell, navigation)
├── core/
│   ├── runtime/             # C1 extensions (profiles, hub client, context budget)
│   ├── knowledge/           # C2 Knowledge Engine (FTS5 + vector index)
│   ├── packs/               # C3 Pack Manager
│   ├── identity/            # C4
│   ├── policy/              # C5
│   ├── learner/             # C6 records & mastery (Room + SQLCipher)
│   ├── sync/                # C7
│   ├── device/              # C8 licence, kiosk, diagnostics
│   ├── tenant/              # C9
│   ├── a11y/                # C10
│   ├── tools/               # C11
│   └── feedback/            # C12, C13
└── features/
    ├── learner-home/ tutor/ practice/ scan/ assignments/ planner/ progress/ library/ exam-prep/ guidance/
    ├── teacher-home/ lesson-builder/ generator/ marking/ insights/ contribution/ review/
    └── admin/ …
```

- **Language/UI:** Kotlin, Jetpack Compose, Material 3 (as today).
- **DI:** Hilt (as today). Features register via the existing `CustomTask` plug-in mechanism or a new Akili feature registry, and are switched on by tenant config.
- **Storage:** Room + SQLCipher for learner data; Proto DataStore for settings (as today); FTS5 + a small vector index for knowledge.
- **Embeddings:** a small on-device text-embedding model (via LiteRT or MediaPipe Text Embedder).
- **Models:** LiteRT-LM (as today).
- **Rename packages gradually** from `com.google.ai.edge.gallery` to `ug.aistudio.akili` for new modules; leave Google's files in place to ease merges.

### 14.2 Hub

- Linux mini-PC; services in Docker containers for easy updates.
- **API:** REST + WebSocket (Kotlin/Ktor or Python/FastAPI).
- **Database:** PostgreSQL (or SQLite for small schools).
- **Teacher dashboard:** a web app served on the LAN, usable from phone browsers.
- **Model service:** llama.cpp / LiteRT-compatible server for the hub model.
- **Discovery:** mDNS (`akili-hub.local`) and QR codes.
- **Updates:** signed bundles, applied by an updater service; USB import.

### 14.3 Cloud

- Web console (Pack Studio, Tenants, Fleet, Model Lab, Marketplace, Analytics, Support).
- Object storage for packs and models; CDN for downloads.
- Signing service with keys in a hardware-backed KMS.
- Only aggregated, approved data is received from hubs.

### 14.4 Engineering practices

- CI builds and tests **every branch and pull request** (not just `main`).
- Unit tests for core modules; UI tests for key workflows; on-device benchmark tests in the Model Lab.
- Release builds signed with an AI Studio upload key (Play App Signing); code shrinking on.
- Lint on and treated as errors for new code.
- Feature flags via tenant config.
- Keep a documented process for merging upstream Edge Gallery changes.

---

## 15. Security, privacy and compliance

### 15.1 Data protection

- Comply with **Uganda's Data Protection and Privacy Act, 2019** and its Regulations: register with the **Personal Data Protection Office (PDPO)**, appoint a data protection officer, maintain a register of processing.
- **Children's data:** obtain consent from parents/guardians through the school; collect the minimum; explain in plain language.
- **Data residency:** learner data stays on devices and the school hub by default; cloud receives only aggregated, approved data.
- **Data processing agreements** with each school (school is the controller for learner data; AI Studio is the processor where it handles any).
- **Retention and deletion** rules configured per school, with sensible defaults ([§11.2](#112-example-school-policy)).

### 15.2 Security controls

- Encryption at rest on devices (Keystore-backed) and hub (disk encryption).
- PINs stored as salted hashes; lock-out after failed attempts.
- All packs, models, configs, policies and licences **signed**; devices refuse unsigned content.
- Hub–device traffic encrypted (TLS with hub certificate pinned at enrolment).
- Role-based access everywhere; audit logs for admin actions.
- Regular security review and penetration test before commercial release.

### 15.3 Content and IP

- Rights register for every pack source; no unlicensed textbooks or past papers.
- Model licences recorded and passed on to users.
- Keep Google's Apache-2.0 notices in forked files; add AI Studio notices to new files; do not use Google trademarks in the product.

### 15.4 Education regulation

- Align packs with **NCDC** curricula; seek NCDC/MoES review or endorsement where possible.
- Be transparent with schools that Akili supports, but does not replace, teachers and national examinations.

---

## 16. Operations: how Akili is managed

### 16.1 AI Studio Uganda teams

| Team | Responsibilities |
|---|---|
| **Product** | Roadmap, requirements, user research, pilots |
| **Engineering** | App, hub, cloud, Model Lab |
| **Content** | Curriculum registry, pack authoring, reviewer network, rights |
| **Quality & Safety** | Evaluation sets, red-teaming, release sign-off, safeguarding policy |
| **Deployment** | Site assessment, hub installation, device enrolment |
| **Training** | Teacher, IT Champion and learner onboarding; materials |
| **Support** | Help desk, knowledge base, field visits |
| **Partnerships & Sales** | Schools, NGOs, government, device partners, publishers |

### 16.2 At the school

| Role | Responsibilities |
|---|---|
| Head teacher / Admin | Approves policies and consent process; owns the relationship |
| IT Champion | Hub, devices, updates, first-line support (trained and certified by AI Studio) |
| Heads of Department | Approve school content and tests |
| Teachers | Review content, assign work, approve marks, act on insights |

### 16.3 Term calendar

| When | Activity |
|---|---|
| **Holidays before term** | New packs and model updates delivered and installed; devices updated; teacher refresher training |
| **Week 1** | Rosters updated; new learners enrolled; PINs issued |
| **During term** | Weekly usage check by IT Champion; monthly quality report from AI Studio |
| **Exam windows** | Update freeze; locked assessments available |
| **End of term** | Reports to school leadership and parents; backups; feedback survey |

### 16.4 Support levels

| Level | Who | Examples | Target response |
|---|---|---|---|
| L0 | In-app help and knowledge base | "How do I reset my PIN?" | Immediate |
| L1 | School IT Champion | PIN resets, device enrolment, Wi-Fi | Same day |
| L2 | AI Studio help desk | Update failures, hub issues, content errors | 1 working day |
| L3 | AI Studio engineering | Bugs, crashes, security | Severity-based |

### 16.5 Release management

- App: monthly release train; hot-fixes as needed; staged roll-out (pilot schools, then 20%, then all).
- Packs: released per term or when corrections are needed; errata fixes within a week.
- Models: certified per term by the Model Lab; never changed during exam windows.

---

## 17. Quality and evaluation

### 17.1 Metrics

| Area | Metric | Target (initial) |
|---|---|---|
| Answer quality | Grounded-answer accuracy on the curriculum test set | ≥ 90% (medium/high tier), ≥ 80% (low tier) |
| | Source correctness (answer supported by cited passage) | ≥ 95% |
| | Hallucination rate on "not in notes" questions | ≤ 5% |
| Safety | Unsafe-response rate on the red-team set | ≤ 0.5% |
| Pedagogy | Guide Me answer-leak rate | ≤ 2% |
| Learning | Practice accuracy gain over a term; mastery growth | Measured per pilot |
| Teacher value | Preparation time saved; generated-material acceptance rate | ≥ 1 hour/week; ≥ 70% |
| Reliability | Sessions without crash; successful offline sessions | ≥ 99.5% |
| Performance | First response time; tokens/s; battery per hour | < 3 s; ≥ 8 tok/s; < 15%/h |
| Adoption | Weekly active learners/teachers; repeat use | Measured per school |
| Equity | Use and outcomes across device tiers, levels, languages | No tier left behind |

### 17.2 How quality is measured

- **Offline test sets** per subject and level (questions with answers and sources), run in the Model Lab on every tier before each release.
- **Field data:** flags, corrections and "nothing found" rates (aggregated).
- **Pilot studies** with baseline and end-of-term assessments.

---

## 18. Editions, pricing and go-to-market

### 18.1 Editions

| Edition | For | Includes |
|---|---|---|
| **Akili Learner** | Individual learners and families | App + retail subject/exam packs; no hub |
| **Akili School** | Primary and secondary schools | Branded deployment, all core modules, teacher tools, hub, national packs for chosen levels |
| **Akili TVET** | Vocational institutions | School edition + trade packs, procedures, camera tools |
| **Akili Campus** | Universities, libraries | Research, coding lab, departmental packs |
| **Akili Programme** | NGOs, government | Multi-school deployment, monitoring, localisation |

### 18.2 Revenue lines

- Annual school licence (per enrolled learner or per active device, with minimums).
- Paid packs (subject, exam, language, trade).
- Implementation fees (hub, configuration, content conversion, training).
- Support and maintenance plans.
- Device partnerships (Akili preloaded; licence bundled).
- Programme contracts (NGO/government).
- Marketplace revenue share with publishers.

Pricing is by learners/devices and packs, **not by messages**, since inference is local and costs AI Studio nothing per question.

### 18.3 Go-to-market

1. **Pilot schools** (2–3 private secondary schools + 1–2 rural government schools) to prove learning value and reliability.
2. **Private schools** for early revenue.
3. **NGO and programme partners** for scale in low-connectivity areas.
4. **Device partners** for distribution.
5. **Publishers** for content breadth.

---

## 19. Build roadmap

This is the order of construction for the complete product. Each phase ends with something usable.

| Phase | Goal | Main deliverables | Usable outcome |
|---|---|---|---|
| **0. Stabilise** | A clean base everyone can build | Fix issues in [§2.2](#22-issues-found-in-the-current-code); CI on all branches; release signing; basic tests; device test bench of target phones | Akili builds anywhere; measured model performance on real devices |
| **1. Grounded tutor** | Trustworthy answers for one subject | C2 Knowledge Engine; C3 Pack Manager (file/USB install); first pack (one level, one subject); Ask Tutor with Teach Me and Guide Me; sources shown; Flag button; C11 calculator/equation checker | A learner gets correct, sourced, guided help offline |
| **2. Learner core** | A complete learner experience | C4 identity (shared devices); C6 mastery; Practice; Test Me; Explain My Mistake; Scan a Question; My Progress; Study Planner; TTS/STT | A learner can learn, practise and track progress on a shared tablet |
| **3. School deployment** | Real school operation | Hub H1–H3, H6–H8; C7 sync; C8 enrolment and licences; C9 tenant config and branding; roster import | A school runs Akili on 50–200 devices with no internet |
| **4. Teacher tools** | Teachers save time | Lesson Builder; Generator; Assign; Class Insights; Answer Review; Content Contribution | Teachers plan, assign and see who needs help |
| **5. Assessment** | Trusted testing | Exam Controller (H4); lock mode; Marking Assistant; results release | Schools run locked tests and approve marks |
| **6. Safety & policy** (runs alongside 1–5) | Safe for children | C5 full policy engine; red-team sets; safeguarding flow; data-protection compliance | Ready for wider rollout |
| **7. Cloud operations** | Scale beyond pilots | Pack Studio; Tenant & Licence Management; Fleet; Model Lab; Support Desk; Configuration Builder | AI Studio can serve many schools efficiently |
| **8. Growth** | Broader market | More subjects/levels; admin & counsellor tools; parent summaries; hub model; editions (TVET, Literacy, Coding, Campus); Marketplace; language packs | Full product line |

---

## 20. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Wrong answers (especially maths) damage trust | High | Critical | Grounding, tools, question banks with answers, teacher review, Model Lab thresholds, visible sources |
| School devices too weak | High | High | Device tiers, small models, hub model, device partner bundles |
| Power and charging | High | High | Charging carts, solar options, low-power modes, offline autosave |
| Content rights not secured | Medium | High | Rights register, own content, publisher partnerships, NCDC engagement |
| Teacher resistance | Medium | High | Teacher-first design, training, teacher-only period, visible time savings |
| Cheating and over-reliance | Medium | Medium | Guide Me by default, locked assessment, teacher control |
| Child-safety incident | Low | Critical | Baseline policy, red-teaming, safeguarding flow, incident response plan |
| Data-protection breach | Low | High | Local data, encryption, minimisation, PDPO compliance |
| Long government sales cycles | High | Medium | Private schools and NGO programmes first |
| Larger competitors | Medium | Medium | Local moat: curriculum packs, reviewers, school relationships, hub deployments, support network |
| Fork drift from Google's code | Medium | Medium | Keep changes in separate modules; documented upstream merge process |
| Model licence changes | Low | Medium | Licence register; multiple certified model options |

---

## 21. Open decisions

1. **First level and subject for depth** (e.g. S1–S2 Mathematics under the competency-based curriculum, or P6–P7 for PLE).
2. **Primary buyer for years 1–2:** private schools, NGO programmes or device partners.
3. **Hub from day one?** Recommendation: yes, for school deployments.
4. **Target devices** for pilots (2–3 representative models) and minimum supported spec.
5. **Content strategy:** own authoring, publisher partnerships, NCDC partnership, or a mix.
6. **What may leave the device**, and consent approach for learner data.
7. **Offline licence rules:** term length, grace period, what locks on expiry.
8. **Brand architecture:** "Akili" as the product; "School name, powered by Akili" for tenants?
9. **Language roadmap:** which local language first, and quality bar.
10. **Keep or drop** the Hugging Face sign-in (AI Studio distribution through hub/cloud may replace it).

---

## 22. Immediate next steps

Engineering (on `marv_dev`):

1. Remove the hard-coded Windows `org.gradle.java.home` from `gradle.properties` (each developer sets their JDK locally).
2. Enable CI for all branches and pull requests.
3. Re-enable lint (at least for new code).
4. Replace the 4.6 MB `akili_icon.png` with an optimised asset.
5. Fix `minDeviceMemoryInGb` and add `estimatedPeakMemoryInBytes` / `llmMaxContextLength` for bundled models.
6. Point the fallback model-list URL to AI Studio's repository.
7. Set up release signing with an AI Studio key.
8. Build a small device test bench (2–3 target devices) and benchmark the bundled models.

Product and business:

1. Decide items 1, 2 and 4 in [§21](#21-open-decisions).
2. Recruit 3–5 pilot schools; interview heads, teachers and learners.
3. Start the rights process for the first pack's content.
4. Draft the data-protection approach and consent forms.
