# Functional Specification
**Document:** `functional.md`

---
## Overview
This document defines what every agent in Project Chimera must be able to do, written as user stories. Each story follows the format:

> "As a [Agent], I need to [Action] so that [Outcome]"

These stories are derived directly from the SRS Functional Requirements (FR 1.0 - FR 6.1) and represent the executable intent of the system.

**Rule:** No implementation code is written until every story in this document is reviewed and approved.

**Traceability Matrix:** Each story references its parent SRS requirement for audit purposes.

---

## FR 1.0 - Cognitive Core & Persona Management
### User_story-001: Persona Loading
**As a** Cognitive Core Agent,
**I need to** load and parse the `SOUL.md` file for the assigned influencer
**So that** I have the correct personality, voice, tone, and ethical constraints before performing any action.

**Acceptance Criteria:**
- SOUL.md must contain ALL four sections:
  - Backstory
  - Voice/Tone
  - Core Beliefs & Values
  - Directives
- If ANY section is missing -> throw PersonaLoadException
- If SOUL.md file is missing entirely -> throw PersonaLoadException
- Persona is loaded ONCE at agent startup
- Persona is NEVER modified at runtime. It is strictly read-only
- No agent performs ANY action before persona is loaded
---

### User_story-002: Context Assembly
**As a** Cognitive Core Agent,
**I need to** assemble a complete system prompt by combining the SOUL.md persona, short-term memory from Redis, and long-term memories from Weaviate
**So that** the other agents have full context before performing any reasoning steps.

**Acceptance Criteria:**
- Must call mcp-server-redis BEFORE assembling context
- Must call mcp-server-weaviate BEFORE assembling context
- SOUL.md persona is ALWAYS included — never optional
- Context must clearly separate three sections:
  WHO YOU ARE / RECENT ACTIVITY / LONG TERM MEMORIES
- If context exceeds LLM window limit:
  > Truncate long-term memories first
  > Never truncate persona
  > Never truncate short-term memory
- Context assembly happens BEFORE every reasoning step
  without exception

---
### User_story-003: Persona Evolution Trigger
**As a** Judge Agent,
**I need to** detect when an approved interaction achieves high engagement
**So that** the agent's long-term memory can be updated to reflect what works.

**Acceptance Criteria:**
- Judge monitors engagement score after every 
  approved post
- If engagement score exceeds threshold (e.g 0.80)
  > trigger Memory Writer background process
- If engagement score is below threshold
   > do nothing
- Triggering is fire-and-forget. Judge does NOT wait for Memory Writer to finish
- Judge logs the trigger event to PostgreSQL

---
### User_story-004: Memory Writing
**As a** Memory Writer Worker,
**I need to** summarise a high-engagement interaction and write it to the agent's Weaviate memory collection
**So that** the influencer progressively learns what content resonates with their audience.

**Acceptance Criteria:**
- Runs as a background process - never blocks the main pipeline
- Must summarise the interaction into a concise memory entry (max 200 words)
- Must write to the MUTABLE memories collection in Weaviate (NOT the SOUL.md - that stays locked)
- If Weaviate write fails: retry 3 times then log error
- Memory entry must include engagement score and date

---

## FR 2.0 - Perception System

### User_story-005: Active Resource Monitoring and Filtering

**As a** Perception Agent,
**I need to** continuously poll all configured MCP Resources, score incoming content for 
relevance, and pass only high-scoring content to the Planner
**So that** the system only reacts to information that actually matters to the current campaign goals.

**Acceptance Criteria:**
- Must poll ALL configured resources on a set interval
- Polling interval is configurable (default: 60 seconds)
- Every piece of content must be scored using a lightweight LLM (Gemini Flash / Claude Haiku)
- Score must be between 0.0 and 1.0
- Only content scoring ABOVE 0.75 reaches the Planner
- Content below 0.75 is silently discarded and logged
- If a resource is unavailable: skip it, log error, continue polling other resources
- Never crash because one resource is down

---

### User_Story-007: Trend Detection
**As a** Trend Spotter Worker,
**I need to** analyse aggregated news headlines every 4 hours and detect emerging topic clusters
**So that** the Planner is alerted to content opportunities before they peak.

**Acceptance Criteria:**
- Runs as a background process every 4 hours
- Collects headlines via mcp-server-news 
- Headlines stored temporarily in Redis during the 4 hour window
- After 4 hours: sends headlines to lightweight LLM for semantic cluster detection(Gemini Flash / Claude Haiku to keep cost low)
- A cluster = 3 or more semantically related topics
- If cluster found: generate Trend Alert
- If no cluster found: log and reset for next window
- Trend Alert is injected into Planner GlobalState
- Never uses expensive LLM for this - cost must be minimal

---

## FR 3.0 - Creative Engine

### User_Story-008: Content Direction
**As a** Creative Worker Agent,
**I need to** act as the director of content creation by deciding what to generate and which tools to call
**So that** the post has all required components(text, image, video) that match the Creative Brief.

**Acceptance Criteria:**
- Must read Creative Brief BEFORE doing anything
- Must read SOUL.md voice traits BEFORE writing text
- Must generate ALL required components before passing to Judge
- Text is generated natively by LLM
- Images are generated via MCP Tool (never by LLM)
- Videos are generated via MCP Tool (never by LLM)
- If any component generation fails, retry once then escalate to Judge
- Every image request MUST include character_reference_id, no exceptions
- Must log estimated cost before calling any generation tool

**LLM Used:** Gemini Pro / Claude Opus
(expensive model - used here because content quality is critical)

