# Technical Specification
*Document:** `technical.md`

---
## Overview

This document defines the technical contracts that govern Project Chimera. It contains two sections:

1. **API Contracts** - The exact JSON inputs and outputs every agent must honour. These are binding contracts. Any agent that breaks them breaks the pipeline.

2. **Database Schema** - The structure of every database and collection used by the system, including the ERD for video metadata storage.

**Rule:** All agent implementations MUST conform to these contracts exactly. No deviation without a spec update first.

---
## API Contracts

## CONTRACT-001: Persona Loading
**Agent:** Cognitive Core
**Linked Story:** User_Story-001
**Direction:** Input -> Output

**Input:**
```json
{
  "influencer_id": "string (required)",
  "soul_file_path": "string (required)"
}
```

**Output (SUCCESS):**
```json
{
  "influencer_id": "string",
  "name": "string",
  "backstory": "string",
  "voice_traits": ["string"],
  "core_beliefs": ["string"],
  "directives": ["string"],
  "loaded_at": "ISO8601 timestamp",
  "status": "LOADED"
}
```

**Output (FAILURE):**
```json
{
  "influencer_id": "string",
  "status": "FAILED",
  "error": "PersonaLoadException",
  "reason": "string",
  "missing_fields": ["string"]
}
```

---
### CONTRACT-002: Context Assembly
**Agent:** Cognitive Core
**Linked Story:** User_Story-002
**Direction:** Input -> Output

**Input:**
```json
{
  "influencer_id": "string (required)",
  "current_context_input": "string (required)",
  "soul_file_path": "string (required)"
}
```

**Output (SUCCESS):**
```json
{
  "system_prompt": {
    "who_you_are": {
      "name": "string",
      "voice": ["string"],
      "directives": ["string"],
      "backstory": "string"
    },
    "recent_activity": ["string"],
    "long_term_memories": [
      {
        "content": "string",
        "relevance_score": "float (0.0-1.0)"
      }
    ]
  },
  "assembled_at": "ISO8601 timestamp",
  "token_count": "integer",
  "truncated": "boolean"
}
```

---

### CONTRACT-003: Persona Evolution Trigger
**Agent:** Cognitive Core
**Linked Story:** User_Story-003
**Direction:** Input -> Output

**Input:**
```json
{
  "interaction_id": "string (required)",
  "influencer_id": "string (required)",
  "engagement_score": "float (required)",
  "interaction_type": "string"
}
```
**Output (Success):**
```json
{
  "trigger_fired": true,
  "memory_writer_job_id": "string",
  "reason": "string",
  "engagement_score": "float"
}
```

**Output (Failed):**
```json
{
  "trigger_fired": false,
  "reason": "below_threshold"
}
```

---
### CONTRACT-004: Memory Writing
**Agent:** Cognitive Core
**Linked Story:** User_Story-003
**Direction:** Input -> Output

**Input:**
```json
{
  "influencer_id": "string (required)",
  "interaction_id": "string (required)",
  "content": "string (required)",
  "engagement_score": "float (required)",
  "interaction_type": "string"
}
```


**Output (SUCCESS):**
```json
{
  "memory_id": "string",
  "influencer_id": "string",
  "summary": "string",
  "memory_type": "LEARNED_BEHAVIOUR",
  "engagement_score": "float",
  "written_to": "weaviate",
  "created_at": "ISO8601 timestamp"
}
```

**Output (FAILURE):**
```json
{
  "status": "FAILED",
  "error": "string",
  "retry_count": "integer"
}

```

---

### CONTRACT-005: Active Resource Monitoring
**Agent:** Perception Agent
**Linked Story:** User_Story-005
**Direction:** Input -> Output

**Input:**
```json
{
  "influencer_id": "string (required)",
  "resources": [
    {
      "uri": "string (e.g. twitter://mentions/recent)",
      "type": "MENTION | NEWS | MARKET"
    }
  ],
  "polling_interval_seconds": "integer (default: 60)"
}
```

**Output (content passes filter):**
```json
{
  "influencer_id": "string",
  "source": "string (resource URI)",
  "content": "string",
  "relevance_score": "float (0.0-1.0)",
  "action": "CREATE_PLANNER_TASK",
  "received_at": "ISO8601 timestamp"
}
```

**Output (content filtered out):**
```json
{
  "influencer_id": "string",
  "content": "string",
  "relevance_score": "float",
  "action": "DISCARDED",
  "reason": "below_threshold",
  "threshold": 0.75
}
```

---

