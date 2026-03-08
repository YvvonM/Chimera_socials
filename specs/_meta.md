# Meta Specification
**Document:** `_meta.md`

--

## 1. Vision

Project Chimera is a cloud-native platform that manufactures and operates thousands of Autonomous AI Influencers simultaneously.

Each influencer is a digital entity that:
- Researches trending topics autonomously
- Generates multimodal content (text, image, video)
- Engages with social media audiences in real time
- Participates in on-chain commerce independently
- Maintains a consistent persona across all interactions
- Learns from high-performing content over time

Chimera does not build a single influencer. It builds the factory that produces and operates thousands of them. Each with their own identity, niche, memory and wallet.

---

## 2. Business Objective

Most AI content projects fail because they rely on fragile prompts and messy codebases. When scaled, they hallucinate or break.

Chimera solves this by:
- Using Spec-Driven Development as the source of truth
- Using Java 21+ Virtual Threads for enterprise-grade concurrency
- Using a Hierarchical Swarm architecture where every layer has a quality gate
- Using MCP as the universal interface for all external interactions
- Using Human-in-the-Loop checkpoints at every high-risk decision point

---

# 3. Goals

### 3.1 Primary Goals
- G-001: Operate thousands of AI influencers simultaneously without human intervention
- G-002: Generate multimodal content(text, image, video) autonomously per campaign
- G-003: Engage with social media audiences 24/7 via real-time comment replies
- G-004: Maintain strict persona consistency across every post and interaction
- G-005: Participate in on-chain commerce autonomously via Coinbase AgentKit
- G-006: Escalate sensitive, legal, or high-risk content to humans immediately
- G-007: Learn from high-engagement interactions and update long-term memory accordingly
- G-008: Support multi-tenancy. Data and memory of one influencer never accessible to another

### 3.2 Secondary Goals
- G-009: Support a Digital Talent Agency model (Chimera operates its own influencers)
- G-010: Support a PaaS model (external brands lease the infrastructure)
- G-011: Integrate with OpenClaw, so Chimera agents can discover and collaborate with external agents

---

## 4. Non-Goals

These are things Chimera explicitly does NOT do. Any agent that attempts these violates the spec.

- NG-001: Chimera is NOT a human social media management tool. Humans govern. They do not operate the system.

- NG-002: Chimera is NOT a general-purpose chatbot. Every agent has a specific role and must not act outside it.

- NG-003: Direct social media API calls from agent core logic are STRICTLY PROHIBITED. All calls must go through the MCP layer.

- NG-004: Private keys and seed phrases are NEVER stored in any database, log file, or code. Only injected via secrets manager at runtime.

- NG-005: Agents NEVER post, reply or execute transactions without Judge approval.

- NG-006: SOUL.md is NEVER modified at runtime. It is the immutable DNA of the influencer.

- NG-007: Chimera does NOT build the OpenClaw network. It integrates with it.

- NG-008: Workers NEVER communicate directly with each other. Shared-nothing architecture.

- NG-009: The system NEVER auto-approves content flagged as sensitive regardless of confidence score.

---

## 5. Constraints

These are non-negotiable technical rules. Every implementation decision must respect them.

### 5.1 Language and Framework
- C-001: Java 21+ is the required language
- C-002: Spring Boot is the required framework
- C-003: Java Records MUST be used for all immutable DTOs passing between agents(AgentTask, WorkerResult, JudgeVerdict etc.)
- C-004: No mutable POJOs. No raw Maps for agent payloads.
- C-005: JUnit 5 MUST be used for all tests
- C-006: Maven or Gradle as build tool

### 5.2 Agent Communication
- C-007: All inter-agent communication uses the A2A (Agent-to-Agent) protocol
- C-008: All agent-to-tool communication uses MCP (Model Context Protocol)
- C-009: All external API calls are strictly prohibited from agent core logic. MCP layer only.

### 5.3 Concurrency
- C-010: Java 21+ Virtual Threads MUST be used for parallel agent execution (Executors.newVirtualThreadPerTaskExecutor())
- C-011: Workers are stateless and ephemeral. They spin up for one task and spin down.
- C-012: Workers share nothing with each other.

### 5.4 Memory and State
- C-013: GlobalState is stored in Redis and versioned with state_version
- C-014: OCC (Optimistic Concurrency Control) MUST be implemented by every Judge before committing any result
- C-015: Short-term memory window is 1 hour stored in Redis (TTL: 3600 seconds)
- C-016: Long-term memory is stored in Weaviate using semantic vector search

### 5.5 Security
- C-017: Private keys injected via AWS Secrets Manager or HashiCorp Vault at startup only
- C-018: Private keys NEVER appear in logs, outputs, or error messages
- C-019: Tenant data isolation is mandatory. Every Redis key and Weaviate collection is namespaced by influencer_id

### 5.6 Human-in-the-Loop
- C-020: Three mandatory human touchpoints:
  > Touchpoint 0: Campaign intake
  > Touchpoint 1: Pre-compiler review
  > Touchpoint 2: Final approval
- C-021: Sensitive content ALWAYS escalates to human. Confidence score is ignored
- C-022: Judge must respond within 5 seconds for engagement replies (timeliness)

### 5.7 Budget
- C-023: get_balance MUST be called before every cost-incurring workflow
- C-024: Daily spend limit enforced by CFO Agent (default: $50 USDC/day)
- C-025: Every financial transaction logged to PostgreSQL AND on-chain

---

## 6. System Architecture Summary

### 6.1 Agent Pattern
Hierarchical Swarm (FastRender Pattern)

Three core roles per campaign thread:
- Planner: reads GlobalState, generates DAG, pushes tasks to TaskQueue
- Worker: pops one task, executes, attaches confidence_score, pushes to ReviewQueue
- Judge: polls ReviewQueue, validates output, commits to GlobalState or re-queues

### 6.2 Topology
Hub-and-Spoke:
- Master Orchestrator (hub)
- Campaign Orchestrators (spokes)
- Campaign Managers (operational layer)
- Planner/Worker/Judge threads (execution layer)
- Engagement Agents (always-on, independent)

### 6.3 External Integration
All external integrations via MCP only:
- Social platforms via MCP Tools
- Content generation via MCP Tools
- Commerce via Coinbase AgentKit MCP
- News and trend data via MCP Resources

---

## 7. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21+ |
| Framework | Spring Boot |
| Agent Protocol | A2A (Agent-to-Agent) |
| External Tools | MCP (Model Context Protocol) |
| Content Pipeline State | Redis |
| Video and Content Metadata | MongoDB |
| Long-Term Memory | Weaviate |
| Structured Data | PostgreSQL |
| Financial Ledger | Base / Ethereum |
| AI Reasoning | Claude Opus / Gemini Pro |
| AI Routine Tasks | Claude Haiku / Gemini Flash |
| Containers | Docker + Kubernetes |
| Cloud | AWS / GCP Hybrid |
| Secrets | AWS Secrets Manager / HashiCorp Vault |
| CI/CD | GitHub Actions |

---

## 8. Open Questions

These are unresolved decisions that must be answered before implementation begins. No code is written for these areas until the question is resolved.

- OQ-001: Does SOUL.md reload mid-campaign if it is updated by an operator Or does the running agent finish with the version it loaded at startup?

- OQ-002: What is the exact formula for deriving confidence_score from the LLM? Is it the raw token probability or a post-processed quality score?


- OQ-003: How does the system handle social media platform API changing its rate limits mid-campaign?

- OQ-004: Who defines the keyword list for sensitive topic detection?How is it updated over time?

- OQ-005: What happens to the HITL queue if a human reviewer is unavailable for more than 24 hours?

---