---

### User_Story-009: Caption and Script Generation
**As a** Creative Worker Agent,
**I need to** generate captions and scripts natively using the LLM in the influencer's voice
**So that** all text content sounds authentic and matches the campaign tone.

**Acceptance Criteria:**
- Must read SOUL.md voice traits before generating
- Must read Creative Brief tone and dont_do list
- Caption must not exceed platform character limit(Instagram: 2200 / Twitter: 280 / TikTok: 2200)
- Must never include content in the dont_do list
- Must never sound generic. voice must match SOUL.md traits exactly

**LLM Used:** Gemini Pro / Claude Opus
---

### User_Story-010: Image Generation with Character Consistency
**As a** Creative Worker Agent,
**I need to** generate an image via mcp-server-ideogram or mcp-server-midjourney with the character reference always attached
**So that** the influencer looks the same across every single post.

**Acceptance Criteria:**
- character_reference_id is MANDATORY in every image request. If missing:
  > throw MissingCharacterReferenceException and abort. Never generate without it
- Style directives must come from Creative Brief
- If mcp-server-ideogram fails, fallback to mcp-server-midjourney
- If both fail, retry once then escalate to Judge
- Must log generation cost before AND after call
---

### User_Story-011: Image Consistency Validation
**As a** Judge Agent,
**I need to** compare the generated image against
the reference image using a Vision LLM
**So that** only images where the influencer
is recognisable are approved for publishing.

**Acceptance Criteria:**
- Must receive BOTH generated_image_url AND reference_image_url. If either is missing
  > throw ValidationException immediately

- Must use a vision-capable LLM ONLY(Gemini Pro Vision / GPT-4o)
- LLM must answer strictly YES or NO to: "Does the person in image A look like the same person in image B?"
- YES -> APPROVE -> pass to next step
- NO  -> raise ValidationError with reason -> Worker retries image generation
- If retry fails twice -> escalate to human
- Must handle API timeouts gracefully(timeout after 10 seconds -> retry once)
- Validation result must be logged with both image URLs

**LLM Used:** Gemini Pro Vision / GPT-4o (must be vision-capable. Regular LLM cannot see images)

---

## FR 4.0 - Action System(Social Interface)

### User_Story-012: Platform-Agnostic Publishing
**As a** Worker Agent,
**I need to** execute ALL social media actions exclusively through MCP Tools
**So that** the system has standardised governance, rate limiting, and logging across every platform.

**Acceptance Criteria:**
- Direct social media API calls are STRICTLY PROHIBITED from agent code.
- Every publish action must go through MCP layer
- Agent treats "post to Twitter" and "post to Instagram" as identical tool calls
- MCP layer must enforce:
  > Rate limiting per platform
  > Full logging of every action
  > Dry-run mode for testing (posts are simulated, not sent)
- If MCP Tool is unavailable -> queue the action, retry after 60 seconds
- Never attempt to bypass MCP even if a platform API is directly accessible

---

### User_Story-013: Comment Ingestion and Reply Planning
**As a** Planner Agent,
**I need to** read incoming comments via MCP Resource and create a structured Reply Task for the Worker
**So that** every relevant comment gets a timely, context-aware response.

**Acceptance Criteria:**
- Must read mentions via MCP Resource ONLY e.g(twitter://mentions/recent)
- Must NOT create a Reply Task for every comment
  > Only comments that passed the Perception Agent relevance filter (score above 0.75)
- Reply Task must contain full comment context so Worker never needs to fetch it again
- Task must be pushed to TaskQueue (Redis) immediately after creation
- Must assign priority level to every Reply Task:
  > P1 -> Direct questions
  > P2 -> Compliments
  > P3 -> General mentions
  > P4 -> Spam -> DISCARD, no task created

---

### User_Story-014: Context-Aware Reply Generation
**As a** Worker Agent,
**I need to** generate a reply that sounds exactly like the influencer by consulting memory before writing anything
**So that** followers feel they are talking to a real, consistent, remembering person.

**Acceptance Criteria:**
- Must read SOUL.md BEFORE generating any reply
- Must fetch last 1 hour history from Redis to maintain conversation continuity
- Must query Weaviate for relevant long-term memories related to the comment topic
- Reply must match the influencer's voice traits exactly 
- Reply must not exceed platform character limit (Twitter: 280 / Instagram: 2200)
- Reply must NEVER be posted directly. It must be passed to Judge first
- Must NEVER reply to P4 (spam) comments

---

### User_Story-015: Pre-Publication Safety Gate
**As a** Judge Agent,
**I need to** review and approve every reply before the reply is sent/posted and execution finalises
**So that** nothing unsafe, off-brand or controversial is ever posted publicly.

**Acceptance Criteria:**
- Judge INTERCEPTS the tool call before it executes not after.
- Must check reply against ALL of these:
  > Is it on-brand? (matches SOUL.md)
  > Is it safe? (no controversy)
  > No legal risk?
  > No political content?
  > No sensitive topics?
- APPROVE -> tool execution resumes -> reply posted
- REJECT  -> tool execution cancelled -> Worker regenerates reply
- ESCALATE -> tool execution cancelled -> human notified immediately
- Max 3 Worker retries before escalating to human
- Judge must complete review within 5 seconds(replies must feel timely)
- Every decision must be logged with reason

**Escalation Triggers (auto-escalate, no retry):**
- Legal threats
- Harassment or hate speech
- Political topics
- Verified or high-profile accounts
- Impersonation claims