### CONTRACT-006: Trend Alert
**Agent:** Trend Spotter Worker
**Linked Story:** User_Story-007
**Direction:** Output -> GlobalState

**Output:**
```json
{
  "alert_type": "TREND_DETECTED",
  "influencer_id": "string",
  "topic": "string",
  "related_headlines": ["string"],
  "confidence": "float (0.0-1.0)",
  "summary": "string",
  "recommended_action": "CREATE_CONTENT | MONITOR | IGNORE",
  "detected_at": "ISO8601 timestamp"
}
```

---

### CONTRACT-007: Content Generation
**Agent:** Creative Worker
**Linked Stories:** User_Story-008, User_Story-009, User_Story-010
**Direction:** Input -> Output

**Input:**
```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "creative_brief_id": "string (required)",
  "soul_file_path": "string (required)",
  "campaign_id": "string (required)",
  "state_version": "integer (required)",
  "required_components": [
    "caption | image | video | hashtags"
  ],
  "platform": "instagram | twitter | tiktok",
  "character_reference_id": "string (required for image/video)",
  "estimated_cost_usdc": "float (required)"
}
```

**Output (SUCCESS):**
```json
{
  "task_id": "string",
  "state_version": "integer",
  "components": {
    "caption": "string (optional)",
    "caption_character_count": "integer",
    "image_url": "string (optional)",
    "video_url": "string (optional)",
    "hashtags": ["string (optional)"]
  },
  "character_reference_used": "string",
  "tool_used": "mcp-server-ideogram | mcp-server-midjourney",
  "estimated_cost_usdc": "float",
  "actual_cost_usdc": "float",
  "voice_validated": "boolean",
  "dont_do_validated": "boolean",
  "confidence_score": "float (0.0-1.0)",
  "status": "COMPLETED",
  "awaiting_judge_approval": true
}
```

**Output (FAILURE):**
```json
{
  "task_id": "string",
  "status": "FAILED",
  "error": "MissingCharacterReferenceException
           | GenerationFailedException
           | BothToolsFailedException",
  "retry_count": "integer",
  "action": "RETRY | ESCALATE"
}
```
---

### CONTRACT-008: Image Consistency Validation
**Agent:** Judge
**Linked Story:** User_Story-011
**Direction:** Input -> Output

**Input:**
```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "generated_image_url": "string (required)",
  "reference_image_url": "string (required)",
  "character_reference_id": "string (required)"
}
```

**Output (APPROVED):**
```json
{
  "task_id": "string",
  "verdict": "APPROVED",
  "consistent": true,
  "llm_response": "YES",
  "validated_at": "ISO8601 timestamp"
}
```

**Output (REJECTED):**
```json
{
  "task_id": "string",
  "verdict": "REJECTED",
  "consistent": false,
  "llm_response": "NO",
  "error": "ValidationError",
  "action": "RETRY_IMAGE_GENERATION",
  "retry_count": "integer"
}
```

---

### CONTRACT-009: Platform-Agnostic Publishing
**Agent:** Action System(Social Interface)
**Linked Story:** User_Story-012
**Direction:** Input -> Output

**Input:**
```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "action": "post_tweet | publish_media | reply_tweet | like_tweet",
  "platform": "twitter | instagram | tiktok",
  "content": {
    "caption": "string (optional)",
    "image_url": "string (optional)",
    "video_url": "string (optional)",
    "hashtags": ["string (optional)"]
  },
  "dry_run": "boolean (default: false)"
}
```

**Output (SUCCESS):**
```json
{
  "task_id": "string",
  "status": "PUBLISHED",
  "platform": "string",
  "post_id": "string",
  "published_at": "ISO8601 timestamp",
  "dry_run": "boolean"
}
```

**Output (FAILURE):**
```json
{
  "task_id": "string",
  "status": "FAILED",
  "error": "string",
  "action": "RETRY | QUEUE"
}
```
---

### CONTRACT-010: Comment Ingestion and Reply Planning
**Agent:** Action System(Social Interface)
**Linked Story:** User_Story-013
**Direction:** Input -> Output

**Input:**
```json
{
  "influencer_id": "string (required)",
  "source": "string (MCP resource URI)",
  "comment": {
    "id": "string",
    "content": "string",
    "commenter_id": "string",
    "received_at": "ISO8601 timestamp"
  }
}
```

