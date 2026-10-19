# SendPromo - DJ & Record Label Promo Dispatcher

SendPromo is a professional promo delivery application built for DJs, music producers, and record labels. It streamlines composing, polishing with Gemini AI, organizing recipient groups, and delivering audio promos and release announcements at scale.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned during the interactive design phase:

- **Confirmed Decision 1: Email Dispatch Flow**: Direct Gmail integration with automatic standard intent fallback. Users can connect their Google Account / Gmail and dispatch directly or choose the native Android email client with all pre-filled metadata (recipients, structured subject, formatted promo pitch, streaming & download links).
- **Confirmed Decision 2: Gemini AI Intelligence**: Embedded Gemini 3.8 Flash intelligence with two primary capabilities:
  1. **AI Pitch Writer**: Generates club-ready, punchy DJ promo descriptions, press quotes, and hype hooks tailored to the genre.
  2. **Smart Audio Tagging**: Suggests matching electronic music genres/sub-genres, optimal club tempo (BPM), key hints, and hashtag packages.
- **Confirmed Decision 3: Seed Contact Groups**: Database will be pre-populated with ready-to-use sample promo pools ("Tech House Top 50", "Melodic Techno Labels & A&R", "Festival & Club Residents", "BBC / Radio 1 Tastemakers") so users can immediately test the full dispatch flow.

---

## 1. Overview & Core Concept

- **What It Does**: SendPromo allows electronic music artists and record labels to compose high-impact track promo mailouts in minutes. Users enter artist details, track name, release type (Original, Remix, Mashup), record label status, and custom pitch note; select delivery methods (audio attachments, download links, SoundCloud secret tokens, Google Drive links); select target DJ groups; and send promos directly.
- **Target Audience / Persona**: Electronic music artists, touring DJs, independent record label A&Rs, and promo pool managers seeking to replace slow, manual copy-pasting with a sleek, automated campaign tool.
- **Key Value**: Drastically cuts time spent on promo distribution, avoids spam filters through personalized formatting, preserves draft progress with auto-save, and tracks delivered campaigns.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Home / Dashboard**:
   - Centered hero branding: App name "SendPromo", tagline *"Envía promos más rápido de forma sencilla"*, prominent *"Comenzar ya"* (Get Started) button, and clickable *"Ver actividad"* (View Activity) link.
   - Top right app bar: Quick access to the **Database** (Recipient Groups) manager.
2. **Promo Composer (Apartado Mensaje)**:
   - **Step 1 - Artist & Track Info**: Real name (optional), City & Country (optional), Artist name(s) (mandatory), Track name (mandatory), Release type (Original, Remix, Mashup segmented selector), Release date picker, Record label toggle (Yes with label name input, or No).
   - **Step 2 - Pitch & Gemini AI Assistant**: 2000-character message area with instant Gemini AI action pills: "✨ Polish DJ Pitch", "🔥 Generate Club Hype", "🎧 Suggest Genre & BPM Tags".
   - **Step 3 - Delivery Formats (Max 2)**:
     - 🔗 Direct Download URL
     - 📎 File Attachment (`.mp3`, `.wav`, `.flac`, `.ogg`, `.aiff`, `.rar`, `.zip`) with Android system picker
     - ☁️ Google Drive link selector / generator
     - 🎵 SoundCloud link (with support for private tracks and secret sharing tokens)
   - Auto-draft background saving active every 5 minutes and on progression.
3. **Structured Review & Preview**:
   - Automatically formats standardized DJ industry subject line:
     `Artist Name - Track Name (Release Type) – Label Name`
   - Formatted body structure combining artist bio metadata, record label info, pitch note, and delivery links.
4. **Target Recipients (Apartado Destinatarios)**:
   - Expandable group cards (accordion). All groups selected by default.
   - Accordion expansion reveals inline email search bar and individual email checkboxes with instant uncheck / check-all controls.
5. **Dispatch & Completion Animation**:
   - Tap "Enviar": Interactive button shows progress spinner.
   - Full-screen celebratory success view with animated green checkmark, prominent "Enviado" heading, and reassuring confirmation:
     *"Revisa en tu Gmail la carpeta de Enviados para asegurarte que todo esté OK"*.
6. **Database Manager**:
   - Displays list of groups sorted by latest creation / modification timestamp.
   - Top-right `+` button opens clean dialog to define Group Name and add multiple email addresses with a dynamic `+` button.
   - 3-dots overflow menu per group with "Editar grupo" (Edit group) and "Eliminar grupo" (Delete group).
7. **Activity Center (Ver Actividad)**:
   - Two distinct tabs: **Tareas finalizadas** (Sent promos history with timestamps, recipient counts, and subject) and **Borradores** (Drafts).
   - Tapping any draft reloads the composer state at the exact saved stage.

