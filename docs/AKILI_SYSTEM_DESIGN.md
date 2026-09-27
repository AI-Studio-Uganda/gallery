# Akili — System Design Document

**High-level and low-level design of the Akili offline AI platform for schools**

| | |
|---|---|
| Status | Draft v0.1, for engineering review |
| Branch | `marv_dev` (based on `akili`) |
| Companion document | [`AKILI_PRODUCT_SPEC.md`](AKILI_PRODUCT_SPEC.md): what the product does. This document describes **how it is built**. |
| Audience | Engineers, interns, technical leads, reviewers |

---

## Contents

**Part A — Foundations**
1. [Purpose and scope](#1-purpose-and-scope)
2. [Architectural drivers](#2-architectural-drivers)
3. [Key design decisions](#3-key-design-decisions)

**Part B — High-level design**

4. [System context](#4-system-context)
5. [Containers](#5-containers)
6. [App architecture: the layers](#6-app-architecture-the-layers)
7. [The plug-in model](#7-the-plug-in-model)
8. [Core directory structure](#8-core-directory-structure)
9. [Key runtime flows](#9-key-runtime-flows)
10. [Deployment views](#10-deployment-views)

**Part C — Low-level design: the app**

11. [Gradle modules and dependency rules](#11-gradle-modules-and-dependency-rules)
12. [Akili SDK: the plug-in contracts](#12-akili-sdk-the-plug-in-contracts)
13. [Kernel: bootstrap and plug-in registry](#13-kernel-bootstrap-and-plug-in-registry)
14. [Core services](#14-core-services)
15. [Tutor orchestration and learning modes](#15-tutor-orchestration-and-learning-modes)
16. [Data storage](#16-data-storage)
17. [File formats](#17-file-formats)

**Part D — Low-level design: hub, cloud and protocols**

18. [Akili Hub](#18-akili-hub)
19. [Akili Cloud](#19-akili-cloud)
20. [Sync protocol](#20-sync-protocol)
21. [Security design](#21-security-design)

**Part E — Engineering**

22. [Error handling and observability](#22-error-handling-and-observability)
23. [Performance budgets](#23-performance-budgets)
24. [Testing strategy](#24-testing-strategy)
25. [Migration from the current codebase](#25-migration-from-the-current-codebase)
26. [Glossary](#26-glossary)

---

# Part A — Foundations

## 1. Purpose and scope

This document defines the architecture of Akili: the Android app, the School Hub and the Akili Cloud. It is organised around one central idea:

> **Akili is a small, stable core with everything else plugged in.**
> Features, AI models, content, tools, policies and even storage or sync mechanisms are plug-ins behind stable contracts. A school edition, a TVET edition or a university edition is the same core with a different set of plug-ins switched on.

**In scope:** architecture, module boundaries, interfaces, data, protocols, security, performance, testing and how to migrate from today's `akili` code.

**Out of scope:** visual design, pricing, content authoring guidelines (see the product specification).

### 1.1 How to read this document

- **Part B** is for everyone: the big picture and the plug-in model.
- **Part C** is for Android engineers.
- **Part D** is for hub/cloud engineers and anyone touching sync or security.
- **Part E** is for everyone who writes or reviews code.

Code samples are **Kotlin** and define contracts. Names are proposals; the shape matters more than the exact spelling.

---

## 2. Architectural drivers

### 2.1 Quality attributes (in priority order)

| # | Attribute | What it means for Akili | How the design responds |
|---|---|---|---|
| Q1 | **Offline operation** | Every daily learning task works with no internet and no hub | All core services run on-device; hub and cloud are optional enhancers; sync uses an outbox |
| Q2 | **Correctness & trust** | Answers must be grounded and verifiable | Retrieval before generation; tool verification; mode state machines in code; sources shown |
| Q3 | **Child safety & privacy** | Safe outputs; learner data protected | Policy Engine on every input and output; encryption at rest; data minimisation; signed content |
| Q4 | **Runs on low-end hardware** | 3–4 GB RAM phones, shared tablets, weak batteries | Device tiers; model router; memory and thermal guards; lazy plug-in loading |
| Q5 | **Pluggability** | Add or remove features, models, content and tools without touching the core | Kernel + SDK contracts + registry; three plug-in kinds; configuration-driven enablement |
| Q6 | **Configurability per school** | Branding, curriculum, roles, policies without code changes | Signed tenant configuration consumed by all layers |
| Q7 | **Updatability** | Content, models, config and app update safely, offline if needed | Signed bundles; staged roll-out; delta updates; rollback |
| Q8 | **Upstream mergeability** | Keep taking fixes from Google's Edge Gallery | Akili code lives in new modules; upstream code stays in `:app` and `:upstream-*` with minimal edits |
| Q9 | **Testability** | Every module testable in isolation | Interfaces everywhere; fakes for every provider; no Android types in domain logic |

### 2.2 Constraints

- Android 12+ (minSdk 31), Kotlin, Jetpack Compose, Hilt: the existing stack.
- Model runtime is **LiteRT-LM** (existing); the design must allow other runtimes later.
- Content must be licensed; learner data regulated by Uganda's Data Protection and Privacy Act, 2019.
- A small team with interns: the design favours clear boundaries and simple technology over cleverness.

---

## 3. Key design decisions

Each decision is recorded as a short Architecture Decision Record (ADR). New significant decisions are added to `docs/adr/`.

| ADR | Decision | Why | Alternatives rejected |
|---|---|---|---|
| 001 | **Microkernel (plug-in) architecture** for the app | Editions, schools and features vary; core must stay stable | Monolithic app with feature flags only (flags alone don't isolate code or dependencies) |
| 002 | **Three plug-in kinds:** Feature, Provider, Content | Different lifecycles: features are compiled in, providers swap implementations, content arrives at runtime | One plug-in type for everything (too broad; weak typing) |
| 003 | **Compile-time discovery via Hilt multibindings** (`@IntoSet`), enablement at runtime by config/licence | Already how Edge Gallery registers `CustomTask`; type-safe; no reflection; works with R8 | Dynamic code loading (DEX at runtime): security risk, Play policy issues |
| 004 | **No executable code in content packs.** Packs carry data, templates and declarative rules only. JS skills run only in a sandboxed WebView and only when policy allows | Safety and Play policy; signed data is auditable | Allowing arbitrary scripts in packs |
| 005 | **Retrieval-augmented generation** with hybrid search (SQLite FTS5 + on-device embeddings) | Grounded answers, works offline, low memory | Fine-tuning per curriculum only (can't cite, costly to update) |
| 006 | **Learning modes as state machines in code**, not long prompts | Small models follow short, single-step instructions reliably | Prompt-only modes (unreliable on 1–2B models; already observed in `SystemPromptUtils.kt`) |
| 007 | **Model Router** chooses device or hub model per request | Low-end devices get better answers on school Wi-Fi without internet | Device-only (weak quality on low tier) or cloud-only (breaks offline) |
| 008 | **Outbox-based, idempotent sync**, device → hub → cloud | Intermittent connectivity; no lost or duplicated records | Direct device-to-cloud sync (needs internet) |
| 009 | **Everything that configures behaviour is signed** (packs, config, policy, licence, models, update bundles) | Integrity without constant internet | Trust-on-download (tampering risk, especially via USB) |
| 010 | **Room + SQLCipher** for learner data; Proto DataStore for settings | Relational queries for mastery/assignments; encryption; existing DataStore stays | Only DataStore (poor for relational data) |
| 011 | **Upstream code isolated**; Akili code in `ug.aistudio.akili.*` modules | Ease merging Google's fixes | Renaming and refactoring all upstream packages (merge conflicts forever) |
| 012 | **Hub services in containers** on Linux mini-PC | Easy install, update and rollback in the field | Android-based hub (weaker server tooling) |

---

# Part B — High-level design

## 4. System context

Who and what interacts with Akili.

```
                 ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
                 │   Learner    │     │   Teacher    │     │ Admin / HoD  │
                 └──────┬───────┘     └──────┬───────┘     │ Counsellor   │
                        │                    │             └──────┬───────┘
                        ▼                    ▼                    ▼
   ┌────────────────────────────────────────────────────────────────────────────┐
   │                               AKILI SYSTEM                                  │
   │   Akili App (devices)  ◄──── school Wi-Fi ────►  Akili Hub (per school)     │
   │                                                        ▲                    │
   │                                         internet / USB │                    │
   │                                                        ▼                    │
   │                                                  Akili Cloud                │
   └────────────────────────────────────────────────────────────────────────────┘
        ▲               ▲                 ▲                 ▲               ▲
        │               │                 │                 │               │
 ┌──────┴─────┐  ┌──────┴──────┐  ┌───────┴──────┐  ┌───────┴─────┐  ┌──────┴──────┐
 │ IT Champion│  │ AI Studio   │  │ Content      │  │ Model       │  │ App stores / │
 │ (school)   │  │ staff       │  │ partners     │  │ sources     │  │ MDM tools    │
 └────────────┘  └─────────────┘  │ (publishers) │  │ (HF, Kaggle)│  └─────────────┘
                                  └──────────────┘  └─────────────┘
```

| External system | Interaction |
|---|---|
| Model sources (Hugging Face, Kaggle, Google) | Cloud Model Lab downloads candidate models; devices never download from them directly in school deployments |
| Google Play / MDM | App distribution; device-owner/kiosk provisioning for school tablets |
| School MIS / spreadsheets | Roster import into the hub |
| School SMS/WhatsApp channels | Parent summaries are exported from the hub for the school to send; Akili does not send them itself |

---

## 5. Containers

The deployable units and their data stores.

```
┌───────────────────────────────── DEVICE (Android) ─────────────────────────────────┐
│  Akili App                                                                          │
│  ┌───────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ Feature       │ │ Core         │ │ Providers    │ │ Content       │              │
│  │ plug-ins      │ │ services     │ │ (runtime,    │ │ (packs,       │              │
│  │ (UI + logic)  │ │ (kernel)     │ │  search, …)  │ │  models)      │              │
│  └───────────────┘ └──────────────┘ └──────────────┘ └──────────────┘               │
│  Stores: akili-core.db (Room/SQLCipher) · learner-<id>.db · knowledge.db (FTS+vec)  │
│          DataStore (settings) · /files/packs · /files/models                        │
└──────────────────────────────────────▲─────────────────────────────────────────────┘
                                       │ HTTPS (pinned) + WebSocket over school LAN
┌──────────────────────────────── SCHOOL HUB (Linux) ────────────────────────────────┐
│  hub-gateway (API, auth) · hub-sync · hub-distribution · hub-dashboard (web)        │
│  hub-exam · hub-inference · hub-updater · hub-health                                │
│  Stores: PostgreSQL (or SQLite) · /srv/akili/bundles (packs, models, APKs) · backups│
└──────────────────────────────────────▲─────────────────────────────────────────────┘
                                       │ HTTPS (mTLS) when online · signed USB bundles
┌──────────────────────────────── AKILI CLOUD ───────────────────────────────────────┐
│  cloud-api · pack-studio · tenant-licence · fleet · model-lab · marketplace         │
│  analytics · support · signing-service (KMS)                                        │
│  Stores: PostgreSQL · object storage (packs, models, bundles) · analytics warehouse │
└────────────────────────────────────────────────────────────────────────────────────┘
```

**Rule of dependence:** a lower tier never *requires* a higher tier to be available. The app runs without the hub; the hub runs without the cloud.

---

## 6. App architecture: the layers

```
┌────────────────────────────────────────────────────────────────────────────┐
│ L5  SHELL          Navigation host, home composition, theming, sign-in UI    │  :app
├────────────────────────────────────────────────────────────────────────────┤
│ L4  FEATURES       Tutor · Practice · Scan · Assignments · Planner · Lesson  │  :feature-*
│     (plug-ins)     Builder · Marking · Insights · Admin · TVET · …           │
├────────────────────────────────────────────────────────────────────────────┤
│ L3  AKILI SDK      Stable contracts: plug-in API, service interfaces,        │  :sdk
│     (contracts)    domain models, events, UI contribution points             │
├────────────────────────────────────────────────────────────────────────────┤
│ L2  CORE SERVICES  Kernel · Tenant · Licence · Identity · Policy · Inference │  :core-*
│                    Knowledge · Packs · Tools · Learner · Assessment · Sync … │
├────────────────────────────────────────────────────────────────────────────┤
│ L1  PROVIDERS      LiteRT-LM runtime · Hub inference client · FTS5 search ·  │  :provider-*
│     (plug-ins)     Embedder · SQLCipher store · LAN sync transport · TTS …   │
├────────────────────────────────────────────────────────────────────────────┤
│ L0  PLATFORM       Android, LiteRT-LM, Room, WorkManager, CameraX, Keystore  │
│                    + upstream Edge Gallery code                              │  :upstream-*
└────────────────────────────────────────────────────────────────────────────┘
```

**Dependency rules** (enforced by Gradle, see [§11](#11-gradle-modules-and-dependency-rules)):

1. A layer may depend only on layers **below** it.
2. **Features depend only on the SDK**, never on core-service implementations or on other features.
3. **Core services depend on the SDK and on provider *interfaces***, never on concrete providers.
4. Only the Shell (`:app`) sees everything, to wire it together with Hilt.
5. Upstream Edge Gallery code is reached only through providers (e.g. `provider-litertlm` wraps `LlmModelHelper`).

This is what makes "plug in and plug out" real: removing a feature module or swapping a provider does not change any other module's code.

---

## 7. The plug-in model

### 7.1 Three kinds of plug-in

| Kind | What it is | Examples | How it is added | How it is switched on/off |
|---|---|---|---|---|
| **Feature plug-in** | A user-facing capability: screens, home tiles, settings, background jobs | Tutor, Practice, Lesson Builder, Exam, TVET Procedures | Gradle module included in the build (edition flavor) | Tenant config + licence + role + policy, at runtime |
| **Provider plug-in** | An implementation of a core service's SPI (service provider interface) | LiteRT-LM runtime, hub inference, FTS5 search, vector search, SQLCipher store, LAN transport, USB transport, Android TTS | Gradle module; bound via Hilt | Selected by priority and availability at runtime (e.g. hub model when reachable) |
| **Content plug-in** | Signed data that extends behaviour without code | Learning packs, model files, prompt templates, policy packs, tool definitions, language packs, JS skills (sandboxed) | Installed at runtime (hub, USB, file, cloud) | Licence + tenant config + policy; install/uninstall in Pack Manager |

### 7.2 Three moments of plugging

```
BUILD TIME                 INSTALL/CONFIG TIME              RUN TIME
(what code ships)          (what this school may use)       (what this user sees now)
───────────────────        ──────────────────────────       ─────────────────────────
Edition flavor selects     Tenant config + licence          Role + policy + device tier
feature & provider         enable/disable plug-ins;         + installed content decide
modules                    packs installed                  what is active and visible
e.g. "school" flavor       e.g. TVET features off,          e.g. Exam lock hides Tutor;
includes :feature-exam     S2 Maths pack installed          low tier hides Scan (no vision)
```

### 7.3 Plug-in lifecycle

```
          ┌──────────┐  discovered by Hilt multibinding at app start
          │ DECLARED │
          └────┬─────┘
               │ Kernel checks manifest: SDK version, dependencies, requirements
       ┌───────┴────────┐
       ▼                ▼
 ┌──────────┐     ┌────────────┐  (missing dependency, incompatible SDK,
 │ RESOLVED │     │ REJECTED   │   requirement not met → logged, never shown)
 └────┬─────┘     └────────────┘
      │ Enablement check: tenant config ∧ licence ∧ edition
  ┌───┴──────────┐
  ▼              ▼
┌─────────┐  ┌──────────┐
│ ENABLED │  │ DISABLED │  ← can change when config/licence updates
└────┬────┘  └──────────┘
     │ first use (lazy) or eager if manifest says so
     ▼
┌─────────┐   onStart(): register routes, tiles, jobs, event handlers
│ STARTED │
└────┬────┘
     │ per session: role/policy/device filters decide visibility
     ▼
┌─────────┐   onStop(): release resources (models, caches)
│ STOPPED │   on sign-out, low memory, disable, or app background
└─────────┘
```

### 7.4 What a feature plug-in can contribute

| Contribution point | Purpose | Example |
|---|---|---|
| `routes` | Navigation destinations (Compose screens) | `tutor/{subjectId}` |
| `homeTiles` | Tiles on role home screens | "Ask Tutor" on the learner home |
| `settingsEntries` | Rows in Settings | "Tutor reading level" |
| `backgroundJobs` | WorkManager jobs | Spaced-repetition reminder |
| `eventHandlers` | React to system events | On `AssignmentReceived`, show inbox item |
| `tools` | Tools the model may call | TVET "wiring diagram lookup" |
| `promptTemplates` | Default templates (overridable by packs) | Teach Me template |
| `policyRules` | Extra policy rules the feature needs | Exam feature adds "lock mode" rule |
| `syncChannels` | Data the feature syncs | Assignments channel |
| `permissions` | Android permissions it needs | Camera for Scan |

### 7.5 What stays in the core (cannot be plugged out)

Kernel, SDK, Tenant Config, Licence, Identity, Policy Engine, Event Bus, Secure Storage, Audit. Everything else is a plug-in or has a pluggable provider.

---

## 8. Core directory structure

### 8.1 Repository layout

```
gallery/                                   (repo root; name kept for now)
├── Android/src/
│   ├── settings.gradle.kts                includes all modules below
│   ├── build-logic/                       convention plugins (akili.feature, akili.core, …)
│   ├── app/                               L5 SHELL (existing module; slimmed over time)
│   │
│   ├── sdk/                               L3 AKILI SDK: contracts only, no implementations
│   │   ├── plugin/                        AkiliPlugin, PluginManifest, contributions
│   │   ├── services/                      service interfaces (InferenceService, …)
│   │   ├── model/                         domain models (User, Outcome, Item, …)
│   │   ├── events/                        AkiliEvent sealed hierarchy
│   │   └── ui/                            shared UI contracts (tile, route, theme tokens)
│   │
│   ├── core/                              L2 CORE SERVICES
│   │   ├── kernel/                        bootstrap, plug-in registry, lifecycle
│   │   ├── tenant/                        tenant config loading & validation
│   │   ├── licence/                       licence validation, entitlements
│   │   ├── identity/                      users, sessions, PINs, roles
│   │   ├── policy/                        policy engine & built-in rules
│   │   ├── inference/                     model router, context budgeter, templates
│   │   ├── knowledge/                     ingestion, indexing, retrieval
│   │   ├── packs/                         pack install/verify/update/remove
│   │   ├── tools/                         tool registry & built-in tools
│   │   ├── learner/                       records, mastery engine, scheduler
│   │   ├── assessment/                    tests, lock mode, submissions, marking
│   │   ├── sync/                          outbox, channels, sync engine
│   │   ├── device/                        enrolment, kiosk, diagnostics
│   │   ├── feedback/                      flags, corrections
│   │   ├── notify/                        inbox, local notifications
│   │   ├── a11y/                          TTS/STT, reading level, languages
│   │   ├── security/                      crypto, signature verification, keystore
│   │   ├── storage/                       Room databases, migrations
│   │   └── testing/                       fakes for every service (test-only)
│   │
│   ├── providers/                         L1 PROVIDERS (swappable implementations)
│   │   ├── litertlm/                      wraps upstream LlmModelHelper
│   │   ├── hub-inference/                 calls the hub model over LAN
│   │   ├── search-fts/                    SQLite FTS5 keyword search
│   │   ├── search-vector/                 embeddings + vector index
│   │   ├── embedder-litert/               on-device text embedding model
│   │   ├── ocr-mlkit/                     on-device OCR fallback (optional)
│   │   ├── tts-android/                   Android TextToSpeech
│   │   ├── transport-lan/                 hub HTTPS/WebSocket transport
│   │   ├── transport-usb/                 file/USB bundle transport
│   │   └── js-sandbox/                    sandboxed WebView for JS skills
│   │
│   ├── features/                          L4 FEATURE PLUG-INS
│   │   ├── learner-home/  tutor/  practice/  scan/  assignments/
│   │   ├── planner/  progress/  library/  exam-prep/  guidance/
│   │   ├── teacher-home/  lesson-builder/  generator/  marking/
│   │   ├── insights/  contribution/  answer-review/
│   │   ├── admin/  school-assistant/  notices/  device-status/
│   │   ├── exam/                          locked assessment (learner side)
│   │   └── editions/  tvet/  coding-lab/  literacy/  campus/
│   │
│   └── upstream/                          L0 Google Edge Gallery code, minimal edits
│       └── (initially remains inside app/; moved gradually, see §25)
│
├── hub/                                   School Hub (Linux, containers)
│   ├── gateway/  sync/  distribution/  dashboard/  exam/
│   ├── inference/  updater/  health/  backup/
│   └── deploy/                            docker-compose, installer, USB import tool
│
├── cloud/                                 Akili Cloud
│   ├── api/  pack-studio/  tenant-licence/  fleet/  model-lab/
│   ├── marketplace/  analytics/  support/  signing/
│   └── infra/                             IaC (Terraform), CI/CD
│
├── packs/                                 Pack sources & build tooling
│   ├── schema/                            JSON Schemas (pack, manifest, policy, config)
│   ├── tools/                             akpack CLI: validate, index, sign, diff
│   └── samples/                           sample pack for development
│
├── skills/                                Existing JS/text skills (sandboxed content)
├── model_allowlists/                      Model catalogue (moving to Model Lab output)
└── docs/
    ├── AKILI_PRODUCT_SPEC.md
    ├── AKILI_SYSTEM_DESIGN.md             (this document)
    └── adr/                               Architecture Decision Records
```

### 8.2 Package naming

| Code | Package |
|---|---|
| New Akili code | `ug.aistudio.akili.<layer>.<module>` e.g. `ug.aistudio.akili.core.policy` |
| Upstream code | stays `com.google.ai.edge.gallery.*` |
| Application ID | `ug.aistudio.learn` (existing) |

---

## 9. Key runtime flows

Sequence diagrams use `→` for calls and `⇢` for events/async.

### 9.1 App start (bootstrap)

```
Android      AkiliApp        Kernel          Tenant/Licence    PluginRegistry     Shell
  │  onCreate  │               │                    │                 │              │
  ├───────────►│ Kernel.boot() │                    │                 │              │
  │            ├──────────────►│ load & verify ────►│                 │              │
  │            │               │ config+licence     │                 │              │
  │            │               │◄───────── ok / NotEnrolled ──────────┤              │
  │            │               │ resolve plugins ─────────────────────►│              │
  │            │               │ (manifests, deps, enablement)        │              │
  │            │               │◄──────────────── active set ─────────┤              │
  │            │               │ start eager plugins; schedule jobs   │              │
  │            │               │ publish ⇢ KernelReady                │              │
  │            │◄──────────────┤                                      │              │
  │            ├──────────────────────────────────────────────────────────────────►│ show
  │            │                          NotEnrolled → Join School screen          │ sign-in
```

Target: shell visible in < 2 s on low tier; models load **lazily** on first AI use.

### 9.2 Ask Tutor (grounded answer)

```
TutorUI   TutorOrchestrator  Policy   Knowledge   Inference(Router)  Tools   Learner
  │ ask(q)       │              │          │              │             │        │
  ├─────────────►│ checkInput ─►│          │              │             │        │
  │              │◄──── allow ──┤          │              │             │        │
  │              │ search(q, scope) ──────►│              │             │        │
  │              │◄──── passages[+cites] ──┤              │             │        │
  │              │ render template (mode, level, passages, history)     │        │
  │              │ generate(prompt) ─────────────────────►│ device|hub  │        │
  │◄ ─ ─ ─ ─ ─ ─ ┤◄ ─ ─ ─ ─ ─ ─ token stream ─ ─ ─ ─ ─ ─ ┤             │        │
  │              │ verify(answer) ─────────────────────────────────────►│        │
  │              │◄───────────────────────────────── ok / corrected ────┤        │
  │              │ checkOutput ►│          │              │             │        │
  │              │◄── allow ────┤          │              │             │        │
  │◄─ final ─────┤ record(activity) ──────────────────────────────────────────►│
```

### 9.3 Pack install

```
PackUI     PackService      Security       Licence      Knowledge      EventBus
  │ install(src) │              │             │             │              │
  ├─────────────►│ stage to tmp │             │             │              │
  │              │ verify(sig, checksum) ────►│             │              │
  │              │◄──────── ok ──────────────┤             │              │
  │              │ checkEntitlement(packId) ─────────────────►             │
  │              │◄──────── entitled ─────────────────────────┤            │
  │              │ checkCompat(app, storage, model caps)                   │
  │              │ unpack → /packs/<id>/<ver>/                              │
  │              │ index(pack) ─────────────────────────────────►│          │
  │              │◄──────────── indexed ────────────────────────┤          │
  │              │ activate (atomic switch of "current" pointer)           │
  │              │ publish ⇢ PackInstalled(id, ver) ───────────────────────►│
  │◄── done ─────┤ keep previous version for rollback                      │
```

### 9.4 Sync with hub

```
SyncWorker   SyncEngine     Outbox      Transport(LAN)       Hub
    │ run()      │             │               │               │
    ├───────────►│ discover hub (mDNS / saved) ───────────────►│
    │            │ handshake (device token, versions) ────────►│
    │            │ pull(channel cursors) ─────────────────────►│
    │            │◄─────────────── changes + new cursors ──────┤
    │            │ apply changes (per channel handler)          │
    │            │ read pending ►│              │               │
    │            │◄── batch ────┤              │               │
    │            │ push(batch, idempotency keys) ─────────────►│
    │            │◄──────────────── acks ──────────────────────┤
    │            │ mark sent ──►│              │               │
    │            │ download bundles if any (resumable)          │
```

### 9.5 Locked assessment

```
Teacher(Hub dashboard)   Hub exam svc     Device (Exam plugin)   Kernel/Policy
       │ start(test,class)   │                   │                     │
       ├────────────────────►│ ⇢ ExamStart (WS) ►│                     │
       │                     │                   │ enterLock() ───────►│ policy: examLock=ON
       │                     │                   │ startLockTask()     │ hide other plugins
       │                     │◄── progress ⇢ ────┤ autosave every 5 s  │
       │◄── live status ─────┤                   │                     │
       │ end()               │ ⇢ ExamEnd ───────►│ submit; exitLock() ►│ examLock=OFF
       │                     │◄── submission ────┤ (or via outbox later)│
```

---

## 10. Deployment views

### 10.1 Standalone device (learner at home)

```
[Phone] Akili App + 1 model + installed packs  → everything offline; syncs when back at school
```

### 10.2 School deployment

```
                          ┌───────────────── School ─────────────────┐
[Cloud] ◄── internet ───► │ [Hub mini-PC + UPS] ── Wi-Fi AP ──┬── [Tablets ×N] (kiosk capable)
  or USB bundle           │                                   ├── [Teacher phones]
                          │                                   └── [Admin laptop → dashboard]
                          └──────────────────────────────────────────┘
```

### 10.3 Cloud

Managed Kubernetes or a small VM set in a nearby region; object storage + CDN for bundles; KMS for signing keys; separate environments `dev`, `staging`, `prod`.

---

# Part C — Low-level design: the app

## 11. Gradle modules and dependency rules

### 11.1 Module types (convention plugins in `build-logic/`)

| Convention plugin | Applied to | Provides |
|---|---|---|
| `akili.android.library` | all Android libraries | SDK levels, Kotlin, lint, test setup |
| `akili.sdk` | `:sdk` | No Android UI deps beyond Compose runtime; API-compatibility check |
| `akili.core` | `:core-*` | Depends on `:sdk`; Hilt; Room if needed |
| `akili.provider` | `:provider-*` | Depends on `:sdk` (+ upstream for litertlm) |
| `akili.feature` | `:feature-*` | Depends on `:sdk` only; Compose; Hilt; navigation |

### 11.2 Allowed dependencies

```
:app ──► everything (wiring only)
:feature-* ──► :sdk
:core-* ──► :sdk, :core-security, :core-storage
:provider-* ──► :sdk  (+ :app-upstream for provider-litertlm only)
:sdk ──► nothing Akili-specific
```

Enforced by a Gradle check (`akili.dependency-rules`) that fails the build if, for example, `:feature-tutor` depends on `:core-knowledge` or `:feature-practice`.

### 11.3 Editions as product flavors

```kotlin
// app/build.gradle.kts (sketch)
flavorDimensions += "edition"
productFlavors {
  create("school")  { dimension = "edition" }
  create("learner") { dimension = "edition" }
  create("tvet")    { dimension = "edition" }
}
dependencies {
  // every edition
  implementation(projects.features.tutor)
  implementation(projects.features.practice)
  // edition-specific
  "schoolImplementation"(projects.features.exam)
  "schoolImplementation"(projects.features.lessonBuilder)
  "tvetImplementation"(projects.features.editions.tvet)
}
```

A plug-in that is **not compiled in** cannot be enabled by config. A plug-in that **is** compiled in stays inactive unless config and licence enable it.

---

## 12. Akili SDK: the plug-in contracts

The SDK is the only thing features see. It changes rarely and carries a version (`AkiliSdk.VERSION`); plug-ins declare the SDK range they support.

### 12.1 Plug-in contract

```kotlin
package ug.aistudio.akili.sdk.plugin

interface AkiliPlugin {
  val manifest: PluginManifest

  /** Called once when the plug-in becomes ENABLED and is first needed. */
  suspend fun onStart(ctx: PluginContext) {}

  /** Release heavy resources. Must be idempotent. */
  suspend fun onStop() {}

  /** Static contributions; read by the Kernel after resolve. */
  fun contributions(): Contributions = Contributions()
}

data class PluginManifest(
  val id: String,                        // "akili.feature.tutor"
  val version: String,                   // semver
  val kind: PluginKind,                  // FEATURE | PROVIDER
  val sdkRange: String,                  // ">=1.0 <2.0"
  val displayName: Int,                  // string resource
  val requires: Requirements = Requirements(),
  val dependsOn: List<String> = emptyList(),   // other plug-in ids
  val roles: Set<Role> = Role.ALL,             // who can ever see it
  val licenceFeature: String? = null,          // entitlement key, e.g. "exam"
  val startMode: StartMode = StartMode.LAZY,
)

data class Requirements(
  val capabilities: Set<ModelCapability> = emptySet(), // TEXT, VISION, AUDIO, TOOLS
  val minTier: DeviceTier = DeviceTier.LOW,
  val androidPermissions: Set<String> = emptySet(),
  val needsHub: Boolean = false,
)

data class Contributions(
  val routes: List<RouteContribution> = emptyList(),
  val homeTiles: List<HomeTile> = emptyList(),
  val settings: List<SettingsEntry> = emptyList(),
  val jobs: List<JobContribution> = emptyList(),
  val eventHandlers: List<EventHandler<*>> = emptyList(),
  val tools: List<ToolDefinition> = emptyList(),
  val promptTemplates: List<PromptTemplate> = emptyList(),
  val policyRules: List<PolicyRule> = emptyList(),
  val syncChannels: List<SyncChannel<*>> = emptyList(),
)
```

### 12.2 UI contribution points

```kotlin
data class RouteContribution(
  val route: String,                                 // "tutor/{subjectId}?mode={mode}"
  val content: @Composable (args: RouteArgs, nav: AkiliNavigator) -> Unit,
)

data class HomeTile(
  val id: String,
  val roles: Set<Role>,
  val title: Int, val icon: ImageVector,
  val order: Int,                                    // lower = earlier
  val targetRoute: String,
  val badge: (suspend (Session) -> String?)? = null, // e.g. "3 due"
)
```

The Shell builds the navigation graph and home screens **only** from contributions of active plug-ins. Nothing in the Shell names a specific feature.

### 12.3 Plug-in context (the services a plug-in may use)

```kotlin
interface PluginContext {
  val session: StateFlow<Session?>       // current user, roles, class, tier
  val tenant: TenantConfig               // read-only
  val policy: PolicyService
  val inference: InferenceService
  val knowledge: KnowledgeService
  val tools: ToolService
  val learner: LearnerService
  val assessment: AssessmentService
  val packs: PackQueryService            // read-only view of installed content
  val events: EventBus
  val notify: NotificationService
  val a11y: AccessibilityService
  val feedback: FeedbackService
  val storage: PluginStorage             // private, encrypted key-value + files per plug-in
  val log: AkiliLogger
}
```

Features receive the context, not concrete classes, so they are testable with fakes from `:core-testing`.

### 12.4 Registration (Hilt multibinding)

This is the same mechanism Edge Gallery already uses for `CustomTask` (`@IntoSet`):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object TutorPluginModule {
  @Provides @IntoSet
  fun provideTutorPlugin(): AkiliPlugin = TutorPlugin()
}
```

### 12.5 Provider SPIs

Each core service that can have alternative implementations defines a provider interface in the SDK. Providers register with `@IntoSet` and declare a priority and availability.

```kotlin
interface Provider {
  val id: String
  val priority: Int                          // higher wins when several are available
  suspend fun isAvailable(): Boolean
}

interface InferenceProvider : Provider {
  val capabilities: Set<ModelCapability>
  suspend fun load(model: ModelRef, opts: LoadOptions): LoadedModel
  fun generate(model: LoadedModel, req: GenerationRequest): Flow<GenerationChunk>
  suspend fun cancel(model: LoadedModel)
  suspend fun unload(model: LoadedModel)
}

interface SearchProvider : Provider {
  suspend fun index(docs: List<IndexDoc>)
  suspend fun remove(packId: String)
  suspend fun search(q: SearchQuery): List<ScoredHit>
}

interface EmbeddingProvider : Provider {
  val dimensions: Int
  suspend fun embed(texts: List<String>): List<FloatArray>
}

interface SyncTransport : Provider {
  suspend fun connect(target: SyncTarget): SyncConnection
}

interface SpeechProvider : Provider {
  suspend fun speak(text: String, lang: String)
  fun transcribe(audio: Flow<ByteArray>, lang: String): Flow<String>
}

interface OcrProvider : Provider {
  suspend fun recognise(image: Bitmap): OcrResult
}
```

### 12.6 Events

```kotlin
sealed interface AkiliEvent {
  data class SessionStarted(val userId: String) : AkiliEvent
  data class SessionEnded(val userId: String) : AkiliEvent
  data class PackInstalled(val packId: String, val version: String) : AkiliEvent
  data class PackRemoved(val packId: String) : AkiliEvent
  data class PolicyChanged(val version: Int) : AkiliEvent
  data class LicenceChanged(val state: LicenceState) : AkiliEvent
  data class ExamLockChanged(val locked: Boolean, val examId: String?) : AkiliEvent
  data class AssignmentReceived(val assignmentId: String) : AkiliEvent
  data class AttemptRecorded(val userId: String, val itemId: String, val correct: Boolean) : AkiliEvent
  data class HubConnectivityChanged(val reachable: Boolean) : AkiliEvent
  data class LowMemory(val level: Int) : AkiliEvent
}

interface EventBus {
  suspend fun publish(event: AkiliEvent)
  fun <T : AkiliEvent> subscribe(type: KClass<T>): Flow<T>
}
```

Events are in-process only (a `SharedFlow`). Durable cross-device messages go through Sync, not the event bus.

### 12.7 SDK versioning rules

- **Additive changes** (new optional fields, new interfaces) are minor versions.
- **Breaking changes** need a major version, a migration note and an ADR.
- A plug-in whose `sdkRange` excludes the running SDK is REJECTED at resolve time.

---

## 13. Kernel: bootstrap and plug-in registry

### 13.1 Components

| Component | Responsibility |
|---|---|
| `Kernel` | Orchestrates boot, holds lifecycle state, exposes `KernelState` flow |
| `PluginRegistry` | Holds all declared plug-ins; resolves, enables, starts, stops |
| `ProviderRegistry<T>` | For each SPI type, picks the best available provider |
| `ContributionIndex` | Merged routes, tiles, tools, rules, channels from active plug-ins |
| `CapabilityResolver` | Device tier, model capabilities, permissions granted |
| `EnablementEvaluator` | Tenant config ∧ licence ∧ edition ∧ requirements |

### 13.2 Boot sequence

```kotlin
suspend fun boot() {
  state.value = Booting
  security.init()                                  // keystore, crypto
  storage.openCoreDb()                             // akili-core.db
  val tenant = tenantService.loadVerified()        // null → NotEnrolled
  val licence = licenceService.loadVerified(tenant)
  capability.detect()                              // tier, RAM, storage, permissions
  registry.resolve(sdk = AkiliSdk.VERSION)         // manifests, deps (topological sort)
  registry.evaluateEnablement(tenant, licence, capability)
  registry.startEager()
  contributions.rebuild(registry.active())
  jobs.schedule(contributions.jobs)
  events.publish(KernelReady)
  state.value = if (tenant == null) NotEnrolled else Ready
}
```

### 13.3 Resolution algorithm

1. Collect all `AkiliPlugin` from the Hilt set.
2. Reject duplicates (same id) → build error in debug, reject lower version in release.
3. Reject plug-ins whose `sdkRange` excludes the SDK.
4. Build a dependency graph from `dependsOn`; reject cycles and missing dependencies (and transitively their dependents).
5. Topologically sort; this is the start order.

### 13.4 Visibility per session

On every `SessionStarted`, `PolicyChanged`, `ExamLockChanged` or `LicenceChanged`, the `ContributionIndex` recomputes **visible** contributions:

```
visible(plugin) = active(plugin)
               ∧ session.roles ∩ manifest.roles ≠ ∅
               ∧ policy.allowsPlugin(plugin.id, session)
               ∧ capability.meets(manifest.requires)
               ∧ ¬(examLock ∧ plugin.id ≠ "akili.feature.exam")
```

The Shell observes `visibleContributions` and redraws navigation and home.

### 13.5 Memory management

- Plug-ins start lazily by default.
- On `LowMemory` or app background, the Kernel calls `onStop()` on non-visible plug-ins and asks `InferenceService` to unload idle models (keep at most one model loaded on LOW/MEDIUM tiers).

---

## 14. Core services

Each service has: an interface in `:sdk`, an implementation in `:core-*`, a fake in `:core-testing`. Only the essentials of each interface are shown.

### 14.1 Tenant Config Service (`core/tenant`)

```kotlin
interface TenantService {
  val config: StateFlow<TenantConfig?>
  suspend fun enrol(bundle: EnrolmentBundle): Result<TenantConfig>  // from hub QR / file
  suspend fun apply(update: SignedDocument): Result<Unit>
}
```

- Reads `tenant.json` (signed, see [§17.3](#173-tenant-configuration)), verifies the signature and schema, then caches it in `akili-core.db`.
- Exposes branding tokens to the Shell's theme (colours, logo, names).
- Unknown fields are ignored (forward compatibility); missing required fields reject the update.

### 14.2 Licence Service (`core/licence`)

```kotlin
interface LicenceService {
  val state: StateFlow<LicenceState>     // Valid, Grace(daysLeft), Expired, Missing
  fun isEntitled(feature: String): Boolean
  fun isPackEntitled(packId: String): Boolean
  suspend fun apply(licence: SignedDocument): Result<Unit>
}
```

- Offline validation: signature, school id match, device seat (device id in list or seat count), expiry + grace.
- **Trusted time:** uses the maximum of device clock, last hub time and last licence issue time, to resist clock rollback.
- **Never blocks** access to a learner's own records; on `Expired`, only premium plug-ins and packs are disabled.
- Exam windows (from tenant policy) postpone expiry effects.

### 14.3 Identity Service (`core/identity`)

```kotlin
interface IdentityService {
  val session: StateFlow<Session?>
  suspend fun listClasses(): List<ClassRef>
  suspend fun listLearners(classId: String): List<UserRef>
  suspend fun signIn(userId: String, pin: CharArray): SignInResult
  suspend fun signInWithQr(token: String): SignInResult
  suspend fun signOut()
  suspend fun resetPin(targetUserId: String, newPin: CharArray): Result<Unit> // teacher/IT only
}

data class Session(
  val user: UserRef, val roles: Set<Role>, val classIds: Set<String>,
  val readingLevel: ReadingLevel, val language: String,
  val tier: DeviceTier, val mode: DeviceMode,     // SHARED | PERSONAL
  val startedAt: Instant,
)
```

- PIN hash: Argon2id (or PBKDF2-HMAC-SHA256, 210k iterations) with a per-user salt; 5 failures → 5-minute lock, escalating.
- Shared mode: idle timeout (tenant-configurable, default 10 minutes) → `signOut()`; clears in-memory conversation state and closes `learner-<id>.db`.
- Each user's learner database is encrypted with a key derived from a device key (Keystore) + user id. The PIN gates access in the UI; data at rest is protected by the Keystore even without the PIN.

### 14.4 Policy Engine (`core/policy`)

**Design:** a pipeline of small, ordered rules. Each rule sees a request and returns a decision. The strictest decision wins.

```kotlin
interface PolicyService {
  suspend fun checkInput(req: PolicyInput): PolicyDecision
  suspend fun checkOutput(req: PolicyOutput): PolicyDecision
  fun allowsPlugin(pluginId: String, session: Session): Boolean
  fun modeRules(session: Session, subjectId: String): ModeRules
  val examLock: StateFlow<ExamLock?>
}

interface PolicyRule {
  val id: String
  val phase: Phase            // INPUT, OUTPUT, BOTH
  val order: Int              // lower runs first
  suspend fun evaluate(ctx: PolicyContext): PolicyDecision
}

sealed interface PolicyDecision {
  data object Allow : PolicyDecision
  data class Modify(val replacement: String, val reason: String) : PolicyDecision
  data class Block(val messageKey: String, val reason: String) : PolicyDecision
  data class Escalate(val kind: EscalationKind, val messageKey: String) : PolicyDecision
}
```

**Built-in rules (in order):**

| Order | Rule | Phase | Action |
|---|---|---|---|
| 10 | `ExamLockRule` | INPUT | Block tutor/chat during exam lock |
| 20 | `SafeguardingRule` | INPUT | Keyword + small classifier for self-harm/abuse signals → Escalate |
| 30 | `PersonalDataRule` | BOTH | Detect phone numbers, addresses; mask or warn |
| 40 | `BlockedTopicsRule` | BOTH | Tenant/edition topic lists → Block with school message |
| 50 | `JailbreakRule` | INPUT | Known patterns ("ignore previous instructions") → Block |
| 60 | `AgeBandRule` | OUTPUT | Age-inappropriate content → Block/Modify |
| 70 | `AnswerLeakRule` | OUTPUT | In Guide Me, final answer before hint limit → Modify (strip) |
| 80 | `ScopeRule` | INPUT | Outside installed packs and general knowledge = off → Block with "ask your teacher" |

Rules come from three places: built-in (core), plug-in contributions, and declarative rules in policy documents (keyword lists, thresholds). **Declarative rules only**: policy documents never contain code.

**Escalation:** `Escalate` shows a fixed, school-approved message and, if tenant policy enables it, enqueues a `SafeguardingAlert` in the Sync outbox (anonymous or named per policy). No conversation text is included unless policy explicitly allows.

### 14.5 Inference Service (`core/inference`)

```kotlin
interface InferenceService {
  suspend fun ensureReady(caps: Set<ModelCapability>): ModelHandle
  fun generate(req: InferenceRequest): Flow<GenerationChunk>
  suspend fun cancel(requestId: String)
  val status: StateFlow<InferenceStatus>   // Idle, Loading(progress), Generating, Unavailable(reason)
}

data class InferenceRequest(
  val id: String,
  val template: PromptTemplateRef,          // e.g. "tutor.teach_me"
  val variables: Map<String, String>,       // subject, level, passages, question, …
  val history: List<Turn> = emptyList(),
  val images: List<Bitmap> = emptyList(),
  val audio: List<ByteArray> = emptyList(),
  val tools: List<ToolRef> = emptyList(),
  val constraints: GenConstraints = GenConstraints(), // maxTokens, temperature, stop, jsonSchema
  val routing: RoutingHint = RoutingHint.AUTO,        // AUTO, DEVICE_ONLY, HUB_PREFERRED
)
```

**Internal components:**

| Component | Responsibility |
|---|---|
| `ModelRouter` | Chooses provider: hub model if reachable, allowed by policy and better for this request; else device model. Falls back automatically on timeout or error |
| `ModelCatalogue` | Certified models per tier (from Model Lab / pack manifests); replaces the current bundled `model_allowlist_full.json` |
| `ModelLifecycle` | Load/unload; one model resident on LOW/MEDIUM; unload after idle timeout (default 5 min) |
| `TemplateEngine` | Renders templates: pack override → plug-in default → core default; per model tier variant |
| `ContextBudgeter` | Fits system prompt + passages + history + reserved answer tokens into the model's context |
| `OutputParser` | Extracts structured outputs (JSON, YES/NO, tool calls); retries once with a stricter template on parse failure |
| `ThermalGuard` | Pauses or shortens generation when thermal status ≥ SEVERE or battery < 10% |

**Context budget algorithm:**

```
budget   = model.contextLength − reserve(maxTokens)
fixed    = tokens(systemTemplate) + tokens(question)
remain   = budget − fixed
passages = take top-k passages while tokens ≤ 0.6 × remain
history  = take newest turns while tokens ≤ remain − tokens(passages)
if history was truncated: prepend a 1-sentence summary (cached per session)
```

Token counts use the runtime's tokenizer when available, otherwise an estimate of 1 token ≈ 4 characters with a 15% safety margin.

**Routing rule:**

```
if routing == DEVICE_ONLY → device
if !policy.hubModelAllowed or !hub.reachable → device
if request.needs(VISION|AUDIO) and !device.supports(that) → hub
if device.tier == LOW → hub
if request.template.heavy (lesson plans, marking) → hub
else → device
on hub error/timeout (first token > 4 s) → device fallback
```

### 14.6 Knowledge Service (`core/knowledge`)

```kotlin
interface KnowledgeService {
  suspend fun search(q: KnowledgeQuery): KnowledgeResult
  suspend fun passage(id: String): Passage?
  suspend fun indexPack(pack: InstalledPack, onProgress: (Float) -> Unit)
  suspend fun removePack(packId: String)
}

data class KnowledgeQuery(
  val text: String,
  val scope: Scope,                 // subjects, levels, outcome ids, packIds
  val audience: Audience,           // LEARNER | TEACHER
  val k: Int = 4,
)

data class KnowledgeResult(val hits: List<Hit>, val confidence: Confidence) // HIGH, LOW, NONE
data class Hit(val passage: Passage, val score: Float, val citation: Citation)
```

**Retrieval pipeline:**

1. **Normalise** query (lowercase, strip punctuation, expand glossary synonyms from packs).
2. **Keyword search** (`SearchProvider` FTS5, BM25) → top 20.
3. **Vector search** (`SearchProvider` vector, cosine) → top 20, if an embedder is available on this tier.
4. **Fuse** with Reciprocal Rank Fusion: `score = Σ 1 / (60 + rank_i)`.
5. **Filter** by scope and audience (teacher-only content never reaches learners).
6. **Rerank** lightly: boost passages whose outcome matches the current topic; penalise very short passages.
7. **Confidence:** HIGH if top fused score ≥ threshold T1 and keyword overlap ≥ 2 terms; LOW if ≥ T2; else NONE (thresholds tuned per pack by the Model Lab).
8. Return top k with citations.

**Vector index:** for pack sizes on phones (≤ 50k passages), a flat or HNSW index stored in `knowledge.db` as BLOBs, with int8-quantised embeddings (384 dims ≈ 384 bytes per passage). Pre-built indexes shipped in packs are used when the embedder version matches; otherwise re-indexed in a background job while charging.

### 14.7 Pack Service (`core/packs`)

```kotlin
interface PackService {
  val installed: StateFlow<List<InstalledPack>>
  suspend fun install(source: PackSource): Flow<InstallProgress>  // File, Uri, Hub, Usb
  suspend fun update(packId: String, delta: PackSource): Flow<InstallProgress>
  suspend fun remove(packId: String): Result<Unit>
  suspend fun rollback(packId: String): Result<Unit>
}
```

**Install state machine:**

```
STAGED → VERIFIED → ENTITLED → COMPATIBLE → UNPACKED → INDEXED → ACTIVE
   │         │          │           │           │          │
   └─────────┴──────────┴───────────┴───────────┴──────────┴──► FAILED (cleanup, keep previous ACTIVE)
```

- Directory layout: `files/packs/<packId>/<version>/…` and `files/packs/<packId>/current` (pointer in DB, switched atomically in one transaction).
- Keep **one previous version** for rollback; older versions are deleted.
- **Delta updates:** `.akdelta` contains added/changed/removed files + target checksum; applied into a new version directory, then verified.
- Models are handled by the same service with type `MODEL` (large files: resumable download via WorkManager, reused from upstream `DownloadWorker`).

### 14.8 Tool Service (`core/tools`)

```kotlin
interface Tool {
  val definition: ToolDefinition   // name, description, JSON schema of args, policy tags
  suspend fun invoke(args: JsonObject, ctx: ToolContext): ToolResult
}

interface ToolService {
  fun available(session: Session, request: ToolScope): List<ToolDefinition>
  suspend fun call(name: String, args: JsonObject): ToolResult
  suspend fun verify(answer: VerifiableAnswer): Verification   // used by orchestrators
}
```

**Built-in tools:** `calculator` (exact rational arithmetic), `equation_check` (substitute and compare; symbolic simplification for polynomials), `unit_convert`, `periodic_table`, `formula_lookup`, `answer_key` (item id → answer; teacher/assessment scope only), `glossary`, `show_diagram`, `plot_function`.

**Two uses:**
1. **Model-called** (function calling via LiteRT-LM tools, as upstream supports).
2. **Orchestrator-called** (deterministic verification after generation; does not rely on the model choosing to call a tool). Small models are unreliable at tool selection, so verification is always orchestrator-driven.

JS skills (existing `skills/` format) are exposed as tools through `provider-js-sandbox` only if policy `communitySkills = true`.

### 14.9 Learner Service and Mastery Engine (`core/learner`)

```kotlin
interface LearnerService {
  suspend fun recordAttempt(a: Attempt)
  suspend fun recordActivity(a: Activity)
  fun mastery(userId: String, scope: Scope): Flow<List<OutcomeMastery>>
  suspend fun nextItems(userId: String, scope: Scope, n: Int): List<ItemRef>   // adaptive
  suspend fun dueReviews(userId: String, on: LocalDate): List<OutcomeRef>
  suspend fun exportUser(userId: String): File
  suspend fun deleteUser(userId: String)
}
```

**Mastery model: Bayesian Knowledge Tracing (BKT) per outcome.**

Parameters per outcome (defaults, tunable per pack): `P(L0)=0.2` (prior knowledge), `P(T)=0.15` (learn per opportunity), `P(G)=0.2` (guess), `P(S)=0.1` (slip).

After an attempt with result `correct`:

```
if correct:  P(L|obs) = P(L)(1−S) / [P(L)(1−S) + (1−P(L))G]
else:        P(L|obs) = P(L)S     / [P(L)S     + (1−P(L))(1−G)]
P(L)_next = P(L|obs) + (1 − P(L|obs)) × T
```

Hints used reduce the evidence: an answer correct after 2 hints counts as 0.5 correct (weighted update). Mastery ≥ 0.95 → "mastered".

**Item selection (adaptive practice):** choose items whose outcome mastery is lowest among due outcomes; within an outcome, pick difficulty closest to `mastery × maxDifficulty`; avoid items seen in the last 3 days.

**Spaced repetition:** for mastered outcomes, schedule review with intervals 1, 3, 7, 14, 30 days (SM-2-lite); a failed review resets to 1 day and lowers mastery by one BKT "incorrect" update.

### 14.10 Assessment Service (`core/assessment`)

```kotlin
interface AssessmentService {
  suspend fun startTest(testId: String, mode: TestMode): TestSession   // PRACTICE | ASSIGNMENT | LOCKED
  suspend fun saveAnswer(sessionId: String, itemId: String, answer: AnswerPayload)
  suspend fun submit(sessionId: String): Submission
  fun lockState(): StateFlow<ExamLock?>
  suspend fun autoMark(sub: Submission): List<MarkSuggestion>          // objective + rubric suggestions
}
```

- Autosave every 5 s and on every answer change to `learner-<id>.db`; submissions go to the Sync outbox.
- **Lock mode:** on `ExamStart` from the hub (or a signed exam token scanned from the teacher), sets `examLock` in the Policy Service, calls `startLockTask()` if the app is device owner or lock-task whitelisted, and hides all other plug-ins (see [§13.4](#134-visibility-per-session)).
- **Auto-marking:** MCQ and numeric by answer key and tolerance; short answers by keyword rubric + model similarity; structured answers by rubric criteria, each producing a **suggestion** with a reason. Marks are never final until a teacher approves (hub side).

### 14.11 Sync Service (`core/sync`)

See [§20](#20-sync-protocol) for the protocol. Client-side components:

| Component | Responsibility |
|---|---|
| `Outbox` | Durable queue in `akili-core.db`; entries = (id, channel, payload, idempotencyKey, createdAt, attempts) |
| `ChannelRegistry` | Registered `SyncChannel`s from core and plug-ins (roster, assignments, submissions, mastery, flags, alerts, policy, licence, packs, device status) |
| `SyncEngine` | Discover target, handshake, pull then push, per-channel cursors, retries with exponential backoff |
| `SyncWorker` | WorkManager periodic job (15 min when on Wi-Fi) + triggered on hub discovery |

```kotlin
interface SyncChannel<T> {
  val name: String                    // "submissions"
  val direction: Direction            // UP, DOWN, BOTH
  val serializer: KSerializer<T>
  suspend fun collectUp(since: Cursor?): List<T>          // for UP
  suspend fun applyDown(items: List<T>): Cursor           // for DOWN
  fun allowedToLeaveDevice(policy: SyncPolicy): Boolean   // tenant sync policy gate
}
```

### 14.12 Device Service (`core/device`)

- Enrolment (QR from hub: hub URL, school id, one-time enrolment token, hub certificate fingerprint).
- Device identity: key pair generated in Android Keystore; public key registered with hub.
- Diagnostics snapshot: versions, storage, RAM, battery health, thermal events, crash counts.
- Kiosk: device-owner provisioning via QR (Android Enterprise) for school tablets; lock-task allowlist for the exam.
- Remote wipe: on `wipe` command from hub, delete learner DBs and keys, then factory-reset if device owner.

### 14.13 Feedback, Notification, Accessibility services

- **Feedback:** `flag(messageRef, reason, comment)`; stores the question, answer, citations and model id (**not** the full conversation) and enqueues to the `flags` channel.
- **Notification:** local inbox table + Android notifications; sources: assignments, reminders, notices, update messages.
- **Accessibility:** `speak(text)`, `transcribe()`, reading-level presets that feed template variables, font scale and contrast tokens for the Shell theme.

---

## 15. Tutor orchestration and learning modes

### 15.1 Orchestrator structure

```kotlin
abstract class ModeOrchestrator(protected val ctx: PluginContext) {
  abstract val mode: LearningMode
  abstract suspend fun start(input: TutorInput): TutorState
  abstract suspend fun onUserTurn(state: TutorState, turn: UserTurn): TutorState
}
```

Each mode is a finite-state machine persisted in the session (so a learner can leave and come back). The model is asked **one small, well-defined thing per state**.

### 15.2 Guide Me state machine

```
          ┌────────────┐
 input ──►│ UNDERSTAND │  retrieve passages; restate problem (model: "Restate in one sentence")
          └─────┬──────┘
                ▼
          ┌────────────┐
          │ PLAN       │  get worked solution: from answer key if item known; else model
          └─────┬──────┘  generates steps (JSON) and tools verify final answer
                ▼
          ┌────────────┐
   ┌─────►│ ASK_STEP   │  "What do you think step {n} is?"
   │      └─────┬──────┘
   │            ▼  learner answers
   │      ┌────────────┐   tools check (numeric/equation) or model judge:
   │      │ CHECK_STEP │   "Is this step equivalent to: {expected}? Reply YES/NO + reason"
   │      └──┬───────┬─┘
   │   correct│       │wrong
   │         ▼       ▼
   │   ┌────────┐ ┌────────────┐ hints < limit → HINT (model: "Give a hint for step {n}
   │   │ NEXT   │ │ HINT       │ without revealing {expected}"; AnswerLeakRule checks)
   │   └───┬────┘ └─────┬──────┘ hints == limit → SHOW_STEP (worked step)
   │       │            │
   └───────┴────────────┘
           │ all steps done
           ▼
     ┌───────────┐
     │ WRAP_UP   │  summary + "Try a similar question?" → Practice item
     └───────────┘
```

### 15.3 Other modes

| Mode | States | Model's job | Code's job |
|---|---|---|---|
| **Teach Me** | RETRIEVE → EXPLAIN → CHECK_QUESTION → FEEDBACK | Explain from passages; phrase feedback | Pick check question from bank; mark it; update mastery |
| **Test Me** | SELECT → ASK → MARK → FEEDBACK → (loop) → SUMMARY | Phrase feedback and hints | Select items (adaptive); mark via answer key/tools |
| **Explain My Mistake** | PARSE_WORK → ALIGN → FIRST_ERROR → EXPLAIN | Parse learner's steps (JSON); explain the error | Align with worked solution; verify with tools |
| **Free Ask** (optional per policy) | RETRIEVE → ANSWER | Answer from passages | Scope/confidence handling |

### 15.4 Prompt templates

Stored as data (plug-in defaults, overridable by pack `tutor_policy.json`). One template per `(templateId, tier)`:

```json
{
  "id": "tutor.guide_me.hint",
  "tier": "LOW",
  "maxTokens": 80,
  "temperature": 0.3,
  "text": "You are Akili, a patient {subject} tutor for {level} learners.\nThe learner is on this step: {step_context}\nGive ONE short hint (max 2 sentences). Do NOT give the answer {expected}."
}
```

---

## 16. Data storage

### 16.1 Stores on the device

| Store | Tech | Encrypted | Contents |
|---|---|---|---|
| `akili-core.db` | Room + SQLCipher | Yes (Keystore key) | Tenant, licence, roster, users, classes, sessions, outbox, cursors, installed packs, inbox, audit, flags |
| `learner-<userId>.db` | Room + SQLCipher | Yes (per-user key) | Attempts, activities, mastery, reviews, test sessions, conversations (retention-limited), plug-in private data |
| `knowledge.db` | SQLite (FTS5) + BLOB vectors | Optional (content is licensed, not personal) | Passages, FTS index, embeddings, outcome map |
| DataStore | Proto | App-private | Device-level settings (existing upstream `settings.proto`) |
| Files | App-private storage | Packs signed; models checksummed | `files/packs/…`, `files/models/…`, media |

### 16.2 Core schema (`akili-core.db`, abridged)

```sql
CREATE TABLE tenant   (id TEXT PRIMARY KEY, doc_json TEXT NOT NULL, version INTEGER, sig_ok INTEGER);
CREATE TABLE licence  (id TEXT PRIMARY KEY, doc_json TEXT NOT NULL, expires_at INTEGER, grace_days INTEGER);
CREATE TABLE users    (id TEXT PRIMARY KEY, display_name TEXT, roles TEXT, pin_hash BLOB, pin_salt BLOB,
                       reading_level TEXT, language TEXT, status TEXT, updated_at INTEGER);
CREATE TABLE classes  (id TEXT PRIMARY KEY, level TEXT, stream TEXT, name TEXT);
CREATE TABLE class_members (class_id TEXT, user_id TEXT, role TEXT, PRIMARY KEY(class_id, user_id));
CREATE TABLE packs    (pack_id TEXT, version TEXT, type TEXT, status TEXT, path TEXT, is_current INTEGER,
                       installed_at INTEGER, PRIMARY KEY(pack_id, version));
CREATE TABLE outbox   (id TEXT PRIMARY KEY, channel TEXT, payload BLOB, idem_key TEXT UNIQUE,
                       created_at INTEGER, attempts INTEGER DEFAULT 0, last_error TEXT);
CREATE TABLE cursors  (channel TEXT PRIMARY KEY, cursor TEXT, updated_at INTEGER);
CREATE TABLE inbox    (id TEXT PRIMARY KEY, user_id TEXT, kind TEXT, title TEXT, body TEXT,
                       ref TEXT, read INTEGER DEFAULT 0, created_at INTEGER);
CREATE TABLE flags    (id TEXT PRIMARY KEY, user_id TEXT, message_ref TEXT, reason TEXT, comment TEXT,
                       model_id TEXT, citations TEXT, status TEXT, created_at INTEGER);
CREATE TABLE audit    (id INTEGER PRIMARY KEY AUTOINCREMENT, at INTEGER, actor TEXT, action TEXT, detail TEXT);
```

### 16.3 Learner schema (`learner-<id>.db`, abridged)

```sql
CREATE TABLE attempts   (id TEXT PRIMARY KEY, item_id TEXT, outcome_ids TEXT, answer TEXT, correct REAL,
                         hints INTEGER, duration_ms INTEGER, mode TEXT, at INTEGER);
CREATE TABLE mastery    (outcome_id TEXT PRIMARY KEY, p_learned REAL, opportunities INTEGER,
                         last_at INTEGER, next_review TEXT, status TEXT);
CREATE TABLE activities (id TEXT PRIMARY KEY, kind TEXT, subject_id TEXT, outcome_id TEXT,
                         mode TEXT, started_at INTEGER, ended_at INTEGER);
CREATE TABLE test_sessions (id TEXT PRIMARY KEY, test_id TEXT, mode TEXT, state TEXT,
                         answers_json TEXT, started_at INTEGER, submitted_at INTEGER);
CREATE TABLE conversations (id TEXT PRIMARY KEY, mode TEXT, subject_id TEXT, turns_json TEXT,
                         created_at INTEGER, expires_at INTEGER);   -- purged by retention job
CREATE TABLE plugin_kv  (plugin_id TEXT, key TEXT, value BLOB, PRIMARY KEY(plugin_id, key));
```

### 16.4 Knowledge schema (`knowledge.db`)

```sql
CREATE TABLE passages (id TEXT PRIMARY KEY, pack_id TEXT, subject TEXT, level TEXT, outcome_ids TEXT,
                       audience TEXT, title TEXT, text TEXT, source TEXT, locator TEXT);
CREATE VIRTUAL TABLE passages_fts USING fts5(title, text, content='passages', content_rowid='rowid',
                       tokenize='porter unicode61');
CREATE TABLE embeddings (passage_id TEXT PRIMARY KEY, model TEXT, dim INTEGER, vec BLOB);  -- int8
CREATE TABLE outcomes (id TEXT PRIMARY KEY, subject TEXT, level TEXT, topic TEXT, description TEXT,
                       prerequisites TEXT);
CREATE TABLE items    (id TEXT PRIMARY KEY, pack_id TEXT, outcome_ids TEXT, type TEXT, stem TEXT,
                       options_json TEXT, difficulty REAL, audience TEXT);
CREATE TABLE item_keys (item_id TEXT PRIMARY KEY, answer_json TEXT, solution_json TEXT, rubric_json TEXT);
-- item_keys is only readable through ToolService/AssessmentService, never by features directly.
```

### 16.5 Migrations and retention

- Room migrations are explicit and tested (`MigrationTestHelper`); no destructive migrations in release builds.
- A daily `RetentionJob` purges expired conversations, old audit rows and orphaned files according to tenant policy.

---

## 17. File formats

All documents are JSON validated against JSON Schemas in `packs/schema/`. Signed documents use a detached signature envelope.

### 17.1 Signed document envelope

```json
{
  "payload": "<base64url of canonical JSON>",
  "alg": "Ed25519",
  "kid": "aisu-content-2026-01",
  "sig": "<base64url signature over payload bytes>"
}
```

Canonical JSON = UTF-8, sorted keys, no insignificant whitespace (RFC 8785 JCS).

### 17.2 Pack (`.akpack`)

A ZIP file containing the directory structure from the product spec (manifest, curriculum, passages, items, keys, templates, safety profile, optional prebuilt index) plus `SIGNATURE` (envelope over `manifest.json`, which includes the SHA-256 of every other file).

Validation on install: signature → manifest schema → every file hash → licence entitlement → compatibility (`requires.appVersion`, `requires.sdk`, `requires.modelCapabilities`, storage).

### 17.3 Tenant configuration

```json
{
  "schema": "akili.tenant/1",
  "tenantId": "sch-0042",
  "name": "St. Example Secondary School",
  "branding": { "primary": "#1DB089", "logo": "logo.png", "welcome": "Welcome to St. Example" },
  "curriculum": { "id": "ncdc-lower-secondary", "levels": ["S1","S2","S3","S4"] },
  "features": {
    "akili.feature.tutor": { "enabled": true },
    "akili.feature.exam": { "enabled": true },
    "akili.feature.guidance": { "enabled": false }
  },
  "devices": { "mode": "SHARED", "idleSignOutMinutes": 10 },
  "hub": { "url": "https://akili-hub.local", "certSha256": "…" },
  "policyRef": "policy-sch-0042@7"
}
```

### 17.4 Licence

```json
{
  "schema": "akili.licence/1",
  "tenantId": "sch-0042",
  "edition": "school",
  "features": ["tutor","practice","exam","lesson-builder","insights"],
  "packs": ["ug.aistudio.pack.s2-maths", "ug.aistudio.pack.s2-biology"],
  "seats": 250,
  "issuedAt": "2026-09-01T00:00:00Z",
  "expiresAt": "2027-01-31T23:59:59Z",
  "graceDays": 30
}
```

### 17.5 Policy

As in the product specification (§11.2) plus declarative rule data (`blockedTopics`, keyword lists per language, thresholds). Schema `akili.policy/1`.

### 17.6 Update bundle (`.akbundle`, for USB/hub)

```
akili-update-2026T3.akbundle   (ZIP)
├── bundle.json      list of artifacts (type, id, version, sha256, size, target selector)
├── apks/            app APK(s)
├── models/          model files
├── packs/           .akpack / .akdelta
├── docs/            tenant/licence/policy documents for specific tenants
└── SIGNATURE
```

---

# Part D — Low-level design: hub, cloud and protocols

## 18. Akili Hub

### 18.1 Services

| Service | Tech (proposal) | Responsibility |
|---|---|---|
| `hub-gateway` | Kotlin/Ktor (or FastAPI), Caddy for TLS | Single entry point; device and staff authentication; routing; rate limits |
| `hub-sync` | same | Sync protocol endpoints ([§20](#20-sync-protocol)) |
| `hub-distribution` | Static file server with range requests | Serves APKs, models, packs; download manifests per device |
| `hub-dashboard` | Web app (React/Vue or Compose Web), served by gateway | Teacher/admin/IT UI on the LAN |
| `hub-exam` | Ktor + WebSocket | Exam sessions: start/stop, live status, collection |
| `hub-inference` | llama.cpp server or LiteRT on CPU | OpenAI-style `/v1/chat/completions` for the hub model; queue; per-class fair use |
| `hub-updater` | Small daemon | Applies signed bundles (online or USB), with rollback |
| `hub-health` | Daemon + dashboard page | Disk, CPU, temperature, UPS, connected devices, alerts |
| `hub-backup` | Cron job | Nightly DB dump + configs to a second disk/USB; retention 30 days |
| Database | PostgreSQL (≥ 200 devices) or SQLite (small schools) | Hub data |

Services run as containers (`docker compose`), images signed; the updater pulls new images from the cloud or loads them from a bundle.

### 18.2 API (REST, JSON; all under `/api/v1`)

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/enrol` | New device (with enrolment token) | Register device public key; receive tenant, licence, policy, roster |
| POST | `/auth/device` | Device | Device challenge–response → short-lived access token (JWT, 1 h) |
| POST | `/auth/staff` | Dashboard user | Staff login (username + password/PIN) → session cookie |
| GET | `/sync/pull?channels=…&cursors=…` | Device | Pull changes per channel |
| POST | `/sync/push` | Device | Push outbox batch (idempotent) |
| GET | `/distribution/manifest` | Device | What this device should have (app, model, packs, versions) |
| GET | `/distribution/files/{sha256}` | Device | Download artifact (range requests) |
| POST | `/inference/v1/chat/completions` | Device | Hub model inference (streaming SSE) |
| GET/POST | `/classes`, `/users`, `/roster/import` | Admin/IT | Roster management |
| GET/POST | `/assignments` | Teacher | Create and list assignments |
| GET | `/insights/class/{id}` | Teacher | Mastery heat-map data |
| GET/POST | `/marking/queue`, `/marking/{submissionId}` | Teacher | Marking suggestions and approvals |
| POST | `/exams/{id}/start`, `/exams/{id}/end` | Teacher | Exam control |
| WS | `/ws/device` | Device | Push commands (exam start/end, sync now, wipe) |
| WS | `/ws/dashboard` | Teacher | Live exam and device status |
| POST | `/devices/{id}/wipe`, `/devices/{id}/lost` | IT | Device management |
| POST | `/updates/import` | IT | Upload/USB-import a signed bundle |
| GET | `/health` | IT / cloud | Hub health |

### 18.3 Hub data model (abridged)

```
tenants, licences, policies                       (signed docs + parsed columns)
users, classes, class_members, staff_accounts
devices (id, public_key, tier, versions, last_seen, status)
assignments (id, class_id, test_id, due_at, settings_json, created_by)
submissions (id, assignment_id, user_id, answers_json, auto_marks_json, final_marks_json,
             status[received|suggested|approved|released], approved_by)
mastery_snapshots (user_id, outcome_id, p_learned, updated_at)
flags (id, user_id, message_ref, reason, comment, status, correction, reviewed_by)
exams (id, test_id, class_id, state, started_at, ended_at)
change_log (seq BIGSERIAL, channel, entity_id, scope, payload_json, at)   -- drives pull cursors
processed_keys (idem_key PRIMARY KEY, at)                                  -- push idempotency
artifacts (sha256, type, id, version, size, path)
uplink_queue (id, kind, payload_json, created_at, sent_at)                 -- to cloud
```

### 18.4 Discovery

- mDNS service `_akili._tcp.local` with TXT records `tenant=<id>`, `fp=<cert fingerprint>`.
- Fallback: hub URL stored at enrolment; QR code on the hub dashboard.

---

## 19. Akili Cloud

### 19.1 Services

| Service | Responsibility | Key data |
|---|---|---|
| `cloud-api` | Public API gateway for hubs and the console; mTLS for hubs | — |
| `tenant-licence` | Tenants, contracts, licence issuance, renewals | tenants, contracts, licences |
| `fleet` | Hub/device inventory, versions, staged roll-outs, bundle builder | hubs, devices, rollouts |
| `pack-studio` | Ingest → structure → tag → questions → review → build → sign → publish | sources, drafts, reviews, releases |
| `curriculum-registry` | Master curricula and outcomes | curricula, outcomes |
| `model-lab` | Model candidates, device farm runs, eval results, certification | models, runs, scores, certifications |
| `marketplace` | Publisher listings, purchases, revenue share | listings, orders |
| `analytics` | Aggregated metrics from hubs; pilot reports | warehouse tables |
| `support` | Tickets, knowledge base, diagnostics | tickets |
| `signing` | Signs documents and bundles with keys in KMS; audit | signing log |

### 19.2 Hub ↔ Cloud API (abridged)

| Method | Path | Purpose |
|---|---|---|
| POST | `/hubs/register` | Register hub with tenant registration token → hub certificate |
| GET | `/hubs/{id}/updates` | Available bundles/documents for this hub |
| GET | `/artifacts/{sha256}` | Download artifact (CDN) |
| POST | `/hubs/{id}/uplink` | Send approved aggregates, flags, diagnostics (idempotent batches) |
| POST | `/hubs/{id}/tickets` | Create support ticket with diagnostics |

### 19.3 Pack Studio pipeline

```
Upload sources → Rights record ─► Parse (PDF/DOCX/HTML → text + images)
      → Chunk (150–300 words, heading-aware) → Tag to outcomes (AI-suggested, human-confirmed)
      → Question bank (import + AI drafts) → Human review (2-person rule for items)
      → Build (.akpack: JSONL, media, prebuilt FTS + vector index per embedder version)
      → Automatic tests (schema, retrieval coverage per outcome, answer-key checks, safety scan,
                         Model Lab trial per tier)
      → Sign (signing service) → Publish (catalogue + roll-out plan)
```

The same `akpack` CLI (`packs/tools/`) is used locally by developers and in the pipeline.

### 19.4 Model Lab

- **Device farm:** physical reference devices per tier connected to a runner (ADB); plus the hub reference box.
- **Runs:** for each candidate model × tier × pack test set: accuracy, faithfulness (answer supported by cited passage), safety red-team, latency, tokens/s, memory peak, battery, thermal.
- **Output:** a signed **model catalogue** (`akili.models/1`) consumed by `ModelCatalogue` in the app; it replaces today's bundled `model_allowlist_full.json` and fills `estimatedPeakMemoryInBytes`, `minDeviceMemoryInGb`, `llmMaxContextLength` with measured values.

---

## 20. Sync protocol

### 20.1 Principles

- **Pull, then push**, per channel, on every sync.
- **Cursors** for pull: the hub's `change_log.seq` filtered by scope (the device's tenant, classes and users).
- **Idempotency** for push: each outbox entry has an `idemKey` (UUIDv7); the hub stores processed keys for 30 days and ignores repeats.
- **Batching:** max 200 entries or 1 MB per push request.
- **Scope:** a device only receives data for its enrolled classes and signed-in users (plus shared school data).
- **Policy gate on the device:** the channel's `allowedToLeaveDevice(policy)` is checked before anything is enqueued.

### 20.2 Messages

```json
// Pull response
{
  "channels": {
    "assignments": { "items": [ { "op": "upsert", "id": "as-1", "data": { } } ], "cursor": "18421" },
    "roster":      { "items": [], "cursor": "18390" }
  },
  "serverTime": "2026-09-27T08:15:00Z"
}

// Push request
{
  "deviceId": "dev-…",
  "batch": [
    { "idemKey": "0192…", "channel": "submissions", "op": "create", "data": { } },
    { "idemKey": "0193…", "channel": "mastery",     "op": "upsert", "data": { } }
  ]
}

// Push response
{ "acks": ["0192…", "0193…"], "rejected": [ { "idemKey": "…", "reason": "SCHEMA" } ] }
```

### 20.3 Conflict rules

| Data | Rule |
|---|---|
| Roster, policy, config, assignments | Hub is authoritative (down only) |
| Attempts, activities, submissions | Append-only (no conflicts) |
| Mastery snapshots | Device is authoritative for its user; hub keeps latest by `updated_at` |
| Marks | Hub is authoritative (teacher approval happens on hub) |
| Settings per user | Last-writer-wins by `updated_at` |

### 20.4 Retries

Exponential backoff with jitter: 30 s, 1 min, 2 min, … max 30 min; after 10 failed attempts an entry is marked `stuck` and shown in diagnostics (never silently dropped).

---

## 21. Security design

### 21.1 Keys and trust chain

```
AI Studio Root Key (offline, HSM)          signs ──► Signing keys (KMS, rotated yearly)
                                                       ├── content key   → packs, model catalogue
                                                       ├── tenant key    → tenant config, policy, licence
                                                       └── release key   → update bundles, hub images
App ships with: root public key + current signing public keys (updatable via signed key list)
Hub:  hub TLS cert issued by AI Studio CA at registration (mTLS to cloud)
Device: Keystore key pair, public key registered at enrolment; pins hub cert fingerprint
```

### 21.2 Threats and mitigations (STRIDE summary)

| Threat | Example | Mitigation |
|---|---|---|
| Spoofing | Fake hub on school Wi-Fi | Hub cert pinned at enrolment; mutual device auth |
| Tampering | Modified pack via USB | Ed25519 signatures + per-file hashes; reject on mismatch |
| Repudiation | Disputed mark changes | Audit log on hub (who approved what, when) |
| Information disclosure | Lost tablet | SQLCipher + Keystore; PIN; remote wipe; no conversation sync by default |
| Denial of service | One class saturates hub model | Queue + per-class quotas; device fallback |
| Elevation of privilege | Learner accesses answer keys | `item_keys` only via Tool/Assessment services with role checks; teacher content filtered by audience |
| Prompt injection | Malicious text in a scanned page | Retrieved/OCR text wrapped as data in templates; JailbreakRule; tools never execute arbitrary code |
| Clock rollback | Extending an expired licence | Trusted-time rule ([§14.2](#142-licence-service-corelicence)) |

### 21.3 Android hardening

- `android:allowBackup="false"` for learner data (or exclude DBs via backup rules).
- No exported components except the launcher and OAuth redirect (if kept).
- `FLAG_SECURE` on exam and marking screens.
- Network security config: cleartext disabled; hub cert pinning.
- R8 enabled for release; release signing with Play App Signing.

---

# Part E — Engineering

## 22. Error handling and observability

### 22.1 Error model

```kotlin
sealed class AkiliError(val code: String, val userMessage: Int, val retryable: Boolean) {
  class ModelUnavailable(reason: String) : AkiliError("E_MODEL", R.string.err_model, true)
  class OutOfMemory : AkiliError("E_OOM", R.string.err_memory, true)
  class PackInvalid(detail: String) : AkiliError("E_PACK", R.string.err_pack, false)
  class NotEntitled(feature: String) : AkiliError("E_LICENCE", R.string.err_licence, false)
  class PolicyBlocked(ruleId: String) : AkiliError("E_POLICY", R.string.err_policy, false)
  class SyncFailed(cause: Throwable) : AkiliError("E_SYNC", R.string.err_sync, true)
}
```

- Services return `Result<T>`/sealed results; exceptions are caught at service boundaries.
- User-facing messages are friendly and localised; codes appear in diagnostics.
- A plug-in crash in `onStart` disables that plug-in for the session and records it; the rest of the app continues.

### 22.2 Observability (offline-first)

- **Local structured logs** (ring buffer, 5 MB, no personal data) exportable to the hub.
- **Metrics counters** (local): generation latency, tokens/s, retrieval confidence distribution, policy blocks per rule, flags per topic, crash counts. Aggregated on the hub; uplinked to cloud only as aggregates.
- **Crash reports** stored locally and sent via hub; no third-party crash SDK by default.
- Analytics stub (`AisuAnalytics`) is replaced by `core/telemetry` writing to these local counters.

---

## 23. Performance budgets

| Metric | LOW tier | MEDIUM tier | HIGH tier | Hub model (per request) |
|---|---|---|---|---|
| Cold start to sign-in screen | ≤ 2.5 s | ≤ 2 s | ≤ 1.5 s | — |
| Model load (first AI use) | ≤ 8 s | ≤ 10 s | ≤ 10 s | preloaded |
| Retrieval (k=4) | ≤ 150 ms | ≤ 100 ms | ≤ 80 ms | — |
| First token | ≤ 3 s | ≤ 2.5 s | ≤ 2 s | ≤ 2 s on LAN |
| Generation speed | ≥ 8 tok/s | ≥ 10 tok/s | ≥ 15 tok/s | ≥ 20 tok/s per stream |
| Peak RAM (app + model) | ≤ 2.2 GB | ≤ 4 GB | ≤ 6 GB | — |
| Battery (30 min tutoring) | ≤ 10% | ≤ 8% | ≤ 7% | — |
| Storage (app + 1 model + 2 packs) | ≤ 1.5 GB | ≤ 3.5 GB | ≤ 5 GB | — |
| Hub concurrent tutoring streams | — | — | — | ≥ 8 (reference box) |

Budgets are checked in the Model Lab and in CI benchmark jobs on reference devices.

---

## 24. Testing strategy

| Level | What | Tools |
|---|---|---|
| Unit | Every core service, policy rule, mastery math, context budgeter, router, parsers | JUnit, Kotlin coroutines test, fakes from `:core-testing` |
| Contract | Every provider passes the SPI contract test suite (e.g. any `SearchProvider` must pass the same tests) | Shared abstract test classes |
| Plug-in | Each feature tested against a `FakePluginContext` | Compose UI tests |
| Database | Room DAOs and migrations | Robolectric, `MigrationTestHelper` |
| Integration | Boot → enrol → sign in → install pack → ask tutor, with a tiny test model and sample pack | Instrumented tests on emulator |
| Sync | Device ↔ hub protocol with a local hub container | Testcontainers / docker compose in CI |
| Quality | Curriculum test sets, faithfulness, safety red-team, answer-leak | Model Lab runners |
| Performance | Budgets in [§23](#23-performance-budgets) on reference devices | Macrobenchmark + device farm |
| Security | Signature bypass attempts, tampered packs, SQLCipher checks, dependency scanning | Custom tests, OWASP MASVS checklist |

**CI:** every branch and pull request runs lint, unit, contract, plug-in and DB tests plus an APK build; nightly runs integration, sync, quality (subset) and performance.

---

## 25. Migration from the current codebase

The current `akili` code is Google's Edge Gallery with Akili branding in a single `:app` module. The migration is gradual; the app keeps working after every step.

### 25.1 Mapping existing code to the new design

| Existing (upstream / akili) | Becomes |
|---|---|
| `runtime/LlmModelHelper`, `ui/llmchat/LlmChatModelHelper` | Wrapped by `providers/litertlm` implementing `InferenceProvider` |
| `runtime/aicore/AICoreModelHelper` | Optional `providers/aicore` |
| `worker/DownloadWorker`, `data/DownloadRepository` | Used by `core/packs` for large downloads |
| `data/Model`, `data/ModelAllowlist`, `assets/model_allowlist_full.json` | Replaced by `core/inference` `ModelCatalogue` fed by the signed model catalogue |
| `ui/modelmanager/ModelManagerViewModel` (1,469 lines) | Split: model catalogue → `core/inference`; downloads → `core/packs`; UI → `features/device-status` |
| `customtasks/common/CustomTask` + `@IntoSet` modules | Superseded by `AkiliPlugin`; an adapter lets existing CustomTasks run as plug-ins during migration |
| `ui/llmchat` (chat, ask image, ask audio) | Reused UI components inside `features/tutor` and `features/scan` |
| `ui/common/chat/*` | Shared chat UI kit moved to `sdk/ui` (or `core/ui-kit`) |
| `ui/llmchat/SystemPromptUtils.kt` | Replaced by `TemplateEngine` + template data |
| `customtasks/agentchat` + `skills/` | `providers/js-sandbox` + skills exposed as tools (policy-gated) |
| `Analytics.kt` (no-op stub) | `core/telemetry` (local counters) |
| `DataStoreRepository`, protos | Kept for device settings; learner data moves to Room/SQLCipher |
| `ui/navigation/GalleryNavGraph` | Shell builds the graph from plug-in route contributions |
| `ui/home/HomeScreen` (1,168 lines) | Shell role homes composed from `HomeTile` contributions |
| `ui/benchmark` | Kept as a developer/IT diagnostic plug-in; feeds tier detection |
| `ProjectConfig` (Hugging Face OAuth) | Kept only if direct HF downloads remain for developer builds |

### 25.2 Migration steps

| Step | Change | Result |
|---|---|---|
| M0 | Fix build blockers (Windows JDK path, CI on all branches, lint for new code) | Everyone can build and test |
| M1 | Add `build-logic/` convention plugins and `:sdk` module with plug-in contracts; add `:core-kernel` with registry | Plug-in system exists alongside old code |
| M2 | Adapter: wrap existing `CustomTask`s as `AkiliPlugin`s; Shell builds home/navigation from contributions | Old features run through the new registry |
| M3 | `providers/litertlm` wrapping `LlmModelHelper`; `core/inference` with router (device only at first) and template engine | All AI calls go through `InferenceService` |
| M4 | `core/security`, `core/storage` (Room + SQLCipher), `core/tenant`, `core/identity` | Signed config, sign-in, encrypted data |
| M5 | `core/packs`, `core/knowledge` (FTS5 first, vectors next), `packs/tools` CLI + sample pack | Grounded retrieval available |
| M6 | `features/tutor` with Teach Me and Guide Me orchestrators; `core/tools` verification; `core/policy` baseline rules | First grounded, guided tutor |
| M7 | `core/learner` (mastery), `features/practice`, `features/progress` | Adaptive learning |
| M8 | Hub skeleton: gateway, sync, distribution; `core/sync`, `core/device`, `core/licence` | School deployments |
| M9 onward | Teacher features, assessment, hub inference, cloud services | Full product per roadmap |

### 25.3 Working with upstream

- Keep a remote `upstream` → `google-ai-edge/gallery`.
- Merge upstream monthly into a `upstream-sync` branch; resolve conflicts (small, because Akili code lives in new modules); run the full test suite; merge into the main development branch.
- Never reformat or rename upstream files unnecessarily.

---

## 26. Glossary

| Term | Meaning |
|---|---|
| **Akili SDK** | The stable contracts (interfaces, models, events) that plug-ins build against |
| **Kernel** | The minimal core that boots the app and manages plug-ins |
| **Feature plug-in** | A user-facing capability delivered as a Gradle module and enabled by config |
| **Provider** | A swappable implementation of a core service interface (SPI) |
| **Content plug-in** | Signed data installed at runtime: packs, models, templates, policies |
| **Pack (`.akpack`)** | A signed bundle of curriculum content, questions, templates and rules |
| **Tenant** | A school or institution deployment with its own configuration |
| **Tier** | Device capability class (LOW, MEDIUM, HIGH) used to choose models and features |
| **Hub** | The school server that distributes content, collects results and optionally runs a larger model |
| **Outbox** | Durable queue of data waiting to be synced |
| **BKT** | Bayesian Knowledge Tracing, the mastery estimation method |
| **RRF** | Reciprocal Rank Fusion, used to combine keyword and vector search results |
| **SPI** | Service Provider Interface |
| **ADR** | Architecture Decision Record |