**Output (TASK CREATED):**
```json
{
  "task_id": "string",
  "task_type": "GENERATE_REPLY",
  "priority": "P1 | P2 | P3",
  "influencer_id": "string",
  "platform": "string",
  "comment": "object",
  "pushed_to_queue": true,
  "created_at": "ISO8601 timestamp"
}
```

**Output (DISCARDED):**
```json
{
  "comment_id": "string",
  "action": "DISCARDED",
  "reason": "P4_SPAM | below_relevance_threshold"
}
```
---
### CONTRACT-011: Reply Generation
**Agent:** Action System(Social Interface)
**Linked Story:** User_Story-014
**Direction:** Input → Output

**Input:**
```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "platform": "twitter | instagram | tiktok",
  "comment": {
    "id": "string",
    "content": "string",
    "commenter_id": "string"
  },
  "priority": "P1 | P2 | P3",
  "state_version": "integer"
}
```

**Output:**
```json
{
  "task_id": "string",
  "state_version": "integer",
  "reply_draft": "string",
  "character_count": "integer",
  "platform": "string",
  "confidence_score": "float (0.0-1.0)",
  "status": "COMPLETED",
  "tool_ready_to_fire": "string",
  "awaiting_judge_approval": true
}
```
---

### CONTRACT-012: Pre-Publication Safety Gate
**Agent:** Action System(Social Interface)
**Linked Story:** User_Story-015
**Direction:** Input -> Output

**Input:**
```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "reply_draft": "string (required)",
  "platform": "string",
  "tool_pending": "string"
}
```

**Output (APPROVED):**
```json
{
  "task_id": "string",
  "verdict": "APPROVED",
  "tool_execution": "RESUME",
  "reason": "string",
  "approved_at": "ISO8601 timestamp"
}
```

**Output (REJECTED):**
```json
{
  "task_id": "string",
  "verdict": "REJECTED",
  "tool_execution": "CANCELLED",
  "reason": "string",
  "action": "REGENERATE",
  "retry_count": "integer"
}
```

**Output (ESCALATED):**
```json
{
  "task_id": "string",
  "verdict": "ESCALATED",
  "tool_execution": "CANCELLED",
  "reason": "string",
  "action": "HUMAN_REVIEW_REQUIRED",
  "human_notified_at": "ISO8601 timestamp"
}
```
---

### CONTRACT-013: Wallet Initialisation
**Agent:** Agentic Commerce
**Linked Story:** User_Story-016
**Direction:** Input -> Output

**Input:**
```json
{
  "influencer_id": "string (required)",
  "secret_key_name": "string (required)",
  "network": "base | ethereum"
}
```

**Output (SUCCESS):**
```json
{
  "influencer_id": "string",
  "wallet_address": "string",
  "network": "string",
  "status": "READY",
  "initialised_at": "ISO8601 timestamp",
  "private_key_logged": false
}
```

**Output (FAILURE):**
```json
{
  "status": "FAILED",
  "error": "WalletInitialisationException",
  "reason": "string",
  "action": "HALT_AND_ALERT_HUMAN"
}

```

### CONTRACT-014: Pre-Workflow Balance Check
**Agent:** Agentic Commerce
**Linked Story:** User_Story-017, User_Story-018
**Direction:** Input -> Output 

**Input:**
```json
{
  "influencer_id": "string (required)",
  "workflow_id": "string (required)",
  "estimated_cost_usdc": "float (required)"
}
```

**Output (SUFFICIENT):**
```json
{
  "wallet_balance_usdc": "float",
  "daily_spend_usdc": "float",
  "daily_limit_usdc": "float",
  "estimated_cost_usdc": "float",
  "remaining_daily_budget": "float",
  "decision": "APPROVED",
  "proceed": true
}
```

**Output (INSUFFICIENT):**
```json
{
  "wallet_balance_usdc": "float",
  "estimated_cost_usdc": "float",
  "decision": "REJECTED",
  "proceed": false,
  "reason": "string",
  "action": "HALT_WORKFLOW"
}
```

### CONTRACT-015: CFO Budget Check
**Agent:** Agentic Commerce
**Linked Story:** User_Story-19
**Direction:** Input -> Output

**Input:**
```json
{
  "transaction_id": "string (required)",
  "influencer_id": "string (required)",
  "action": "native_transfer | deploy_token",
  "amount_usdc": "float (required)",
  "to_address": "string (required)",
  "daily_spend_so_far": "float",
  "daily_limit_usdc": "float"
}
```

**Output (APPROVED):**
```json
{
  "transaction_id": "string",
  "verdict": "APPROVED",
  "daily_spend_after_usdc": "float",
  "daily_limit_usdc": "float",
  "budget_remaining_usdc": "float",
  "approved_at": "ISO8601 timestamp"
}
```

