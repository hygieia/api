package com.capitalone.dashboard.webhook.github;

import com.capitalone.dashboard.model.GitHubCollector;
import com.capitalone.dashboard.model.PullRequestEvent;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.UserEntitlementsRepository;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.model.webhook.github.GitHubParsed;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.Comment;
import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.CommitStatus;
import com.capitalone.dashboard.model.GitRequest;
import com.capitalone.dashboard.model.Review;
import com.capitalone.dashboard.repository.CommitRepository;
import com.capitalone.dashboard.repository.GitRequestRepository;
import com.capitalone.dashboard.service.CollectorService;
import com.capitalone.dashboard.webhook.settings.GitHubWebHookSettings;
import com.capitalone.dashboard.webhook.settings.WebHookSettings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class GitHubPullRequestV3 extends GitHubV3 {
    private static final Log LOG = LogFactory.getLog(GitHubPullRequestV3.class);

    private final GitRequestRepository gitRequestRepository;
    private final CommitRepository commitRepository;

    public GitHubPullRequestV3(CollectorService collectorService,
                               RestClient restClient,
                               GitRequestRepository gitRequestRepository,
                               CommitRepository commitRepository,
                               CollectorItemRepository collectorItemRepository,
                               UserEntitlementsRepository userEntitlementsRepository,
                               ApiSettings apiSettings,
                               BaseCollectorRepository<GitHubCollector> collectorRepository) {
        super(collectorService, restClient, apiSettings, collectorItemRepository, userEntitlementsRepository, collectorRepository);

        this.gitRequestRepository = gitRequestRepository;
        this.commitRepository = commitRepository;
    }

    @Override
    public CollectorItemRepository getCollectorItemRepository() { return super.collectorItemRepository; }

    @Override
    public String process(JSONObject prJsonObject) throws MalformedURLException, HygieiaException, ParseException {
        Object pullRequestObject = restClient.getAsObject(prJsonObject, "pull_request");
        String event = restClient.getString(prJsonObject,"action");
        if(!isValidEvent(event)) return "Pull Request data skipped due to event - "+event;
        if (pullRequestObject == null) return "Pull Request Data Not Available";

        int prNumber = restClient.getInteger(pullRequestObject,"number");
        if (prNumber == 0) return "Pull Request Number Not Available";

        Object repoMap = prJsonObject.get("repository");
        if (repoMap == null) { return "Repository Data Not Available"; }

        String repoUrl = restClient.getString(repoMap, "html_url");
        GitHubParsed gitHubParsed = new GitHubParsed(repoUrl);

        boolean isPrivate = restClient.getBoolean(repoMap, "private");

        JSONObject postBody = buildGraphQLQuery(gitHubParsed, pullRequestObject);

        if (postBody == null) { return "No Commits found on the PR. Returning ...";}

        WebHookSettings webHookSettings = apiSettings.getWebHook();

        if (webHookSettings == null) {
            return "Github Webhook properties not set on the properties file";
        }

        GitHubWebHookSettings gitHubWebHookSettings = webHookSettings.getGitHub();

        if (gitHubWebHookSettings == null) {
            return "Github Webhook properties not set on the properties file";
        }

        String gitHubWebHookToken = gitHubWebHookSettings.getToken();

        long start = System.currentTimeMillis();

        String repoToken = getRepositoryToken(gitHubParsed.getUrl());

        long end = System.currentTimeMillis();
        LOG.debug("Time to make collectorItemRepository call to fetch repository token = "+(end-start));

        String token = isPrivate ? repoToken : gitHubWebHookToken;

        if (StringUtils.isEmpty(token)) {
            throw new HygieiaException("Failed processing payload. Missing Github API token in Hygieia.", HygieiaException.INVALID_CONFIGURATION);
        }

        ResponseEntity<String> response = null;

        int retryCount = 0;
        while(true) {
            try {
                response = restClient.makeRestCallPost(gitHubParsed.getGraphQLUrl(), "token", token, postBody);
                break;
            } catch (Exception e) {
                retryCount++;
                if(retryCount > gitHubWebHookSettings.getMaxRetries()) {
                    LOG.error("Unable to get PR from " + repoUrl + " after " + gitHubWebHookSettings.getMaxRetries() + " tries!");
                    throw new HygieiaException(e);
                }
            }
        }

        JSONObject responseJsonObject = restClient.parseAsObject(response);

        if ((responseJsonObject == null) || responseJsonObject.isEmpty()) { return "GraphQL Response Empty From "+gitHubParsed.getGraphQLUrl(); }

        checkForErrors(responseJsonObject);

        JSONObject prData = (JSONObject) responseJsonObject.get("data");

        if ((prData == null) || prData.isEmpty()) { return "Pull Request Data Empty From "+gitHubParsed.getGraphQLUrl(); }

        Object base = restClient.getAsObject(pullRequestObject, "base");
        String branch = restClient.getString(base, "ref");

        if(!isRegistered(repoUrl, branch)) {
            return "Repo: <" + repoUrl + "> Branch: <" + branch + "> is not registered in Hygieia";
        }

        GitRequest pull = buildGitRequestFromPayload(repoUrl, branch, pullRequestObject, token);

        updateGitRequestWithGraphQLData(pull, repoUrl, branch, prData, token);

        updateCollectorItemLastUpdated(repoUrl, branch);
        gitRequestRepository.save(pull);

        return "Pull Request Processed Successfully";
    }

    protected JSONObject buildGraphQLQuery(GitHubParsed gitHubParsed, Object pullRequestObject) {
        StringBuilder queryBuilder = new StringBuilder("");

        int pullNumber = restClient.getInteger(pullRequestObject,"number");
        int commitsCount = restClient.getInteger(pullRequestObject, "commits");
        int commentsCount = restClient.getInteger(pullRequestObject, "comments");

        if (commitsCount == 0) { return null; }

        JSONObject variableJSON = new JSONObject();
        variableJSON.put("owner", gitHubParsed.getOrgName());
        variableJSON.put("name", gitHubParsed.getRepoName());
        variableJSON.put("number", pullNumber);

        queryBuilder.append(GraphQLQuery.PR_GRAPHQL_BEGIN_PRE);
        if (commitsCount > 0) {
            queryBuilder.append(GraphQLQuery.PR_GRAPHQL_COMMITS_BEGIN);
            variableJSON.put("commits", commitsCount);
        }
        if (commentsCount > 0) {
            queryBuilder.append(GraphQLQuery.PR_GRAPHQL_COMMENTS_BEGIN);
            variableJSON.put("comments", commentsCount);
        }
        queryBuilder.append(GraphQLQuery.PR_GRAPHQL_BEGIN_POST);

        if (commitsCount > 0) {
            queryBuilder.append(GraphQLQuery.PR_GRAPHQL_COMMITS);
        }
        if (commentsCount > 0) {
            queryBuilder.append(GraphQLQuery.PR_GRAPHQL_COMMENTS);
        }

        queryBuilder.append(GraphQLQuery.PR_GRAPHQL_REVIEWS);
        queryBuilder.append(GraphQLQuery.PR_GRAPHQL_END);

        JSONObject query = new JSONObject();

        query.put("query", queryBuilder.toString());
        query.put("variables", variableJSON.toString());

        return query;
    }

    protected GitRequest buildGitRequestFromPayload(String repoUrl, String branch, Object pullRequestObject, String token) throws HygieiaException, MalformedURLException {
        GitRequest pull = new GitRequest();
        GitHubParsed gitHubParsed = new GitHubParsed(repoUrl);

        pull.setRequestType("pull");
        pull.setNumber(restClient.getString(pullRequestObject,"number"));
        Object user = restClient.getAsObject(pullRequestObject, "user");
        pull.setUserId(restClient.getString(user, "login"));
        pull.setScmUrl(repoUrl);
        pull.setScmBranch(branch);
        pull.setOrgName(gitHubParsed.getOrgName());
        pull.setRepoName(gitHubParsed.getRepoName());
        pull.setScmCommitLog(restClient.getString(pullRequestObject, "title"));
        pull.setTimestamp(System.currentTimeMillis());

        String createdTimestampStr = restClient.getString(pullRequestObject, "created_at");
        long createdTimestampMillis = getTimeStampMills(createdTimestampStr);
        pull.setCreatedAt(createdTimestampMillis);

        String updatedTimestampStr = restClient.getString(pullRequestObject, "updated_at");
        pull.setUpdatedAt(getTimeStampMills(updatedTimestampStr));

        String closedTimestampStr = restClient.getString(pullRequestObject, "closed_at");
        pull.setClosedAt(getTimeStampMills(closedTimestampStr));

        String stateStr = restClient.getString(pullRequestObject, "state");
        if (!StringUtils.isEmpty(stateStr)) {
            if ("closed".equalsIgnoreCase(stateStr) || "close".equalsIgnoreCase(stateStr)) {
                stateStr = "merged";
            }
            pull.setState(stateStr.toLowerCase());
        }

        // Source Repo on which the changes/commits have been made.
        Object head = restClient.getAsObject(pullRequestObject, "head");
        pull.setHeadSha(restClient.getString(head, "sha"));
        Object headRepo = restClient.getAsObject(head, "repo");
        pull.setSourceRepo(restClient.getString(headRepo, "full_name"));
        pull.setSourceBranch(restClient.getString(head, "ref"));

        // Target Repo against which the PR has been raised.
        Object base = restClient.getAsObject(pullRequestObject, "base");
        pull.setBaseSha(restClient.getString(base, "sha"));
        pull.setTargetBranch(branch);
        pull.setTargetRepo(!Objects.equals("", gitHubParsed.getOrgName()) ? gitHubParsed.getOrgName() + "/" + gitHubParsed.getRepoName() : gitHubParsed.getRepoName());

        // Total number of commits
        pull.setNumberOfChanges(restClient.getInteger(pullRequestObject, "commits"));
        pull.setCountFilesChanged(restClient.getLong(pullRequestObject, "changed_files"));
        pull.setLineAdditions(restClient.getLong(pullRequestObject, "additions"));
        pull.setLineDeletions(restClient.getLong(pullRequestObject, "deletions"));

        // Merge Details: From the closed PR
        long mergedTimestampMillis = getTimeStampMills(restClient.getString(pullRequestObject, "merged_at"));

        if (mergedTimestampMillis > 0) {
            if (createdTimestampMillis > 0) {
                pull.setResolutiontime((mergedTimestampMillis - createdTimestampMillis));
            }
            pull.setScmCommitTimestamp(mergedTimestampMillis);
            pull.setMergedAt(mergedTimestampMillis);
            String mergeSha = restClient.getString(pullRequestObject, "merge_commit_sha");
            pull.setScmRevisionNumber(mergeSha);
            pull.setScmMergeEventRevisionNumber(mergeSha);
            Object mergedBy = restClient.getAsObject(pullRequestObject,"merged_by");
            pull.setMergeAuthor(restClient.getString(mergedBy, "login"));
            String mergeAuthorType = getAuthorType(repoUrl, pull.getMergeAuthor(), token);
            if (!StringUtils.isEmpty(mergeAuthorType)) {
                pull.setMergeAuthorType(mergeAuthorType);
            }
            String mergeAuthorLDAPDN = getLDAPDN(repoUrl, pull.getMergeAuthor(), token);
            if (!StringUtils.isEmpty(mergeAuthorLDAPDN)) {
                pull.setMergeAuthorLDAPDN(mergeAuthorLDAPDN);
            }
        }

        setCollectorItemId(pull);

        return pull;
    }

    protected void setCollectorItemId (GitRequest pull) throws MalformedURLException, HygieiaException {
        long start = System.currentTimeMillis();

        GitRequest existingPR
                = gitRequestRepository.findByScmUrlIgnoreCaseAndScmBranchIgnoreCaseAndNumberAndRequestTypeIgnoreCase(pull.getScmUrl(), pull.getScmBranch(), pull.getNumber(), "pull");

        if (existingPR != null) {
            pull.setId(existingPR.getId());
            pull.setCollectorItemId(existingPR.getCollectorItemId());
            CollectorItem collectorItem = collectorService.getCollectorItem(existingPR.getCollectorItemId());
            collectorItem.setEnabled(true);
            collectorItem.setPushed(true);
            collectorItemRepository.save(collectorItem);
        } else {
            GitHubParsed gitHubParsed = new GitHubParsed(pull.getScmUrl());
            CollectorItem collectorItem = getCollectorItem(gitHubParsed.getUrl(), pull.getScmBranch());
            pull.setCollectorItemId(collectorItem.getId());
        }

        long end = System.currentTimeMillis();

        LOG.debug("Time to make gitRequestRepository call to create the collector item = "+(end-start));
    }

    protected void updateGitRequestWithGraphQLData(GitRequest pull, String repoUrl,
                                                   String branch, JSONObject prData,
                                                   String token)  {
        LOG.debug("prData = "+prData.toJSONString());

        Object repoObject = restClient.getAsObject(prData, "repository");
        if (repoObject == null) {
            LOG.info("No Repository Data Available For "+repoUrl+" ; Branch "+branch+". Returning ...");
            return;
        }

        Object pullRequestObject = restClient.getAsObject(repoObject, "pullRequest");

        if (pullRequestObject == null) {
            LOG.info("No Pull Request Data Available For "+repoUrl+" ; Branch "+branch+". Returning ...");
            return;
        }

        if (pull.getMergedAt() > 0) {
            Object commitsObject = restClient.getAsObject(pullRequestObject, "commits");
            pull.setNumberOfChanges(restClient.getInteger(commitsObject, "totalCount"));

            List<Commit> prCommits = getPRCommits(repoUrl, commitsObject, pull, token);
            pull.setCommits(prCommits);

            Object commentsObject = restClient.getAsObject(pullRequestObject,"comments");
            List<Comment> comments = getComments(repoUrl, commentsObject, token);
            pull.setComments(comments);

            Object reviewsObject = restClient.getAsObject(pullRequestObject,"reviews");
            List<Review> reviews = getReviews(repoUrl, reviewsObject, token);
            pull.setReviews(reviews);
        }
    }

    protected List<Review> getReviews(String repoUrl, Object reviewObject, String token) throws RestClientException {
        List<Review> reviews = new ArrayList<>();

        if (reviewObject == null) { return reviews; }

        JSONArray nodes = (JSONArray) restClient.getAsObject(reviewObject, "nodes");

        if (CollectionUtils.isEmpty(nodes)) { return reviews; }

        for (Object n : nodes) {
            JSONObject node = (JSONObject) n;
            Review review = new Review();
            review.setState(restClient.getString(node, "state"));
            review.setBody(restClient.getString(node, "bodyText"));
            JSONObject authorObj = (JSONObject) node.get("author");
            review.setAuthor(restClient.getString(authorObj, "login"));
            String authorType = getAuthorType(repoUrl, review.getAuthor(), token);
            if (!StringUtils.isEmpty(authorType)) {
                review.setAuthorType(authorType);
            }
            String authorLDAPDN = getLDAPDN(repoUrl, review.getAuthor(), token);
            if (!StringUtils.isEmpty(authorLDAPDN)) {
                review.setAuthorLDAPDN(authorLDAPDN);
            }
            review.setCreatedAt(getTimeStampMills(restClient.getString(node, "createdAt")));
            review.setUpdatedAt(getTimeStampMills(restClient.getString(node, "updatedAt")));
            reviews.add(review);
        }

        return reviews;
    }

    protected List<Comment> getComments(String repoUrl, Object commentsObject, String token) throws RestClientException {
        List<Comment> comments = new ArrayList<>();
        if (commentsObject == null) {
            return comments;
        }
        JSONArray nodes = (JSONArray) restClient.getAsObject(commentsObject, "nodes");
        if (CollectionUtils.isEmpty(nodes)) { return comments; }

        for (Object n : nodes) {
            JSONObject node = (JSONObject) n;
            Comment comment = new Comment();
            comment.setBody(restClient.getString(node, "bodyText"));
            comment.setUser(restClient.getString((JSONObject) node.get("author"), "login"));
            String userType = getAuthorType(repoUrl, comment.getUser(), token);
            if (!StringUtils.isEmpty(userType)) {
                comment.setUserType(userType);
            }
            String userLDAPDN = getLDAPDN(repoUrl, comment.getUser(), token);
            if (!StringUtils.isEmpty(userLDAPDN)) {
                comment.setUserLDAPDN(userLDAPDN);
            }
            comment.setCreatedAt(getTimeStampMills(restClient.getString(node, "createdAt")));
            comment.setUpdatedAt(getTimeStampMills(restClient.getString(node, "updatedAt")));
            comment.setStatus(restClient.getString(node, "state"));
            comments.add(comment);
        }

        return comments;
    }

    protected List<Commit> getPRCommits(String repoUrl, Object commitsObject, GitRequest pull, String token) {
        List<Commit> prCommits = new ArrayList<>();

        if (commitsObject == null) { return prCommits; }

        String prHeadSha = pull.getHeadSha();

        JSONArray nodes = (JSONArray) restClient.getAsObject(commitsObject, "nodes");

        if (CollectionUtils.isEmpty(nodes)) { return prCommits; }

        JSONObject lastCommitStatusObject = null;
        long lastCommitTime = 0L;
        for (Object n : nodes) {
            JSONObject c = (JSONObject) n;
            JSONObject commit = (JSONObject) c.get("commit");
            String commitOid = restClient.getString(commit, "oid");

            Commit newCommit = new Commit();
            newCommit.setScmRevisionNumber(commitOid);
            newCommit.setScmCommitLog(restClient.getString(commit, "message"));
            JSONObject author = (JSONObject) commit.get("author");
            JSONObject authorUserJSON = (JSONObject) author.get("user");
            newCommit.setScmAuthor(restClient.getString(author, "name"));
            newCommit.setScmAuthorLogin((authorUserJSON == null) ? "unknown" : restClient.getString(authorUserJSON, "login"));
            String scmAuthorName = authorUserJSON == null ? null : restClient.getString(authorUserJSON, "name");
            newCommit.setScmAuthorName(scmAuthorName);

            String authorType = getAuthorType(repoUrl, newCommit.getScmAuthorLogin(), token);
            if (!StringUtils.isEmpty(authorType)) {
                newCommit.setScmAuthorType(authorType);
            }
            String authorLDAPDN = getLDAPDN(repoUrl, newCommit.getScmAuthorLogin(), token);
            if (!StringUtils.isEmpty(authorLDAPDN)) {
                newCommit.setScmAuthorLDAPDN(authorLDAPDN);
            }

            int changedFiles = NumberUtils.toInt(restClient.getString(commit, "changedFiles"));
            int deletions = NumberUtils.toInt(restClient.getString(commit, "deletions"));
            int additions = NumberUtils.toInt(restClient.getString(commit, "additions"));
            newCommit.setNumberOfChanges((long) changedFiles + deletions + additions);

            newCommit.setScmCommitTimestamp(getTimeStampMills(restClient.getString(author, "date")));
            JSONObject statusObj = (JSONObject) commit.get("status");

            if (statusObj != null && lastCommitTime <= newCommit.getScmCommitTimestamp()) {
                lastCommitTime = newCommit.getScmCommitTimestamp();
                lastCommitStatusObject = statusObj;
                setPRCommitStatus(statusObj, newCommit, pull);
            }

            // Relies mostly on an open pr to find commits from other repos, branches in the database.
            updateMatchingCommitsInDb(newCommit, pull);

            prCommits.add(newCommit);
        }

        updateCommitsWithPullNumber(pull);

        if (StringUtils.isEmpty(prHeadSha) || CollectionUtils.isEmpty(pull.getCommitStatuses())) {
            List<CommitStatus> commitStatuses = getCommitStatuses(lastCommitStatusObject);
            List<CommitStatus> existingCommitStatusList = pull.getCommitStatuses();
            if (!CollectionUtils.isEmpty(commitStatuses) && !CollectionUtils.isEmpty(existingCommitStatusList)) {
                existingCommitStatusList.addAll(commitStatuses);
            } else {
                pull.setCommitStatuses(commitStatuses);
            }
        }

        return prCommits;
    }

    private void setPRCommitStatus(JSONObject statusObj, Commit newCommit, GitRequest pull) {
        String prHeadSha = pull.getHeadSha();
        if (Objects.equals(newCommit.getScmRevisionNumber(), prHeadSha)) {
            List<CommitStatus> commitStatuses = getCommitStatuses(statusObj);
            List<CommitStatus> existingCommitStatusList = pull.getCommitStatuses();
            if (!CollectionUtils.isEmpty(commitStatuses) && !CollectionUtils.isEmpty(existingCommitStatusList)) {
                existingCommitStatusList.addAll(commitStatuses);
            } else {
                pull.setCommitStatuses(commitStatuses);
            }
        }

    }

    protected void updateMatchingCommitsInDb(Commit commit, GitRequest pull) {
        long start = System.currentTimeMillis();

        List<Commit> commitsInDb
                = commitRepository.findAllByScmRevisionNumberAndScmAuthorIgnoreCaseAndScmCommitLogAndScmCommitTimestamp(commit.getScmRevisionNumber(), commit.getScmAuthor(), commit.getScmCommitLog(), commit.getScmCommitTimestamp());
        if(CollectionUtils.isEmpty(commitsInDb)) { return; }
        commitsInDb.forEach(commitInDb -> {
            commitInDb.setPullNumber(pull.getNumber());
            commitRepository.save(commitInDb);
        });

        long end = System.currentTimeMillis();

        LOG.debug("Time to make commitRepository call = "+(end-start));
    }

    // Add pull number to merge commits for the PR if they don't have one, in case of rebase merge or squash merge
    private void updateCommitsWithPullNumber(GitRequest pull) {
        long start = System.currentTimeMillis();
        List<Commit> commitsInDb
                = commitRepository.findByScmRevisionNumber(pull.getScmRevisionNumber());
        if(CollectionUtils.isEmpty(commitsInDb)) { return; }
        commitsInDb.forEach(commitInDb -> {
            commitInDb.setPullNumber(pull.getNumber());
            commitRepository.save(commitInDb);
        });
        long end = System.currentTimeMillis();
        LOG.debug("Time to make commitRepository call = "+(end-start));
    }

    protected List<CommitStatus> getCommitStatuses(JSONObject statusObject) throws RestClientException {
        Map<String, CommitStatus> statuses = new HashMap<>();

        if (statusObject == null) { return new ArrayList<>(); }

        JSONArray contexts = (JSONArray) statusObject.get("contexts");

        if (CollectionUtils.isEmpty(contexts)) { return new ArrayList<>(); }

        for (Object ctx : contexts) {
            String ctxStr = restClient.getString((JSONObject) ctx, "context");
            if ((ctxStr != null) && !statuses.containsKey(ctxStr)) {
                CommitStatus status = new CommitStatus();
                status.setContext(ctxStr);
                status.setDescription(restClient.getString((JSONObject) ctx, "description"));
                status.setState(restClient.getString((JSONObject) ctx, "state"));
                statuses.put(ctxStr, status);
            }
        }

        return new ArrayList<>(statuses.values());
    }

    private long getTimeStampMills(String dateTime) {
        return StringUtils.isEmpty(dateTime) ? 0 : new DateTime(dateTime).getMillis();
    }

    private boolean isValidEvent(String action) {
        List<PullRequestEvent> validPullRequestEvents = new ArrayList<>();
        Collections.addAll(validPullRequestEvents,PullRequestEvent.Opened,PullRequestEvent.Edited,PullRequestEvent.Closed,
                PullRequestEvent.Reopened,
                PullRequestEvent.Merged,
                PullRequestEvent.Synchronize);
        return validPullRequestEvents.contains(PullRequestEvent.fromString(action));
    }

}