package com.chimera.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests: Skills Interface
 *
 * Covers TWO skills exactly as defined in the skills/ directory:
 *
 * SKILL-001: skill_post_content  → User_Story-012 → FR 4.0
 * SKILL-002: skill_fetch_trends  → User_Story-007 → FR 2.2
 *
 * STATUS: ALL TESTS MUST FAIL UNTIL IMPLEMENTATION IS COMPLETE.
 * These tests are the goal posts. The AI agent fills the empty slots.
 *
 * Run: mvn test -Dtest=skillsInterfaceTest
 */
@DisplayName("Skills Interface — SKILL-001 and SKILL-002 Contract Tests")
class skillsInterfaceTest {

    // ─────────────────────────────────────────────
    // CUSTOM EXCEPTIONS
    // All must be implemented before tests pass
    // ─────────────────────────────────────────────

    static class MissingJudgeVerdictException extends RuntimeException {
        public MissingJudgeVerdictException(String message) {
            super(message);
        }
    }

    static class MCPToolUnavailableException extends RuntimeException {
        private final String toolName;
        private final String platform;
        public MCPToolUnavailableException(String message, String toolName, String platform) {
            super(message);
            this.toolName = toolName;
            this.platform = platform;
        }
        public String getToolName() { return toolName; }
        public String getPlatform() { return platform; }
    }

    static class BudgetExceededException extends RuntimeException {
        private final double requestedAmount;
        private final double dailyLimit;
        public BudgetExceededException(String message, double requestedAmount, double dailyLimit) {
            super(message);
            this.requestedAmount = requestedAmount;
            this.dailyLimit = dailyLimit;
        }
        public double getRequestedAmount() { return requestedAmount; }
        public double getDailyLimit() { return dailyLimit; }
    }

    static class DirectApiCallException extends RuntimeException {
        public DirectApiCallException(String message) {
            super(message);
        }
    }

    

    record PostContent(
        String caption,     // optional
        String imageUrl,    // optional
        String videoUrl,    // optional
        java.util.List<String> hashtags  // optional
    ) {}

    record PostContentRequest(
        String taskId,          // required
        String influencerId,    // required
        String platform,        // instagram | twitter | tiktok
        String action,          // post_tweet | publish_media
        PostContent content,
        String judgeVerdict,    // AUTO_APPROVED | HUMAN_APPROVED
        boolean dryRun,         // default: false
        String campaignId       // required
    ) {}

    record PostContentSuccess(
        String skillId,         // "SKILL-001"
        String taskId,
        String status,          // "PUBLISHED"
        String platform,
        String postId,
        String postUrl,
        String publishedAt,     // ISO8601
        boolean dryRun
    ) {}

    record PostContentDryRun(
        String skillId,         // "SKILL-001"
        String taskId,
        String status,          // "DRY_RUN_SIMULATED"
        String platform,
        boolean dryRun,
        String note
    ) {}

    record PostContentFailure(
        String skillId,         
        String taskId,
        String status,          
        String error,           
        int retryCount,
        String action           
    ) {}

    

    interface PostContentSkill {
        PostContentSuccess publish(PostContentRequest request);
        PostContentDryRun simulateDryRun(PostContentRequest request);
    }

    

    @Test
    @DisplayName("SKILL-001 INPUT: Must reject null task_id")
    void postContent_nullTaskId_throwsException() {
        // FAILS: validation not implemented
        assertThrows(IllegalArgumentException.class, () ->
            new PostContentRequest(
                null,               // task_id required
                "influencer-001",
                "instagram",
                "publish_media",
                new PostContent("Great post!", null, null, null),
                "AUTO_APPROVED",
                false,
                "campaign-001"
            ),
            "task_id is required per skill input contract"
        );
    }

