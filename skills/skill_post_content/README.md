# Skill: skill_post_content
**Skill ID:** SKILL-001
**Linked Story:** User_Story-012


---

## What This Skill Does

Publishes approved content to social media platforms exclusively through MCP Tools. No direct API calls. Ever.

This skill is the ONLY way content leaves the Chimera system and reaches
a social platform. It is the final execution step after Judge approval.

---

## When This Skill Is Called

```
Judge -> AUTO_APPROVED or HUMAN_APPROVED -> skill_post_content -> MCP Tool fires -> Post live on platform
```

---

## Input Contract

```json
{
  "task_id": "string (required)",
  "influencer_id": "string (required)",
  "platform": "instagram | twitter | tiktok",
  "action": "post_tweet | publish_media",
  "content": {
    "caption": "string (optional)",
    "image_url": "string (optional)",
    "video_url": "string (optional)",
    "hashtags": ["string (optional)"]
  },
  
  "dry_run": "boolean (default: false)",
  "campaign_id": "string (required)"
}
```

---

## Output Contract

**Output (SUCCESS):**
```json
{
  "skill_id": "SKILL-001",
  "task_id": "string",
  "status": "PUBLISHED",
  "platform": "string",
  "post_id": "string",
  "post_url": "string",
  "published_at": "ISO8601 timestamp",
  "dry_run": false
}
```

**Output (DRY RUN):**
```json
{
  "skill_id": "SKILL-003",
  "task_id": "string",
  "status": "DRY_RUN_SIMULATED",
  "platform": "string",
  "dry_run": true,
  "note": "Post was NOT sent. Simulation only."
}
```

**Output (FAILURE):**
```json
{
  "skill_id": "SKILL-003",
  "task_id": "string",
  "status": "FAILED",
  "error": "MCPToolUnavailable | RateLimitExceeded
           | InvalidContent",
  "retry_count": "integer",
  "action": "QUEUE_AND_RETRY"
}
```

---

## Internal Execution Steps

```
Step 1: Verify Judge verdict
  > Must be AUTO_APPROVED or HUMAN_APPROVED
  > If verdict missing → abort immediately

Step 2: Check dry_run flag
  > If dry_run: true -> simulate, never send

Step 3: Route to correct MCP Tool
  > twitter -> mcp-server-twitter:post_tweet
  > instagram -> mcp-server-instagram:publish_media
  > tiktok -> mcp-server-tiktok:upload_video

Step 4: Execute via MCP only
  > Never call platform API directly

Step 5: Log result
  > Write post_id and post_url to MongoDB
  > Update campaign status in PostgreSQL
```

---

## Tools Used Internally

| Tool | Platform | Action |
|---|---|---|
| `mcp-server-twitter` | Twitter/X | post_tweet |
| `mcp-server-instagram` | Instagram | publish_media |
| `mcp-server-tiktok` | TikTok | upload_video |

---

## Rules

- Direct social media API calls are STRICTLY PROHIBITED
- dry_run mode MUST be used for all testing
- Never publish without a confirmed Judge verdict
- If MCP Tool is unavailable: queue, retry after 60s
- Never drop a task silently — always log failure

---

## Failure Handling

| Failure | Response |
|---|---|
| MCP Tool unavailable | Queue task, retry after 60s |
| Rate limit exceeded | Queue task, retry after platform cooldown |
| Missing Judge verdict | Abort, escalate to human |
| Invalid content format | Abort, return to Creative Worker |