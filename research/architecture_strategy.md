# Project Chimera — Architecture Strategy
**Document:** `architecture_strategy.md`  


---

## 1. Executive Summary

Project Chimera is a platform that manufactures and operates thousands of Autonomous AI Influencers simultaneously. Each influencer researches trends, generates content, manages social media engagement and participates in commerce.

This document defines:
- The Agent Pattern chosen and why
- Where humans intervene (Human-in-the-Loop)
- The database strategy for high-velocity video metadata
- The full system architecture with diagrams

---

## 2. Agent Pattern Decision

### 2.1 Patterns Considered

Hierarchical Swarm 

### 2.2 Why Hierarchical Swarm

Project Chimera requires:
- Thousands of influencers running simultaneously
- Multiple content tasks executing in parallel per influencer
- Clear authority chains for quality control
- Independent engagement handling alongside content creation

A **Hierarchical Swarm** satisfies all of these because:
1. A top-level Orchestrator commands Campaign Managers
2. Campaign Managers command Planners
3. Planners command Workers
4. All parallel threads operate independently
5. Judges at every layer enforce quality without blocking other threads

A Sequential Chain would mean one task finishes before the next starts - unacceptable at the scale of thousands of influencers and millions of daily operations.

### 2.3 The Three-Role Core (FastRender Swarm Pattern)

Every campaign thread runs on three specialised roles:

```mermaid
flowchart LR
    P[Planner<br/>Strategist] --> W[Worker<br/>Executor]
    W --> J[Judge<br/>Gatekeeper]
    J -->|APPROVE| NEXT[Next Step]
    J -->|REJECT| W
    J -->|ESCALATE| H[Human]
```


- **Planner**: Thinks and strategises. Breaks goals into tasks.
- **Worker**: Executes one atomic task with maximum speed.
- **Judge**: Reviews output. Has absolute authority to approve, reject, or escalate.

---

## 3. Full System Architecture

### 3.1 Platform Layer (Top Level)

The Platform Layer sits above all individual influencers. It manages the fleet.

```mermaid
flowchart TD
    PL["PLATFORM LAYER"]
    IF["Influencer Factory<br/>(creates & retires influencers)"]
    IR["Influencer Registry<br/>(tracks all active influencers)"]
    GCFO["Global CFO Agent<br/>(budget across all influencers)"]
    GS["Global Scheduler<br/>(staggered posting times)"]

    PL --> IF
    PL --> IR
    PL --> GCFO
    PL --> GS
```

### 3.2 Per-Influencer Architecture

Each influencer runs two completely independent systems that share one identity file: `SOUL.md`.

```mermaid
flowchart TD
    SOUL["SOUL.md<br/>(Shared Identity — Read Only)"]

    subgraph CONTENT["Content Pipeline (Campaign-Driven)"]
        MO["Master Orchestrator"]
        CO["Campaign Orchestrator"]
        CM["Campaign Manager"]
    end

    subgraph ENGAGEMENT["Engagement Agent (24/7)"]
        PA["Perception Agent"]
        PQ["Priority Queue"]
        EP["Engagement Planner"]
        EW["Engagement Worker"]
        EJ["Engagement Judge"]
    end

    SOUL --> CONTENT
    SOUL --> ENGAGEMENT
```

### 3.3 Content Pipeline — Full Flow

```mermaid
flowchart TD
    MO["MASTER ORCHESTRATOR<br/>• Receives campaigns<br/>• Health monitoring<br/>• Restarts crashed orchestrators"]
    CO["CAMPAIGN ORCHESTRATOR<br/>• Breaks campaign into steps<br/>• Spawns one Planner per step<br/>• Hands off to Manager"]
    CM["CAMPAIGN MANAGER<br/>• Creates & locks Creative Brief<br/>• Tracks Worker status via Redis<br/>• Controls budget<br/>• Activates Compiler"]
    CB["CREATIVE BRIEF<br/>Locked. Read-only. Shared by all Workers."]

    subgraph PARALLEL["Parallel Planner Threads"]
        PA["Planner A<br/>Step 1"]
        PB["Planner B<br/>Step 2"]
        PC["Planner C<br/>Step 3"]
        PJA["Planner Judge A"]
        PJB["Planner Judge B"]
        PJC["Planner Judge C"]
        WPA["Worker Pool A<br/>W1 W2 W3"]
        WPB["Worker Pool B<br/>W1 W2 W3"]
        WPC["Worker Pool C<br/>W1 W2 W3"]
        WJA["Worker Judges A"]
        WJB["Worker Judges B"]
        WJC["Worker Judges C"]
    end

    REDIS["REDIS REGISTRY<br/>W1 → APPROVED <br/>W2 → APPROVED <br/>W3 → RETRYING <br/>W4 → FAILED "]
    HC1["HUMAN CHECKPOINT 1<br/>Review worker outputs<br/>APPROVE / REJECT / EDIT"]
    COMP["COMPILER AGENT<br/>Reads all approved .md files<br/>Assembles final post"]
    CJ["COMPILER JUDGE<br/>Coherent? Matches Brief? Safe?"]
    HC2["HUMAN CHECKPOINT 2<br/>Final review<br/>APPROVE / EDIT / REJECT"]
    PUB["PUBLISHED"]

    MO --> CO --> CM --> CB
    CM --> PA & PB & PC
    PA --> PJA --> WPA --> WJA
    PB --> PJB --> WPB --> WJB
    PC --> PJC --> WPC --> WJC
    WJA & WJB & WJC --> REDIS
    REDIS --> CM
    CM --> HC1 --> COMP --> CJ --> HC2 --> PUB
```