**Output (REJECTED):**
```json
{
  "transaction_id": "string",
  "verdict": "REJECTED",
  "error": "BudgetExceededError | AnomalyDetected",
  "reason": "string",
  "action": "HUMAN_REVIEW_REQUIRED",
  "human_notified": true,
  "flagged_at": "ISO8601 timestamp"
}
```

---

### CONTRACT-016: Planner Task Generation
**Agent:** Orchestration and Swarm Governance
**Linked Story:** User_Story-20
**Direction:** Input -> Output

**Input:**
```json

{
  "campaign_id": "string (required)",
  "influencer_id": "string (required)",
  "global_state": {
    "goals": ["string"],
    "budget_remaining_usdc": "float",
    "state_version": "integer"
  }
}
```

**Output:**
```json

{
  "campaign_id": "string",
  "tasks_generated": "integer",
  "tasks": [
    {
      "task_id": "string",
      "type": "string",
      "depends_on": ["string"],
      "state_version": "integer",
      "estimated_cost_usdc": "float"
    }
  ],
  "pushed_to_queue": true,
  "generated_at": "ISO8601 timestamp"
}
```

## CONTRACT-017: Judge Confidence Routing
**Agent:** Orchestration and Swarm Governance
**Linked Story:** User_Story-22
**Direction:** Input → Output

**Input:**
```json
{
  "task_id": "string (required)",
  "result": "object (required)",
  "confidence_score": "float (required)",
  "state_version": "integer (required)",
  "sensitive_check": "CLEAR | TRIGGERED"
}
```

**Output (HIGH - auto approved):**
```json
{
  "task_id": "string",
  "verdict": "AUTO_APPROVED",
  "confidence_score": "float",
  "tier": "HIGH",
  "committed_to_global_state": true,
  "approved_at": "ISO8601 timestamp"
}
```

**Output (MEDIUM - async):**
```json
{
  "task_id": "string",
  "verdict": "PENDING_HUMAN",
  "confidence_score": "float",
  "tier": "MEDIUM",
  "added_to_dashboard_queue": true,
  "other_tasks_continue": true
}
```

**Output (LOW - rejected):**
```json
{
  "task_id": "string",
  "verdict": "REJECTED",
  "confidence_score": "float",
  "tier": "LOW",
  "action": "RETRY",
  "retry_count": "integer",
  "max_retries": 3
}
```

---

### CONTRACT-018: Sensitive Topic Detection
**Agent: Orchestration and Swarm Governance** 
**Linked Story: User_Story-23** 
**Direction:** Input -> Output

**Input:**
```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "output_content": "string (required)",
  "confidence_score": "float (required)",
  "output_type": "caption | reply | image_prompt
                 | video_script | hashtags"
}
```

**Output (SENSITIVE DETECTED — keyword):**
```json
{
  "task_id": "string",
  "sensitive_check": "TRIGGERED",
  "detection_method": "KEYWORD",
  "category": "POLITICS | HEALTH_ADVICE
              | FINANCIAL_ADVICE | LEGAL_CLAIM",
  "matched_keyword": "string",
  "confidence_score_ignored": true,
  "verdict": "MANDATORY_HUMAN_REVIEW",
  "added_to_hitl_queue": true,
  "auto_approve_blocked": true,
  "detected_at": "ISO8601 timestamp"
}
```

**Output (SENSITIVE DETECTED — semantic):**
```json
{
  "task_id": "string",
  "sensitive_check": "TRIGGERED",
  "detection_method": "SEMANTIC_CLASSIFICATION",
  "llm_used": "Gemini Flash | Claude Haiku",
  "llm_response": "YES",
  "category": "POLITICS | HEALTH_ADVICE
              | FINANCIAL_ADVICE | LEGAL_CLAIM",
  "confidence_score_ignored": true,
  "verdict": "MANDATORY_HUMAN_REVIEW",
  "added_to_hitl_queue": true,
  "auto_approve_blocked": true,
  "detected_at": "ISO8601 timestamp"
}
```

**Output (CLEAR — proceed to confidence routing):**
```json
{
  "task_id": "string",
  "sensitive_check": "CLEAR",
  "detection_method": "KEYWORD_THEN_SEMANTIC",
  "keyword_check": "PASSED",
  "semantic_check": "PASSED",
  "llm_response": "NO",
  "proceed_to_confidence_routing": true
}
```

