# Skill: skill_fetch_trends
**Skill ID:** SKILL-002
**Linked Story:** User_Story-007


---

## What This Skill Does

Fetches trending news headlines from external sources, clusters them semantically, and returns a Trend Alert to the Planner if a relevant cluster is detected.

This skill runs as a background process every 4 hours per influencer. It never blocks the main content pipeline.

---

## When This Skill Is Called

```
Trend Spotter Worker -> Every 4 hours (background process) -> skill_fetch_trends -> Returns TrendAlert -> injected into GlobalState by Planner
```

---

## Input Contract

```json
{
  "influencer_id": "string (required)",
  "niche": "string (required)",
  "region": "string (required)",
  "timeframe_hours": "integer (default: 4)",
  "cluster_threshold": "integer (default: 3)"
}
```
---

## Output Contract

**Output (TREND DETECTED):**
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

**Output (NO TREND):**
```json
{
  "skill_id": "SKILL-001",
  "influencer_id": "string",
  "status": "NO_TREND_DETECTED",
  "headlines_collected": "integer",
  "reason": "below_cluster_threshold",
  "next_run_at": "ISO8601 timestamp"
}
```

**Output (ERROR):**
```json
{
  "skill_id": "SKILL-001",
  "influencer_id": "string",
  "status": "FAILED",
  "error": "NewsSourceUnavailable
           | RedisUnavailable
           | LLMTimeout",
  "retry_count": "integer",
  "action": "RETRY | SKIP_WINDOW"
}
```

---

## Internal Execution Steps

```
Step 1: Fetch headlines
  > Tool: mcp-server-news
  > Filter by niche and region
  > Store in Redis with TTL = 4 hours

Step 2: Check cluster
  > Count semantically related headlines
  > If count < cluster_threshold: 
      return NO_TREND_DETECTED

Step 3: Semantic clustering (if threshold met)
  > Tool: LLM (Gemini Flash / Claude Haiku)
  > NEVER use expensive model here
  > Prompt: "Group these headlines into
    topic clusters. Return the largest
    cluster and a one-sentence summary."

Step 4: Generate Trend Alert
  > Populate output contract
  > Return to Trend Spotter Worker
```

---

## Tools Used Internally

| Tool | Purpose |
|---|---|
| `mcp-server-news` | Fetch raw headlines |
| `mcp-server-redis` | Cache headlines during 4h window |
| `Gemini Flash / Claude Haiku` | Semantic cluster detection |


---

## Rules

- NEVER use Gemini Pro or Claude Opus here.
  Cost must be minimal.
- NEVER block the main pipeline. Always runs as background process.
- NEVER generate content in this skill. It only detects. It does not create.
- If news source is down: skip this window, log error, schedule next run normally.

---

## Failure Handling

| Failure | Response |
|---|---|
| News source unavailable | Skip window, log, retry next cycle |
| Redis unavailable | Abort, alert human |
| LLM timeout (>10s) | Retry once, then skip window |
| No cluster found | Log, reset Redis, wait for next window |