### 3.4 Engagement Agent — Full Flow

Runs independently from the Content Pipeline. Never blocked by campaigns.

```mermaid
flowchart TD
    SM["SOCIAL PLATFORM<br/>Comments arriving 24/7"]
    PER["PERCEPTION AGENT<br/>• Reads mentions & comments<br/>• Filters spam<br/>• Scores relevance (threshold: 0.75)"]
    PQ["PRIORITY QUEUE<br/>P1: Direct questions → reply fast<br/>P2: Compliments → reply within hours<br/>P3: Mentions → reply within 24hrs<br/>P4: Spam → IGNORE"]
    EPLANNER["ENGAGEMENT PLANNER<br/>• Categorises comment<br/>• Decides response strategy<br/>• Flags sensitive content"]
    EW["ENGAGEMENT WORKER<br/>• Reads SOUL.md<br/>• Reads Redis (recent history)<br/>• Reads Weaviate (long-term memory)<br/>• Drafts reply in influencer's voice"]
    EJ["ENGAGEMENT JUDGE<br/>• On brand?<br/>• Safe?<br/>• No legal/political risk?"]
    REPLY["REPLY POSTED"]
    HUMAN["HUMAN REVIEW<br/>Sensitive / Legal / High-profile"]

    SM --> PER --> PQ --> EPLANNER --> EW --> EJ
    EJ -->|APPROVE| REPLY
    EJ -->|ESCALATE| HUMAN
```

---

## 4. Human-in-the-Loop (HITL) Strategy

### 4.1 Philosophy

Humans do not run the system. They **govern** it. The system operates autonomously unless it reaches a checkpoint or escalation trigger.

### 4.2 The Three Human Touchpoints

```mermaid
flowchart LR
    T0["TOUCHPOINT 0<br/>Campaign Intake<br/>Human defines goals<br/>Before anything starts"]
    T1["TOUCHPOINT 1<br/>Pre-Compiler Review<br/>Human reviews worker outputs<br/>Before expensive compilation"]
    T2["TOUCHPOINT 2<br/>Final Approval<br/>Human approves final post<br/>Before publishing"]
    T3["ESCALATION<br/>Anytime<br/>Judge flags sensitive content<br/>Immediate human intervention"]

    T0 --> T1 --> T2
    T3 -.->|triggered anytime| T2
```

| Touchpoint | When | Human Action | Cost of Getting it Wrong |
|---|---|---|---|
| **0 - Campaign Intake** | Before pipeline starts | Define goals, tone, budget | None - nothing has run yet |
| **1 - Pre-Compiler** | After all Workers approved | Approve / Reject / Edit outputs | Low - just redo specific Workers |
| **2 - Final Approval** | After Compiler Judge approves | Approve / Edit / Reject final post | Medium - redo compilation |
| **Escalation** | Anytime, triggered by Judge | Handle sensitive content | Prevents brand/legal damage |

### 4.3 Escalation Triggers — What Always Goes to a Human

| Trigger | Reason |
|---|---|
| Legal threats in comments | Liability |
| Harassment | Safety |
| Political topics | Brand risk |
| Verified / high-profile account interaction | High visibility mistake |
| Budget limit exceeded | Financial control |
| Worker fails 3 retries | Quality breakdown |
| Low confidence score from Judge | Uncertainty |
| Impersonation claims | Legal risk |

---

## 5. Database Strategy

### 5.1 The Core Question

