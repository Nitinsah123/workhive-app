package com.workhive.module.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class IntegrationDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectIntegrationRequest {
        @NotBlank(message = "Provider is required")
        private String provider; // GITHUB, GITLAB, JIRA, SLACK

        @NotBlank(message = "Access token is required")
        private String accessToken;

        private String instanceUrl; // for Jira (e.g. https://domain.atlassian.net), GitLab, Slack
        private String externalUsername; // Jira email, GitLab user, Slack bot user
        private String scopes;
        private String defaultChannel; // for Slack
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubOAuthUrlResponse {
        private String authorizationUrl;
        private String state;
        private String clientId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubOAuthExchangeRequest {
        @NotBlank(message = "Authorization code is required")
        private String code;
        private String state;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubRepositoryDto {
        private Long id;
        private String name;
        private String fullName;
        private String description;
        private String htmlUrl;
        private String defaultBranch;
        private boolean isPrivate;
        private String owner;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubCommitDto {
        private String sha;
        private String shortSha;
        private String message;
        private String authorName;
        private String authorEmail;
        private String date;
        private String htmlUrl;
        private String repositoryName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubPullRequestDto {
        private Long number;
        private String title;
        private String state;
        private String author;
        private String headBranch;
        private String baseBranch;
        private String createdAt;
        private String updatedAt;
        private String htmlUrl;
        private String repositoryName;
        private boolean draft;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubIssueDto {
        private Long number;
        private String title;
        private String state;
        private String author;
        private String createdAt;
        private String updatedAt;
        private String htmlUrl;
        private String repositoryName;
        private List<String> labels;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubOverviewDto {
        private String username;
        private String htmlUrl;
        private String avatarUrl;
        private int repositoriesCount;
        private int pullRequestsCount;
        private int issuesCount;
        private int commitsCount;
        private String webhookStatus;
        private String syncStatus;
        private Instant lastSyncAt;
        private List<GitHubRepositoryDto> recentRepositories;
        private List<GitHubPullRequestDto> recentPullRequests;
        private List<GitHubIssueDto> recentIssues;
        private List<GitHubCommitDto> recentCommits;
    }

    // ==========================================
    // JIRA DTOs
    // ==========================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JiraProjectDto {
        private String id;
        private String key;
        private String name;
        private String projectTypeKey;
        private String lead;
        private String avatarUrl;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JiraIssueDto {
        private String id;
        private String key;
        private String summary;
        private String status;
        private String priority;
        private String issueType;
        private String assigneeName;
        private String reporterName;
        private String created;
        private String updated;
        private String url;
    }

    // ==========================================
    // GITLAB DTOs
    // ==========================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitLabProjectDto {
        private Long id;
        private String name;
        private String pathWithNamespace;
        private String description;
        private String webUrl;
        private String defaultBranch;
        private String visibility;
        private Integer starCount;
        private String lastActivityAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitLabCommitDto {
        private String id;
        private String shortId;
        private String title;
        private String message;
        private String authorName;
        private String authorEmail;
        private String authoredDate;
        private String webUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitLabMergeRequestDto {
        private Long id;
        private Long iid;
        private String title;
        private String state;
        private String authorName;
        private String sourceBranch;
        private String targetBranch;
        private String webUrl;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitLabIssueDto {
        private Long id;
        private Long iid;
        private String title;
        private String state;
        private String authorName;
        private String webUrl;
        private String createdAt;
    }

    // ==========================================
    // SLACK DTOs
    // ==========================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackChannelDto {
        private String id;
        private String name;
        private boolean isPrivate;
        private Integer numMembers;
        private String topic;
        private String purpose;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackAuthTestDto {
        private boolean ok;
        private String team;
        private String user;
        private String teamId;
        private String userId;
        private String url;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackPostMessageRequest {
        @NotBlank(message = "Channel is required")
        private String channel;

        @NotBlank(message = "Message text is required")
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackPostMessageResponse {
        private boolean ok;
        private String channel;
        private String ts;
        private String message;
    }

    // ==========================================
    // ENTITY MAPPING & WEBHOOK
    // ==========================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapExternalEntityRequest {
        @NotBlank(message = "External ID is required")
        private String externalId;
        private String externalName;
        @NotBlank(message = "External type is required")
        private String externalType; // REPOSITORY, ISSUE, PROJECT, CHANNEL

        private String workhiveEntityType; // PROJECT, TASK
        private UUID workhiveEntityId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubWebhookPayload {
        private String action;
        private String repositoryName;
        private String sender;
        private String commitMessage;
        private String commitId;
        private String commitUrl;
        private String prTitle;
        private String prNumber;
        private String prUrl;
        private String prState;
        private String issueTitle;
        private String issueNumber;
        private String issueUrl;
        private String issueState;
        private String rawPayload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationSyncResponse {
        private UUID integrationId;
        private String provider;
        private String status;
        private int itemsSynced;
        private String message;
        private Instant timestamp;
    }
}