    @Test
    @DisplayName("SKILL-001 INPUT: Must reject null influencer_id")
    void postContent_nullInfluencerId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new PostContentRequest(
                "task-001",
                null,               // influencer_id required
                "instagram",
                "publish_media",
                new PostContent("Great post!", null, null, null),
                "AUTO_APPROVED",
                false,
                "campaign-001"
            )
        );
    }

    @Test
    @DisplayName("SKILL-001 INPUT: Must reject invalid platform")
    void postContent_invalidPlatform_throwsException() {
        // FAILS: platform enum validation not implemented
        assertThrows(IllegalArgumentException.class, () ->
            new PostContentRequest(
                "task-001",
                "influencer-001",
                "facebook",         // invalid — must be instagram|twitter|tiktok
                "publish_media",
                new PostContent("Great post!", null, null, null),
                "AUTO_APPROVED",
                false,
                "campaign-001"
            ),
            "platform must be instagram | twitter | tiktok"
        );
    }

    @Test
    @DisplayName("SKILL-001 INPUT: Must reject null campaign_id")
    void postContent_nullCampaignId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new PostContentRequest(
                "task-001",
                "influencer-001",
                "instagram",
                "publish_media",
                new PostContent("Great post!", null, null, null),
                "AUTO_APPROVED",
                false,
                null                // campaign_id required
            )
        );
    }

    

    @Test
    @DisplayName("SKILL-001 RULE: Must throw MissingJudgeVerdictException when judge_verdict is null")
    void postContent_missingJudgeVerdict_throwsException() {
        // FAILS: MissingJudgeVerdictException not implemented
        // Rule: "Never publish without a confirmed Judge verdict"
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001",
            "influencer-001",
            "instagram",
            "publish_media",
            new PostContent("Great post!", "https://image.url", null, null),
            null,           // judge_verdict missing — must throw
            false,
            "campaign-001"
        );

        assertThrows(MissingJudgeVerdictException.class,
            () -> skill.publish(request),
            "Must throw MissingJudgeVerdictException — never publish without Judge approval"
        );
    }

    @Test
    @DisplayName("SKILL-001 RULE: Must throw MissingJudgeVerdictException for invalid verdict value")
    void postContent_invalidJudgeVerdict_throwsException() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Great post!", null, null, null),
            "SELF_APPROVED",    // invalid — only AUTO_APPROVED | HUMAN_APPROVED
            false, "campaign-001"
        );

        assertThrows(MissingJudgeVerdictException.class,
            () -> skill.publish(request)
        );
    }

   

    @Test
    @DisplayName("SKILL-001 OUTPUT: skill_id must be SKILL-001")
    void postContent_success_correctSkillId() {
        
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Great post!", "https://image.url", null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertEquals("SKILL-001", result.skillId(),
            "skill_id must be SKILL-001 in success output");
    }

    @Test
    @DisplayName("SKILL-001 OUTPUT: status must be PUBLISHED")
    void postContent_success_statusPublished() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "twitter", "post_tweet",
            new PostContent("My tweet!", null, null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertEquals("PUBLISHED", result.status());
    }

    @Test
    @DisplayName("SKILL-001 OUTPUT: post_id must not be null or blank")
    void postContent_success_postIdNotBlank() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Post!", "https://img.url", null, null),
            "HUMAN_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertNotNull(result.postId());
        assertFalse(result.postId().isBlank(),
            "post_id must be returned by the platform after publishing");
    }

    @Test
    @DisplayName("SKILL-001 OUTPUT: post_url must not be null or blank")
    void postContent_success_postUrlNotBlank() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Post!", "https://img.url", null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertNotNull(result.postUrl());
        assertFalse(result.postUrl().isBlank());
    }

    @Test
    @DisplayName("SKILL-001 OUTPUT: published_at must be valid ISO8601 timestamp")
    void postContent_success_publishedAtISO8601() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "twitter", "post_tweet",
            new PostContent("Tweet!", null, null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertNotNull(result.publishedAt());
        assertDoesNotThrow(() -> java.time.Instant.parse(result.publishedAt()),
            "published_at must be valid ISO8601");
    }

    @Test
    @DisplayName("SKILL-001 OUTPUT: dry_run must be false on real publish")
    void postContent_success_dryRunFalse() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Post!", null, null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertFalse(result.dryRun(),
            "dry_run must be false when actually publishing");
    }

    

    @Test
    @DisplayName("SKILL-001 DRY RUN: skill_id must be SKILL-001")
    void postContent_dryRun_correctSkillId() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Test post!", null, null, null),
            "AUTO_APPROVED",
            true,       // dry_run = true
            "campaign-001"
        );

        PostContentDryRun result = skill.simulateDryRun(request);
        assertEquals("SKILL-001", result.skillId(),
            "skill_id must be SKILL-001 — not SKILL-003 (spec inconsistency to fix)");
    }

    @Test
    @DisplayName("SKILL-001 DRY RUN: status must be DRY_RUN_SIMULATED")
    void postContent_dryRun_correctStatus() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "twitter", "post_tweet",
            new PostContent("Test!", null, null, null),
            "AUTO_APPROVED", true, "campaign-001"
        );

        PostContentDryRun result = skill.simulateDryRun(request);
        assertEquals("DRY_RUN_SIMULATED", result.status());
    }

    @Test
    @DisplayName("SKILL-001 DRY RUN: dry_run must be true")
    void postContent_dryRun_flagIsTrue() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "tiktok", "upload_video",
            new PostContent(null, null, "https://video.url", null),
            "AUTO_APPROVED", true, "campaign-001"
        );

        PostContentDryRun result = skill.simulateDryRun(request);
        assertTrue(result.dryRun(),
            "dry_run flag must be true in dry run output");
    }

    @Test
    @DisplayName("SKILL-001 DRY RUN: note must state post was NOT sent")
    void postContent_dryRun_noteConfirmsNotSent() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001", "instagram", "publish_media",
            new PostContent("Test!", null, null, null),
            "AUTO_APPROVED", true, "campaign-001"
        );

        PostContentDryRun result = skill.simulateDryRun(request);
        assertNotNull(result.note());
        assertTrue(result.note().contains("NOT"),
            "note must confirm the post was NOT sent per dry run output contract");
    }

    @Test
    @DisplayName("SKILL-001 RULE: Must throw DirectApiCallException if called without MCP")
    void postContent_directApiCall_throwsException() {
        
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        
        PostContentRequest request = new PostContentRequest(
            "task-bypass", "influencer-001", "twitter", "direct_api_call",
            new PostContent("Direct call!", null, null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        assertThrows(DirectApiCallException.class,
            () -> skill.publish(request),
            "Direct API calls must be strictly prohibited — MCP only"
        );
    }

    @Test
    @DisplayName("SKILL-001 RULE: Platform routing must use correct MCP tool")
    void postContent_twitterPlatform_routesToCorrectMCPTool() {
        PostContentSkill skill = null;
        assertNotNull(skill, "PostContentSkill must be implemented");

        // Twitter must route to mcp-server-twitter:post_tweet
        PostContentRequest request = new PostContentRequest(
            "task-001", "influencer-001",
            "twitter",      // platform
            "post_tweet",   // action
            new PostContent("Tweet!", null, null, null),
            "AUTO_APPROVED", false, "campaign-001"
        );

        PostContentSuccess result = skill.publish(request);
        assertEquals("twitter", result.platform(),
            "Platform must be echoed correctly in output");
    }

    
    @Test
    @DisplayName("IMMUTABILITY: PostContentRequest must be a Java Record")
    void immutability_postContentRequest_isRecord() {
        assertTrue(PostContentRequest.class.isRecord(),
            "PostContentRequest MUST be a Java Record — no mutable POJOs");
    }

    @Test
    @DisplayName("IMMUTABILITY: PostContentSuccess must be a Java Record")
    void immutability_postContentSuccess_isRecord() {
        assertTrue(PostContentSuccess.class.isRecord(),
            "PostContentSuccess MUST be a Java Record");
    }

    @Test
    @DisplayName("IMMUTABILITY: PostContentDryRun must be a Java Record")
    void immutability_postContentDryRun_isRecord() {
        assertTrue(PostContentDryRun.class.isRecord(),
            "PostContentDryRun MUST be a Java Record");
    }

    @Test
    @DisplayName("IMMUTABILITY: PostContentFailure must be a Java Record")
    void immutability_postContentFailure_isRecord() {
        assertTrue(PostContentFailure.class.isRecord(),
            "PostContentFailure MUST be a Java Record");
    }

    @Test
    @DisplayName("IMMUTABILITY: PostContent must be a Java Record")
    void immutability_postContent_isRecord() {
        assertTrue(PostContent.class.isRecord(),
            "PostContent (nested content object) MUST be a Java Record");
    }
}