### Visual Identity & Theme

- **Aesthetic Direction**: *Midnight Club & Studio Darkroom* — sleek, modern, electronic music aesthetic inspired by Pioneer CDJ / Ableton dark themes with luminous neon accents.
- **Color Palette**:
  - Background: Deep Obsidian (`#0F1117`) and Surface Charcoal (`#1A1D27`)
  - Accent / Primary: Neon Cyan (`#00E5FF`) and Electric Violet (`#7C4DFF`)
  - Success: Acid Emerald (`#00E676`)
  - Text: Bright White (`#F5F5F7`) and Muted Titanium (`#9E9EB2`)
- **Typography**: Clean, geometric modern sans typography with high legibility for track titles, BPM counters, and email addresses.
- **Micro-interactions**: Spring-based accordion expansions, animated checkmark popups, smooth tab transitions, and tactile button feedback.

---

## 3. Key Product Decisions & Trade-Offs

- **Hybrid Dispatch Architecture**:
  - *Chosen Approach*: Direct Google Account / Gmail API integration paired with seamless Android Intent fallback (`Intent.ACTION_SENDTO` / `ACTION_SEND_MULTIPLE`).
  - *Why*: Ensures 100% sending reliability regardless of local Gmail app configurations, device accounts, or network states.
- **Dual-Tier Persistence (Firestore Cloud + Local Cache)**:
  - *Chosen Approach*: Cloud Firestore backend for contact groups, drafts, and sent campaigns, with instant offline access and responsive UI state.
  - *Why*: DJs and label managers switch between mobile and studio devices; cloud persistence prevents loss of curated contact lists.
- **On-Device & Gemini 3.8 Flash AI Integration**:
  - *Chosen Approach*: Firebase AI Logic using `gemini-3.8-flash` with structured system instructions specialized in electronic music marketing and DJ promo etiquette.
  - *Why*: Provides near-instant response times and high-quality tone adjustment (underground club vs commercial radio).

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                        SendPromo UI                             │
├─────────────────┬──────────────────────┬────────────────────────┤
│   HomeScreen    │   PromoComposer      │   DatabaseScreen       │
│  - Hero Brand   │  - Track Metadata    │  - Group Accordions    │
│  - Get Started  │  - Gemini AI Pitch   │  - Email Search & Edit │
│  - Activity Nav │  - Delivery Channels │  - Add Group Modal     │
└────────┬────────┴──────────┬───────────┴───────────┬────────────┘
         │                   │                       │
┌────────▼───────────────────▼───────────────────────▼────────────┐
│                  SendPromo ViewModel Layer                      │
│   (StateFlow, Credential Manager, Draft AutoSave, Validation)   │
└────────┬───────────────────┬───────────────────────┬────────────┘
         │                   │                       │
┌────────▼────────┐ ┌────────▼────────┐     ┌────────▼────────────┐
│  Firebase Auth  │ │ Firebase AI     │     │ Firestore Database  │
│  & Google Sign  │ │ Logic (Gemini)  │     │ - /users/{uid}/     │
│  Credential Mgr │ │ - DJ Pitcher    │     │     groups/         │
│                 │ │ - Genre Tagger  │     │     drafts/         │
│                 │ │                 │     │     campaigns/      │
└─────────────────┘ └─────────────────┘     └─────────────────────┘
```

### Data Model

1. **PromoGroup**:
   - `id: String`, `name: String`, `emails: List<String>`, `updatedAt: Long`, `createdAt: Long`
2. **PromoDraft**:
   - `id: String`, `realName: String?`, `cityCountry: String?`, `artistNames: String`, `trackTitle: String`, `releaseType: String`, `releaseDate: String`, `isLabelRelease: Boolean`, `labelName: String?`, `pitchMessage: String`, `deliveryMethods: List<DeliveryOption>`, `selectedGroupIds: List<String>`, `excludedEmails: List<String>`, `updatedAt: Long`
3. **SentCampaign**:
   - `id: String`, `subject: String`, `bodyPreview: String`, `recipientCount: Int`, `recipients: List<String>`, `sentAt: Long`, `status: String`
4. **DeliveryOption**:
   - `type: DeliveryType` (LINK, ATTACHMENT, GOOGLE_DRIVE, SOUNDCLOUD), `uriOrUrl: String`, `isPrivateSoundCloud: Boolean`, `secretToken: String?`

### Interactive Component & State Mapping

- **Auto-Save Engine**: Runs a background coroutine timer saving the current draft state every 5 minutes, as well as on screen navigation / step transitions.
- **Gemini Assistant Drawer**: Floating bottom sheet or expandable card providing real-time AI pitch refinement without losing user input.
- **Dispatch Animator**: Handles the two-stage visual confirmation (button loading state -> full-screen celebration card with Sent badge and guidance note).