High-velocity video metadata means:
- Hundreds of videos processed simultaneously
- Metadata structure varies per video (some have transcripts, some don't)
- Constant updates (views, likes, engagement scores)
- Need to query by semantic similarity ("find videos like this one")

This rules out a single database solution. **Chimera uses a polyglot persistence strategy** — the right database for each type of data.

### 5.2 Database Decision Matrix

| Data Type | Volume | Structure | Best Fit | Choice |
|---|---|---|---|---|
| Video metadata | Very High | Flexible, varies | Document Store | **MongoDB** |
| Trending topics, live counters | Extreme | Key-Value | In-memory cache | **Redis** |
| Agent memories, semantic search | High | Vector embeddings | Vector DB | **Weaviate** |
| Users, campaigns, financial logs | Medium | Fixed, relational | SQL | **PostgreSQL** |
| Financial transactions | Low | Immutable ledger | Blockchain | **Base / Ethereum** |

### 5.3 Why MongoDB for Video Metadata

SQL forces every row to have the same columns. Video metadata does not cooperate:

```
Video A: { title, tags, views, transcript, sentiment_score }
Video B: { title, tags, views, duration, audio_fingerprint, location }
Video C: { title, tags, views, captions, character_reference_id, style_lora }
```

MongoDB stores each as a flexible document. No empty columns. No schema migrations every time a new field is needed.

```mermaid
flowchart LR
    subgraph MONGO["MongoDB — Video Metadata"]
        V1["Video Doc 1<br/>title, tags, views<br/>transcript, sentiment"]
        V2["Video Doc 2<br/>title, tags, views<br/>duration, audio"]
        V3["Video Doc 3<br/>title, tags, views<br/>style_lora, character_ref"]
    end

    subgraph REDIS["Redis — Live Data"]
        T1["trending_now → dance challenge"]
        T2["influencer_001:budget → $32.50"]
        T3["worker_3:status → APPROVED"]
    end

    subgraph WEAVIATE["Weaviate — Semantic Memory"]
        M1["Memory: 'User loved beach content'"]
        M2["Memory: 'Campaign 12 tone was vibrant'"]
        M3["Persona: SOUL.md embeddings"]
    end

    subgraph POSTGRES["PostgreSQL — Structured Records"]
        U["Users & Accounts"]
        C["Campaigns"]
        L["Audit Logs"]
    end
```

### 5.4 Data Flow Through the Stack

```mermaid
sequenceDiagram
    participant W as Worker Agent
    participant M as MongoDB
    participant R as Redis
    participant WV as Weaviate
    participant PG as PostgreSQL

    W->>R: Check trending topics
    R-->>W: ["dance challenge", "AI news"]
    W->>WV: Search past memories for context
    WV-->>W: Relevant memories returned
    W->>M: Save generated video metadata
    M-->>W: Saved 
    W->>R: Update status → APPROVED
    W->>PG: Log task completion
```

---

## 6. Failure Handling Summary

```mermaid
flowchart TD
    FAIL["Failure Event"]
    WF["Worker Fails"]
    MF["Manager Crashes"]
    BF["Budget Exceeded"]
    SF["Sensitive Content"]

    FAIL --> WF & MF & BF & SF

    WF -->|Retry 1-2-3| RETRY["Retry"]
    RETRY -->|Still fails| HC1["Human Checkpoint 1"]

    MF --> MO["Master Orchestrator<br/>Detects via heartbeat<br/>Restarts Manager<br/>Recovers state from Redis"]

    BF --> CFO["Global CFO Agent<br/>Blocks ALL Workers<br/>Flags for human"]

    SF --> EJ["Engagement Judge<br/>Escalates immediately<br/>Human handles it"]
```

---

## 7. Technology Stack Summary

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 21+ | Virtual Threads for massive concurrency |
| Framework | Spring Boot | Enterprise agent services |
| Agent Protocol | A2A (Agent-to-Agent) | Inter-agent communication |
| External Tools | MCP (Model Context Protocol) | Agent-to-tool communication |
| Content Pipeline State | Redis | Task queue, status registry |
| Video Metadata | MongoDB | Flexible high-velocity document storage |
| Long-term Memory | Weaviate | Semantic vector search |
| Structured Data | PostgreSQL | Users, campaigns, logs |
| Financial Ledger | Base / Ethereum | Immutable transaction records |
| AI Reasoning | Claude Opus / Gemini Pro | Complex planning and judging |
| AI Routine Tasks | Claude Haiku / Gemini Flash | High-volume low-cost tasks |
| Containers | Docker + Kubernetes | Auto-scaling agent workloads |
| Cloud | AWS / GCP Hybrid | High availability infrastructure |

---

## 8. Architecture in One Paragraph

Project Chimera uses a **Hierarchical Swarm pattern** where a Platform Layer manages thousands of independent Influencer Instances. Each instance runs two parallel systems: a Campaign-driven Content Pipeline and a 24/7 Engagement Agent, both sharing a single identity file (`SOUL.md`). The Content Pipeline flows from Master Orchestrator → Campaign Orchestrator → Campaign Manager → parallel Planners → parallel Workers → three-tier Judges (Planner, Worker, Compiler) → two Human Checkpoints → Published. The Engagement Agent runs independently, filtering comments through a Priority Queue, drafting replies using the influencer's memory and voice, and escalating sensitive content to humans immediately. The database strategy uses MongoDB for flexible high-velocity video metadata, Redis for live state and caching, Weaviate for semantic agent memory, and PostgreSQL for structured relational data. Humans intervene at three defined points and via escalation — governing the system without running it.