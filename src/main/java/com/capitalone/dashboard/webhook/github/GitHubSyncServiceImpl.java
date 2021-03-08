package com.capitalone.dashboard.webhook.github;

import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.client.RestUserInfo;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CollectionError;
import com.capitalone.dashboard.model.CollectionMode;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.Comment;
import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.CommitStatus;
import com.capitalone.dashboard.model.CommitType;
import com.capitalone.dashboard.model.GitHubPaging;
import com.capitalone.dashboard.model.GitRequest;
import com.capitalone.dashboard.model.Review;
import com.capitalone.dashboard.model.webhook.github.GitHubParsed;
import com.capitalone.dashboard.model.webhook.github.GitHubRepo;
import com.capitalone.dashboard.model.webhook.github.MergeEvent;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.CommitRepository;
import com.capitalone.dashboard.repository.GitHubRepoRepository;
import com.capitalone.dashboard.repository.GitRequestRepository;
import com.capitalone.dashboard.request.GitSyncRequest;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.util.CommitPullMatcher;
import com.capitalone.dashboard.util.Encryption;
import com.capitalone.dashboard.util.EncryptionException;
import com.capitalone.dashboard.util.GithubGraphQLQuery;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GitHubSyncServiceImpl implements GitHubSyncService {
    public static final String GIT_HUB = "GitHub";
    private static final Log LOG = LogFactory.getLog(GitHubSyncServiceImpl.class);
    private static final long ONE_DAY_IN_MILLISECONDS = (long) 24 * 60 * 60 * 1000;

    private final CommitRepository commitRepository;
    private final GitRequestRepository gitRequestRepository;
    private final CollectorItemRepository collectorItemRepository;
    protected final ApiSettings apiSettings;
    protected final RestClient restClient;
    protected final GitHubRepoRepository gitHubRepoRepository;
    protected final CollectorRepository collectorRepository;
    List<Commit> commits;
    List<GitRequest> pullRequests;
    List<GitRequest> issues;
    Map<String, String> ldapMap;
    Map<String, String> authorTypeMap;
    private final List<Pattern> commitExclusionPatterns = new ArrayList<>();


    @Autowired
    public GitHubSyncServiceImpl(CommitRepository commitRepository,
                                 GitRequestRepository gitRequestRepository,
                                 CollectorItemRepository collectorItemRepository,
                                 GitHubRepoRepository gitHubRepoRepository,
                                 CollectorRepository collectorRepository,
                                 ApiSettings apiSettings,
                                 RestClient restClient) {
        this.commitRepository = commitRepository;
        this.gitRequestRepository = gitRequestRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.apiSettings = apiSettings;
        this.restClient = restClient;
        this.gitHubRepoRepository = gitHubRepoRepository;
        this.collectorRepository = collectorRepository;

        if (!CollectionUtils.isEmpty(apiSettings.getGithubSyncSettings().getNotBuiltCommits())) {
            apiSettings.getGithubSyncSettings().getNotBuiltCommits().stream().map(regExStr -> Pattern.compile(regExStr, Pattern.CASE_INSENSITIVE)).forEach(commitExclusionPatterns::add);
        }

    }

    @Override
    public String syncGithubRepo(GitSyncRequest request) throws ParseException, HygieiaException, MalformedURLException {

        int commitCount = 0;
        int pullCount = 0;
        int issueCount = 0;
        String statusString;
        long start = System.currentTimeMillis();
        int repoCount = 0;
        apiSettings.getGithubSyncSettings().setFirstRunHistoryDays(request.getHistoryDays());
        try {
            Collector collector = collectorRepository.findByName(GIT_HUB);
            if (Objects.isNull(collector)) return "Invalid collector";
            List<GitHubRepo> repos = gitHubRepoRepository.findRepoByUrlAndBranch(collector.getId(), request.getRepo(), request.getBranch());
            for (GitHubRepo repo : repos) {
                try {
                    collectorItemRepository.save(repo);
                    List<GitRequest> allRequests = gitRequestRepository.findRequestNumberAndLastUpdated(repo.getId());

                    Map<Long, String> existingPRMap = allRequests.stream().filter(r -> Objects.equals(r.getRequestType(), "pull")).collect(
                            Collectors.toMap(GitRequest::getUpdatedAt, GitRequest::getNumber,
                                    (oldValue, newValue) -> oldValue
                            )
                    );

                    Map<Long, String> existingIssueMap = allRequests.stream().filter(r -> Objects.equals(r.getRequestType(), "issue")).collect(
                            Collectors.toMap(GitRequest::getUpdatedAt, GitRequest::getNumber,
                                    (oldValue, newValue) -> oldValue
                            )
                    );

                    fireGraphQL(repo, true, existingPRMap, existingIssueMap);
                    int commitCount1 = processCommits(repo);
                    commitCount += commitCount1;

                    //Get all the Pull Requests
                    int pullCount1 = processPRorIssueList(repo, allRequests.stream().filter(r -> Objects.equals(r.getRequestType(), "pull")).collect(Collectors.toList()), "pull");
                    pullCount += pullCount1;

                    //Get all the Issues
                    int issueCount1 = processPRorIssueList(repo, allRequests.stream().filter(r -> Objects.equals(r.getRequestType(), "issue")).collect(Collectors.toList()), "issue");
                    issueCount += issueCount;

                    // Due to timing of PRs and Commits in PR merge event, some commits may not be included in the response and will not be connected to a PR.
                    // This is the place attempting to re-connect the commits and PRs in case they were missed during previous run.

                    processOrphanCommits(repo);

                    repo.setLastUpdated(System.currentTimeMillis());
                    // if everything went alright, there should be no error!
                    repo.getErrors().clear();
                    statusString = "SUCCESS, pulls=" + pullCount1 + ", commits=" + pullCount1 + ", issues=" + issueCount1;
                    repoCount++;

                } catch (HttpStatusCodeException hc) {
                    LOG.error("Error fetching commits for:" + repo.getRepoUrl(), hc);
                    statusString = "EXCEPTION, " + hc.getClass().getCanonicalName();
                    CollectionError error = new CollectionError(hc.getStatusCode().toString(), hc.getMessage());
                    if (hc.getStatusCode() == HttpStatus.UNAUTHORIZED || hc.getStatusCode() == HttpStatus.FORBIDDEN) {
                        LOG.info("add 0.2 sec delay when received 401/403 from GitHub");

                    }
                    repo.getErrors().add(error);
                } catch (RestClientException | MalformedURLException ex) {
                    LOG.error("Error fetching commits for:" + repo.getRepoUrl(), ex);
                    statusString = "EXCEPTION, " + ex.getClass().getCanonicalName();
                    CollectionError error = new CollectionError(CollectionError.UNKNOWN_HOST, ex.getMessage());
                    repo.getErrors().add(error);
                } catch (HygieiaException he) {
                    LOG.error("Error fetching commits for:" + repo.getRepoUrl(), he);
                    statusString = "EXCEPTION, " + he.getClass().getCanonicalName();
                    CollectionError error = new CollectionError(String.valueOf(he.getErrorCode()), he.getMessage());
                    repo.getErrors().add(error);
                }
                gitHubRepoRepository.save(repo);
            }
        } catch (Throwable e) {
            statusString = "EXCEPTION, " + e.getClass().getCanonicalName();
            LOG.error("Unexpected exception when collecting : " + statusString, e);
        }
        long end = System.currentTimeMillis();
        long elapsedSeconds = (end - start) / 1000;
        String format = String.format("GithubSyncServiceImpl : totalProcessSeconds=%d, totalRepoCount=%d, totalNewPulls=%d, totalNewCommits=%d totalNewIssues=%d",
                elapsedSeconds, repoCount, pullCount, commitCount, issueCount);
        LOG.info(format);
        return format;

    }

    public List<Commit> getCommits() {
        return commits;
    }


    public List<GitRequest> getPulls() {
        return pullRequests;
    }


    public List<GitRequest> getIssues() {
        return issues;
    }


    public Map<String, String> getLdapMap() {
        return ldapMap;
    }

    public void setLdapMap(Map<String, String> ldapMap) {
        this.ldapMap = ldapMap;
    }


    private long getTimeStampMills(String dateTime) {
        return StringUtils.isEmpty(dateTime) ? 0 : new DateTime(dateTime).getMillis();
    }

    private static DateTime getDate(DateTime dateInstance, int offsetDays, int offsetMinutes) {
        return dateInstance.minusDays(offsetDays).minusMinutes(offsetMinutes);
    }

    private static String decryptString(String string, String key) {
        if (!StringUtils.isEmpty(string)) {
            try {
                return Encryption.decryptString(
                        string, key);
            } catch (EncryptionException e) {
                LOG.error(e.getMessage());
            }
        }
        return "";
    }

    private int getFetchCount() {
        return apiSettings.getGithubSyncSettings().getFetchCount();
    }

    private CollectionMode getCollectionMode(boolean firstTime, GitHubPaging commitPaging, GitHubPaging pullPaging, GitHubPaging issuePaging) {
        if (firstTime) {
            if (!pullPaging.isLastPage() && !issuePaging.isLastPage()) return CollectionMode.FirstTimeAll;
            if (pullPaging.isLastPage() && !issuePaging.isLastPage()) return CollectionMode.FirstTimeCommitAndIssue;
            if (!pullPaging.isLastPage() && issuePaging.isLastPage()) return CollectionMode.FirstTimeCommitAndPull;
            if (pullPaging.isLastPage() && issuePaging.isLastPage()) return CollectionMode.FirstTimeCommitOnly;
        }

        if (commitPaging.isLastPage() && pullPaging.isLastPage() && issuePaging.isLastPage())
            return CollectionMode.None;
        if (commitPaging.isLastPage() && pullPaging.isLastPage() && !issuePaging.isLastPage())
            return CollectionMode.IssueOnly;
        if (commitPaging.isLastPage() && !pullPaging.isLastPage() && !issuePaging.isLastPage())
            return CollectionMode.PullAndIssue;
        if (!commitPaging.isLastPage() && pullPaging.isLastPage() && issuePaging.isLastPage())
            return CollectionMode.CommitOnly;
        if (!commitPaging.isLastPage() && !pullPaging.isLastPage() && issuePaging.isLastPage())
            return CollectionMode.CommitAndPull;
        if (commitPaging.isLastPage() && !pullPaging.isLastPage() && issuePaging.isLastPage())
            return CollectionMode.PullOnly;
        if (!commitPaging.isLastPage() && pullPaging.isLastPage() && !issuePaging.isLastPage())
            return CollectionMode.CommitAndIssue;
        if (!commitPaging.isLastPage() && !pullPaging.isLastPage() && !issuePaging.isLastPage())
            return CollectionMode.All;
        return CollectionMode.None;
    }

    private JSONObject buildQuery(boolean firstTime, boolean firstRun, boolean missingCommits, GitHubParsed gitHubParsed, GitHubRepo repo, GitHubPaging commitPaging, GitHubPaging pullPaging, GitHubPaging issuePaging) {
        CollectionMode mode = getCollectionMode(firstTime, commitPaging, pullPaging, issuePaging);
        JSONObject jsonObj = new JSONObject();
        String query;
        JSONObject variableJSON = new JSONObject();
        variableJSON.put("owner", gitHubParsed.getOrgName());
        variableJSON.put("name", gitHubParsed.getRepoName());
        variableJSON.put("fetchCount", getFetchCount());

        LOG.debug("Collection Mode =" + mode.toString());
        switch (mode) {
            case FirstTimeAll:
                query = GithubGraphQLQuery.QUERY_BASE_ALL_FIRST + GithubGraphQLQuery.QUERY_PULL_HEADER_FIRST + GithubGraphQLQuery.QUERY_PULL_MAIN + GithubGraphQLQuery.QUERY_COMMIT_HEADER_FIRST + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_ISSUES_HEADER_FIRST + GithubGraphQLQuery.QUERY_ISSUE_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case FirstTimeCommitOnly:
                query = GithubGraphQLQuery.QUERY_BASE_ALL_FIRST + GithubGraphQLQuery.QUERY_COMMIT_HEADER_FIRST + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;

            case FirstTimeCommitAndIssue:
                query = GithubGraphQLQuery.QUERY_BASE_ALL_FIRST + GithubGraphQLQuery.QUERY_COMMIT_HEADER_FIRST + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_ISSUES_HEADER_FIRST + GithubGraphQLQuery.QUERY_ISSUE_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;

            case FirstTimeCommitAndPull:
                query = GithubGraphQLQuery.QUERY_BASE_ALL_FIRST + GithubGraphQLQuery.QUERY_PULL_HEADER_FIRST + GithubGraphQLQuery.QUERY_PULL_MAIN + GithubGraphQLQuery.QUERY_COMMIT_HEADER_FIRST + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;

            case CommitOnly:
                query = GithubGraphQLQuery.QUERY_BASE_COMMIT_ONLY_AFTER + GithubGraphQLQuery.QUERY_COMMIT_HEADER_AFTER + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("afterCommit", commitPaging.getCursor());
                variableJSON.put("branch", repo.getBranch());

                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case PullOnly:
                query = GithubGraphQLQuery.QUERY_BASE_PULL_ONLY_AFTER + GithubGraphQLQuery.QUERY_PULL_HEADER_AFTER + GithubGraphQLQuery.QUERY_PULL_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("afterPull", pullPaging.getCursor());
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case IssueOnly:
                query = GithubGraphQLQuery.QUERY_BASE_ISSUE_ONLY_AFTER + GithubGraphQLQuery.QUERY_ISSUES_HEADER_AFTER + GithubGraphQLQuery.QUERY_ISSUE_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("afterIssue", issuePaging.getCursor());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case CommitAndIssue:
                query = GithubGraphQLQuery.QUERY_BASE_COMMIT_AND_ISSUE_AFTER + GithubGraphQLQuery.QUERY_COMMIT_HEADER_AFTER + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_ISSUES_HEADER_AFTER + GithubGraphQLQuery.QUERY_ISSUE_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("afterIssue", issuePaging.getCursor());
                variableJSON.put("afterCommit", commitPaging.getCursor());
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("branch", repo.getBranch());

                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case CommitAndPull:
                query = GithubGraphQLQuery.QUERY_BASE_COMMIT_AND_PULL_AFTER + GithubGraphQLQuery.QUERY_PULL_HEADER_AFTER + GithubGraphQLQuery.QUERY_PULL_MAIN + GithubGraphQLQuery.QUERY_COMMIT_HEADER_AFTER + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("afterPull", pullPaging.getCursor());
                variableJSON.put("afterCommit", commitPaging.getCursor());
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case PullAndIssue:
                query = GithubGraphQLQuery.QUERY_BASE_ISSUE_AND_PULL_AFTER + GithubGraphQLQuery.QUERY_PULL_HEADER_AFTER + GithubGraphQLQuery.QUERY_PULL_MAIN + GithubGraphQLQuery.QUERY_ISSUES_HEADER_AFTER + GithubGraphQLQuery.QUERY_ISSUE_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("afterPull", pullPaging.getCursor());
                variableJSON.put("afterIssue", issuePaging.getCursor());
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case All:
                query = GithubGraphQLQuery.QUERY_BASE_ALL_AFTER + GithubGraphQLQuery.QUERY_COMMIT_HEADER_AFTER + GithubGraphQLQuery.QUERY_COMMIT_MAIN + GithubGraphQLQuery.QUERY_PULL_HEADER_AFTER + GithubGraphQLQuery.QUERY_PULL_MAIN + GithubGraphQLQuery.QUERY_ISSUES_HEADER_AFTER + GithubGraphQLQuery.QUERY_ISSUE_MAIN + GithubGraphQLQuery.QUERY_END;
                variableJSON.put("since", getRunDate(repo, firstRun, missingCommits));
                variableJSON.put("afterPull", pullPaging.getCursor());
                variableJSON.put("afterCommit", commitPaging.getCursor());
                variableJSON.put("afterIssue", issuePaging.getCursor());
                variableJSON.put("branch", repo.getBranch());
                jsonObj.put("query", query);
                jsonObj.put("variables", variableJSON.toString());
                break;


            case None:
                jsonObj = null;
                break;


            default:
                jsonObj = null;
                break;

        }
        return jsonObj;
    }

    private JSONObject getDataFromRestCallPost(GitHubParsed gitHubParsed, GitHubRepo repo, String password, String personalAccessToken, JSONObject query) throws MalformedURLException, HygieiaException {
        ResponseEntity<String> response = makeRestCallPost(gitHubParsed.getGraphQLUrl(), repo.getUserId(), password, personalAccessToken, query);
        JSONObject data = (JSONObject) parseAsObject(response).get("data");
        JSONArray errors = getArray(parseAsObject(response), "errors");

        if (CollectionUtils.isEmpty(errors)) {
            return data;
        }

        JSONObject error = (JSONObject) errors.get(0);

        if (!error.containsKey("type") || !error.get("type").equals("NOT_FOUND")) {
            throw new HygieiaException("Error in GraphQL query:" + errors.toJSONString(), HygieiaException.JSON_FORMAT_ERROR);
        }

        JSONParser parser = new JSONParser();
        try {
            JSONObject variableJSON = (JSONObject) parser.parse(str(query, "variables"));
            variableJSON.put("name", gitHubParsed.getRepoName());
            variableJSON.put("owner", gitHubParsed.getOrgName());
            query.put("variables", variableJSON.toString());
        } catch (ParseException e) {
            LOG.error("Could not parse JSON String", e);
        }
        return getDataFromRestCallPost(gitHubParsed, repo, password, personalAccessToken, query);
    }


    private JSONObject parseAsObject(ResponseEntity<String> response) {
        try {
            return (JSONObject) new JSONParser().parse(response.getBody());
        } catch (ParseException pe) {
            LOG.error(pe.getMessage());
        }
        return new JSONObject();
    }

    private String str(JSONObject json, String key) {
        if (json == null) return "";
        Object value = json.get(key);
        return (value == null) ? "" : value.toString();
    }

    private JSONArray getArray(JSONObject json, String key) {
        if (json == null) return new JSONArray();
        if (json.get(key) == null) return new JSONArray();
        return (JSONArray) json.get(key);
    }

    private List<String> getParentShas(JSONObject commit) {
        JSONObject parents = (JSONObject) commit.get("parents");
        JSONArray parentNodes = (JSONArray) parents.get("nodes");
        List<String> parentShas = new ArrayList<>();
        if (!CollectionUtils.isEmpty(parentNodes)) {
            for (Object parentObj : parentNodes) {
                parentShas.add(str((JSONObject) parentObj, "oid"));
            }
        }
        return parentShas;
    }

    private ResponseEntity<String> makeRestCallPost(String url, String userId, String password, String personalAccessToken, JSONObject query) {
        // Basic Auth only.
        if (!Objects.equals("", userId) && !Objects.equals("", password)) {
            RestUserInfo userInfo = new RestUserInfo(userId, password);
            return restClient.makeRestCallPost(url, userInfo, query);
        } else if (personalAccessToken != null && !Objects.equals("", personalAccessToken)) {
            return restClient.makeRestCallPost(url, "token", personalAccessToken, query);
        } else {
            // This handles the case when settings.getPersonalAccessToken() is empty
            return restClient.makeRestCallPost(url, "token", apiSettings.getGithubSyncSettings().getToken(), query);
        }
    }

    private GitHubPaging processPullRequest(JSONObject pullObject, GitHubRepo repo, Map<Long, String> prMap, long historyTimeStamp) throws MalformedURLException, HygieiaException {
        GitHubPaging paging = new GitHubPaging();
        paging.setLastPage(true);
        if (pullObject == null) return paging;
        paging.setTotalCount(asInt(pullObject, "totalCount"));
        JSONObject pageInfo = (JSONObject) pullObject.get("pageInfo");
        paging.setCursor(str(pageInfo, "endCursor"));
        paging.setLastPage(!(Boolean) pageInfo.get("hasNextPage"));
        JSONArray edges = getArray(pullObject, "edges");
        if (CollectionUtils.isEmpty(edges)) {
            return paging;
        }
        int localCount = 0;
        for (Object o : edges) {
            JSONObject node = (JSONObject) ((JSONObject) o).get("node");
            if (node == null) break;
            JSONObject userObject = (JSONObject) node.get("author");
            String merged = str(node, "mergedAt");
            String closed = str(node, "closedAt");
            String updated = str(node, "updatedAt");
            String created = str(node, "createdAt");
            long createdTimestamp = getTimeStampMills(created);
            long mergedTimestamp = getTimeStampMills(merged);
            long closedTimestamp = getTimeStampMills(closed);
            long updatedTimestamp = getTimeStampMills(updated);
            GitHubParsed gitHubParsed = new GitHubParsed(repo.getRepoUrl());
            GitRequest pull = new GitRequest();
            //General Info
            pull.setRequestType("pull");
            pull.setNumber(str(node, "number"));
            pull.setUserId(str(userObject, "login"));
            pull.setScmUrl(repo.getRepoUrl());
            pull.setScmBranch(repo.getBranch());
            pull.setOrgName(gitHubParsed.getOrgName());
            pull.setRepoName(gitHubParsed.getRepoName());
            pull.setScmCommitLog(str(node, "title"));
            pull.setTimestamp(System.currentTimeMillis());
            pull.setCreatedAt(createdTimestamp);
            pull.setClosedAt(closedTimestamp);
            pull.setUpdatedAt(updatedTimestamp);
            pull.setCountFilesChanged(asInt(node, "changedFiles"));
            pull.setLineAdditions(asInt(node, "additions"));
            pull.setLineDeletions(asInt(node, "deletions"));
            //Status
            pull.setState(str(node, "state").toLowerCase());
            JSONObject headrefJson = (JSONObject) node.get("headRef");
            if (headrefJson != null) {
                JSONObject targetJson = (JSONObject) headrefJson.get("target");
                pull.setHeadSha(str(targetJson, "oid"));
            }
            if (!StringUtils.isEmpty(merged)) {
                pull.setScmRevisionNumber(str((JSONObject) node.get("mergeCommit"), "oid"));
                pull.setResolutiontime((mergedTimestamp - createdTimestamp));
                pull.setScmCommitTimestamp(mergedTimestamp);
                pull.setMergedAt(mergedTimestamp);
                JSONObject commitsObject = (JSONObject) node.get("commits");
                pull.setNumberOfChanges(commitsObject != null ? asInt(commitsObject, "totalCount") : 0);
                List<Commit> prCommits = getPRCommits(repo, commitsObject, pull);
                pull.setCommits(prCommits);
                List<Comment> comments = getComments(repo, (JSONObject) node.get("comments"));
                pull.setComments(comments);
                List<Review> reviews = getReviews(repo, (JSONObject) node.get("reviews"));
                pull.setReviews(reviews);
                MergeEvent mergeEvent = getMergeEvent(repo, pull, (JSONObject) node.get("timeline"));
                if (mergeEvent != null) {
                    pull.setScmMergeEventRevisionNumber(mergeEvent.getMergeSha());
                    pull.setMergeAuthor(mergeEvent.getMergeAuthor());
                    String mergeAuthorType = getAuthorType(repo, mergeEvent.getMergeAuthor());
                    if (!StringUtils.isEmpty(mergeAuthorType)) {
                        pull.setMergeAuthorType(mergeAuthorType);
                    }
                    String mergeAuthorLDAPDN = getLDAPDN(repo, mergeEvent.getMergeAuthor());
                    if (!StringUtils.isEmpty(mergeAuthorLDAPDN)) {
                        pull.setMergeAuthorLDAPDN(mergeAuthorLDAPDN);
                    }
                }
            }
            // commit etc details
            pull.setSourceBranch(str(node, "headRefName"));
            if (node.get("headRepository") != null) {
                JSONObject headObject = (JSONObject) node.get("headRepository");
                GitHubParsed sourceRepoUrlParsed = new GitHubParsed(str(headObject, "url"));
                pull.setSourceRepo(!Objects.equals("", sourceRepoUrlParsed.getOrgName()) ? sourceRepoUrlParsed.getOrgName() + "/" + sourceRepoUrlParsed.getRepoName() : sourceRepoUrlParsed.getRepoName());
            }
            if (node.get("baseRef") != null) {
                pull.setBaseSha(str((JSONObject) ((JSONObject) node.get("baseRef")).get("target"), "oid"));
            }
            pull.setTargetBranch(str(node, "baseRefName"));
            pull.setTargetRepo(!Objects.equals("", gitHubParsed.getOrgName()) ? gitHubParsed.getOrgName() + "/" + gitHubParsed.getRepoName() : gitHubParsed.getRepoName());

            boolean exists = (!MapUtils.isEmpty(prMap) && prMap.get(pull.getUpdatedAt()) != null) && (Objects.equals(prMap.get(pull.getUpdatedAt()), pull.getNumber()));
            if(!exists) {
                localCount++;
                pullRequests.add(pull);
            }
            if(pull.getUpdatedAt() < (System.currentTimeMillis() - (long) apiSettings.getGithubSyncSettings().getFirstRunHistoryDays()*ONE_DAY_IN_MILLISECONDS)) {
                paging.setLastPage(true);
                break;
            }
        }
        paging.setCurrentCount(localCount);
        return paging;
    }

    private int asInt(JSONObject json, String key) {
        return NumberUtils.toInt(str(json, key));
    }

    private boolean isNewCommit(GitHubRepo repo, Commit commit) {
        return commitRepository.findByCollectorItemIdAndScmRevisionNumber(
                repo.getId(), commit.getScmRevisionNumber()) == null;
    }


    /**
     * Process commits
     *
     * @param repo
     * @return count added
     */
    private int processCommits(GitHubRepo repo) {
        int count = 0;
        Long existingCount = commitRepository.countCommitsByCollectorItemId(repo.getId());
        if (existingCount == 0) {
            List<Commit> newCommits = getCommits();
            newCommits.forEach(c -> c.setCollectorItemId(repo.getId()));
            Iterable<Commit> saved = commitRepository.save(newCommits);
            count = saved != null ? Lists.newArrayList(saved).size() : 0;
        } else {
            Collection<Commit> nonDupCommits = getCommits().stream()
                    .<Map<String, Commit>>collect(HashMap::new, (m, c) -> m.put(c.getScmRevisionNumber(), c), Map::putAll)
                    .values();
            for (Commit commit : nonDupCommits) {
                LOG.debug(commit.getTimestamp() + ":::" + commit.getScmCommitLog());
                if (isNewCommit(repo, commit)) {
                    commit.setCollectorItemId(repo.getId());
                    commitRepository.save(commit);
                    count++;
                }
            }
        }
        LOG.info("-- Saved Commits = " + count);
        return count;
    }


    private int processPRorIssueList(GitHubRepo repo, List<GitRequest> existingList, String type) {
        int count = 0;
        boolean isPull = "pull".equalsIgnoreCase(type);
        List<GitRequest> entries = isPull ? getPulls() : getIssues();

        if (CollectionUtils.isEmpty(entries)) return 0;

        List<String> pullNumbers = new ArrayList<>();

        for (GitRequest entry : entries) {
            Optional<GitRequest> existingOptional = existingList.stream().filter(r -> Objects.equals(r.getNumber(), entry.getNumber())).findFirst();
            GitRequest existing = existingOptional.orElse(null);
            if (isPull) {
                if (pullNumbers.size() < 10) {
                    pullNumbers.add(entry.getNumber());
                } else if (pullNumbers.size() == 10) {
                    pullNumbers.add("...");
                }
            }
            if (existing == null) {
                entry.setCollectorItemId(repo.getId());
                count++;
            } else {
                entry.setId(existing.getId());
                entry.setCollectorItemId(repo.getId());
            }
            gitRequestRepository.save(entry);
        }
        LOG.info("-- Saved " + type + ":" + count + (isPull ? (" " + pullNumbers) : ""));
        return count;
    }

    public long getRepoOffsetTime(GitHubRepo repo) {
        List<Commit> allPrCommits = new ArrayList<>();
        pullRequests.stream().filter(pr -> "merged".equalsIgnoreCase(pr.getState())).forEach(pr -> {
            allPrCommits.addAll(pr.getCommits().stream().collect(Collectors.toList()));
        });
        if (CollectionUtils.isEmpty(allPrCommits)) {
            return 0;
        }
        Commit oldestPrCommit = allPrCommits.stream().min(Comparator.comparing(Commit::getScmCommitTimestamp)).orElse(null);
        return (oldestPrCommit != null) ? oldestPrCommit.getScmCommitTimestamp() : 0;
    }


    private void processOrphanCommits(GitHubRepo repo) {
        long refTime = Math.min(System.currentTimeMillis() - apiSettings.getGithubSyncSettings().getCommitPullSyncTime(), getRepoOffsetTime(repo));
        List<Commit> orphanCommits = commitRepository.findCommitsByCollectorItemIdAndTimestampAfterAndPullNumberIsNull(repo.getId(), refTime);
        List<GitRequest> pulls = gitRequestRepository.findByCollectorItemIdAndMergedAtIsBetween(repo.getId(), refTime, System.currentTimeMillis());
        orphanCommits = CommitPullMatcher.matchCommitToPulls(orphanCommits, pulls);
        List<Commit> orphanSaveList = orphanCommits.stream().filter(c -> !StringUtils.isEmpty(c.getPullNumber())).collect(Collectors.toList());
        orphanSaveList.forEach(c -> LOG.info("Updating orphan " + c.getScmRevisionNumber() + " " +
                new DateTime(c.getScmCommitTimestamp()).toString("yyyy-MM-dd hh:mm:ss.SSa") + " with pull " + c.getPullNumber()));
        commitRepository.save(orphanSaveList);
    }


    @SuppressWarnings("PMD.NPathComplexity")


    private CommitType getCommitType(int parentSize, String commitMessage) {
        if (parentSize > 1) return CommitType.Merge;
        //if (settings.getNotBuiltCommits() == null) return CommitType.New;
        if (!CollectionUtils.isEmpty(commitExclusionPatterns)) {
            for (Pattern pattern : commitExclusionPatterns) {
                if (pattern.matcher(commitMessage).matches()) {
                    return CommitType.NotBuilt;
                }
            }
        }
        return CommitType.New;
    }

    private GitHubPaging processCommits(JSONObject refObject, GitHubRepo repo) {
        GitHubPaging paging = new GitHubPaging();
        paging.setLastPage(true); //initialize

        if (refObject == null) return paging;

        JSONObject target = (JSONObject) refObject.get("target");

        if (target == null) return paging;

        JSONObject history = (JSONObject) target.get("history");

        JSONObject pageInfo = (JSONObject) history.get("pageInfo");

        paging.setCursor(str(pageInfo, "endCursor"));
        paging.setLastPage(!(Boolean) pageInfo.get("hasNextPage"));

        JSONArray edges = (JSONArray) history.get("edges");

        if (CollectionUtils.isEmpty(edges)) {
            return paging;
        }

        paging.setCurrentCount(edges.size());

        for (Object o : edges) {
            JSONObject node = (JSONObject) ((JSONObject) o).get("node");
            JSONObject authorJSON = (JSONObject) node.get("author");
            JSONObject authorUserJSON = (JSONObject) authorJSON.get("user");

            String sha = str(node, "oid");
            int changedFiles = NumberUtils.toInt(str(node, "changedFiles"));
            int deletions = NumberUtils.toInt(str(node, "deletions"));
            int additions = NumberUtils.toInt(str(node, "additions"));
            String message = str(node, "message");
            String authorName = str(authorJSON, "name");
            String authorLogin = authorUserJSON == null ? "unknown" : str(authorUserJSON, "login");
            Commit commit = new Commit();
            commit.setTimestamp(System.currentTimeMillis());
            commit.setScmUrl(repo.getRepoUrl());
            commit.setScmBranch(repo.getBranch());
            commit.setScmRevisionNumber(sha);
            commit.setScmAuthor(authorName);
            commit.setScmAuthorLogin(authorLogin);
            String authorType = getAuthorType(repo, authorLogin);
            if (!StringUtils.isEmpty(authorType)) {
                commit.setScmAuthorType(authorType);
            }
            String authorLDAPDN = getLDAPDN(repo, authorLogin);
            if (!StringUtils.isEmpty(authorLDAPDN)) {
                commit.setScmAuthorLDAPDN(authorLDAPDN);
            }
            commit.setScmCommitLog(message);
            commit.setScmCommitTimestamp(getTimeStampMills(str(authorJSON, "date")));
            commit.setNumberOfChanges((long) changedFiles + deletions + additions);
            List<String> parentShas = getParentShas(node);
            commit.setScmParentRevisionNumbers(parentShas);
            commit.setFirstEverCommit(CollectionUtils.isEmpty(parentShas));
            commit.setType(getCommitType(CollectionUtils.size(parentShas), message));
            commits.add(commit);
        }
        return paging;
    }


    private GitHubPaging processIssues(JSONObject issueObject, GitHubParsed gitHubParsed, Map<Long, String> issuesMap, long historyTimeStamp) {
        GitHubPaging paging = new GitHubPaging();
        paging.setLastPage(true);

        if (issueObject == null) return paging;

        paging.setTotalCount(asInt(issueObject, "totalCount"));
        JSONObject pageInfo = (JSONObject) issueObject.get("pageInfo");
        paging.setCursor(str(pageInfo, "endCursor"));
        paging.setLastPage(!(Boolean) pageInfo.get("hasNextPage"));
        JSONArray edges = getArray(issueObject, "edges");

        if (CollectionUtils.isEmpty(edges)) {
            return paging;
        }

        int localCount = 0;
        for (Object o : edges) {
            JSONObject node = (JSONObject) ((JSONObject) o).get("node");
            if (node == null) break;

            String message = str(node, "title");
            String number = str(node, "number");

            JSONObject userObject = (JSONObject) node.get("author");
            String name = str(userObject, "login");
            String created = str(node, "createdAt");
            String updated = str(node, "updatedAt");
            long createdTimestamp = new DateTime(created).getMillis();
            long updatedTimestamp = new DateTime(updated).getMillis();

            GitRequest issue = new GitRequest();
            String state = str(node, "state");

            issue.setClosedAt(0);
            issue.setResolutiontime(0);
            issue.setMergedAt(0);
            if (Objects.equals("CLOSED", state)) {
                //ideally should be checking closedAt field. But it's not yet available in graphQL schema
                issue.setScmCommitTimestamp(updatedTimestamp);
                issue.setClosedAt(updatedTimestamp);
                issue.setMergedAt(updatedTimestamp);
                issue.setResolutiontime((updatedTimestamp - createdTimestamp));
            }
            issue.setUserId(name);
            issue.setScmUrl(gitHubParsed.getUrl());
            issue.setTimestamp(System.currentTimeMillis());
            issue.setScmRevisionNumber(number);
            issue.setNumber(number);
            issue.setScmCommitLog(message);
            issue.setCreatedAt(createdTimestamp);
            issue.setUpdatedAt(updatedTimestamp);

            issue.setNumber(number);
            issue.setRequestType("issue");
            if (Objects.equals("CLOSED", state)) {
                issue.setState("closed");
            } else {
                issue.setState("open");
            }
            issue.setOrgName(gitHubParsed.getOrgName());
            issue.setRepoName(gitHubParsed.getRepoName());

            boolean stop = (issue.getUpdatedAt() < historyTimeStamp) ||
                    ((!MapUtils.isEmpty(issuesMap) && issuesMap.get(issue.getUpdatedAt()) != null) && (Objects.equals(issuesMap.get(issue.getUpdatedAt()), issue.getNumber())));
            if (stop) {
                paging.setLastPage(true);
                LOG.debug("------ Stopping issue processing. History check is met OR Found matching entry in existing issues. Issue#" + issue.getNumber());
                break;
            } else {
                //add to the list
                issues.add(issue);
                localCount++;
            }
        }
        paging.setCurrentCount(localCount);
        return paging;
    }

    private ResponseEntity<String> makeRestCallGet(String url) throws RestClientException {
        // Basic Auth only.
        // This handles the case when settings.getPersonalAccessToken() is empty
        return restClient.makeRestCallGet(url, "token", apiSettings.getGithubSyncSettings().getToken());
    }


    public void getUser(GitHubRepo repo, String user) {
        if (StringUtils.isEmpty(user)) return;
        //This is weird. Github does replace the _ in commit author with - in the user api!!!
        String formattedUser = user.replace("_", "-");
        String repoUrl = (String) repo.getOptions().get("url");
        try {
            GitHubParsed gitHubParsed = new GitHubParsed(repoUrl);
            String apiUrl = gitHubParsed.getBaseApiUrl();
            String queryUrl = apiUrl.concat("users/").concat(formattedUser);
            ResponseEntity<String> response = makeRestCallGet(queryUrl);
            JSONObject jsonObject = parseAsObject(response);
            String ldapDN = str(jsonObject, "ldap_dn");
            String authorTypeStr = str(jsonObject, "type");
            if (StringUtils.isNotEmpty(ldapDN)) {
                ldapMap.put(user, ldapDN);
            }
            if (StringUtils.isNotEmpty(authorTypeStr)) {
                authorTypeMap.put(user, authorTypeStr);
            }
        } catch (MalformedURLException | HygieiaException | RestClientException e) {
            LOG.error("Error getting LDAP_DN For user " + user, e);
        }
    }

    public String getLDAPDN(GitHubRepo repo, String user) {
        if (StringUtils.isEmpty(user) || "unknown".equalsIgnoreCase(user)) return null;
        if (ldapMap == null) { ldapMap = new HashMap<>(); }
        //This is weird. Github does replace the _ in commit author with - in the user api!!!
        String formattedUser = user.replace("_", "-");
        if(ldapMap.containsKey(formattedUser)) {
            return ldapMap.get(formattedUser);
        }
        this.getUser(repo, formattedUser);
        return ldapMap.get(formattedUser);
    }

    public String getAuthorType(GitHubRepo repo, String user) {
        if (StringUtils.isEmpty(user) || "unknown".equalsIgnoreCase(user)) return null;
        if (authorTypeMap == null) { authorTypeMap = new HashMap<>(); }
        //This is weird. Github does replace the _ in commit author with - in the user api!!!
        String formattedUser = user.replace("_", "-");
        if(authorTypeMap.containsKey(formattedUser)) {
            return authorTypeMap.get(formattedUser);
        }
        this.getUser(repo, formattedUser);
        return authorTypeMap.get(formattedUser);
    }

    private List<Comment> getComments(GitHubRepo repo, JSONObject commentsJSON) throws RestClientException {

        List<Comment> comments = new ArrayList<>();
        if (commentsJSON == null) {
            return comments;
        }
        JSONArray nodes = getArray(commentsJSON, "nodes");
        if (CollectionUtils.isEmpty(nodes)) {
            return comments;
        }
        for (Object n : nodes) {
            JSONObject node = (JSONObject) n;
            Comment comment = new Comment();
            comment.setBody(str(node, "bodyText"));
            comment.setUser(str((JSONObject) node.get("author"), "login"));
            String userType = getAuthorType(repo, comment.getUser());
            if (!StringUtils.isEmpty(userType)) {
                comment.setUserType(userType);
            }
            String userLDAPDN = getLDAPDN(repo, comment.getUser());
            if (!StringUtils.isEmpty(userLDAPDN)) {
                comment.setUserLDAPDN(userLDAPDN);
            }
            comment.setCreatedAt(getTimeStampMills(str(node, "createdAt")));
            comment.setUpdatedAt(getTimeStampMills(str(node, "updatedAt")));
            comment.setStatus(str(node, "state"));
            comments.add(comment);
        }
        return comments;
    }

    @SuppressWarnings({"PMD.NPathComplexity"})
    private List<Commit> getPRCommits(GitHubRepo repo, JSONObject commits, GitRequest pull) {
        List<Commit> prCommits = new ArrayList<>();

        if (commits == null) {
            return prCommits;
        }

        JSONArray nodes = (JSONArray) commits.get("nodes");
        if (CollectionUtils.isEmpty(nodes)) {
            return prCommits;
        }
        JSONObject lastCommitStatusObject = null;
        long lastCommitTime = 0L;
        for (Object n : nodes) {
            JSONObject c = (JSONObject) n;
            JSONObject commit = (JSONObject) c.get("commit");
            Commit newCommit = new Commit();
            newCommit.setScmRevisionNumber(str(commit, "oid"));
            newCommit.setScmCommitLog(str(commit, "message"));
            JSONObject author = (JSONObject) commit.get("author");
            JSONObject authorUserJSON = (JSONObject) author.get("user");
            newCommit.setScmAuthor(str(author, "name"));
            newCommit.setScmAuthorLogin(authorUserJSON == null ? "unknown" : str(authorUserJSON, "login"));
            String authorLDAPDN = "unknown".equalsIgnoreCase(newCommit.getScmAuthorLogin()) ? null : getLDAPDN(repo, newCommit.getScmAuthorLogin());
            String authorType = getAuthorType(repo, newCommit.getScmAuthorLogin());
            if (!StringUtils.isEmpty(authorType)) {
                newCommit.setScmAuthorType(authorType);
            }
            if (!StringUtils.isEmpty(authorLDAPDN)) {
                newCommit.setScmAuthorLDAPDN(authorLDAPDN);
            }
            newCommit.setScmCommitTimestamp(getTimeStampMills(str(author, "date")));
            JSONObject statusObj = (JSONObject) commit.get("status");

            if (statusObj != null) {
                if (lastCommitTime <= newCommit.getScmCommitTimestamp()) {
                    lastCommitTime = newCommit.getScmCommitTimestamp();
                    lastCommitStatusObject = statusObj;
                }

                if (Objects.equals(newCommit.getScmRevisionNumber(), pull.getHeadSha())) {
                    List<CommitStatus> commitStatuses = getCommitStatuses(statusObj);
                    if (!CollectionUtils.isEmpty(commitStatuses)) {
                        pull.setCommitStatuses(commitStatuses);
                    }
                }
            }
            int changedFiles = NumberUtils.toInt(str(commit, "changedFiles"));
            int deletions = NumberUtils.toInt(str(commit, "deletions"));
            int additions = NumberUtils.toInt(str(commit, "additions"));
            newCommit.setNumberOfChanges((long) changedFiles + deletions + additions);
            prCommits.add(newCommit);
        }

        if (StringUtils.isEmpty(pull.getHeadSha()) || CollectionUtils.isEmpty(pull.getCommitStatuses())) {
            List<CommitStatus> commitStatuses = getCommitStatuses(lastCommitStatusObject);
            if (!CollectionUtils.isEmpty(commitStatuses)) {
                pull.setCommitStatuses(commitStatuses);
            }
        }
        return prCommits;
    }

    private List<CommitStatus> getCommitStatuses(JSONObject statusObject) throws RestClientException {

        Map<String, CommitStatus> statuses = new HashMap<>();

        if (statusObject == null) {
            return new ArrayList<>();
        }

        JSONArray contexts = (JSONArray) statusObject.get("contexts");

        if (CollectionUtils.isEmpty(contexts)) {
            return new ArrayList<>();
        }
        for (Object ctx : contexts) {
            String ctxStr = str((JSONObject) ctx, "context");
            if ((ctxStr != null) && !statuses.containsKey(ctxStr)) {
                CommitStatus status = new CommitStatus();
                status.setContext(ctxStr);
                status.setDescription(str((JSONObject) ctx, "description"));
                status.setState(str((JSONObject) ctx, "state"));
                statuses.put(ctxStr, status);
            }
        }
        return new ArrayList<>(statuses.values());
    }

    private List<Review> getReviews(GitHubRepo repo, JSONObject reviewObject) throws RestClientException {

        List<Review> reviews = new ArrayList<>();

        if (reviewObject == null) {
            return reviews;
        }

        JSONArray nodes = (JSONArray) reviewObject.get("nodes");

        if (CollectionUtils.isEmpty(nodes)) {
            return reviews;
        }

        for (Object n : nodes) {
            JSONObject node = (JSONObject) n;
            Review review = new Review();
            review.setState(str(node, "state"));
            review.setBody(str(node, "bodyText"));
            JSONObject authorObj = (JSONObject) node.get("author");
            review.setAuthor(str(authorObj, "login"));
            String authorType = getAuthorType(repo, review.getAuthor());
            if (!StringUtils.isEmpty(authorType)) {
                review.setAuthorType(authorType);
            }
            String authorLDAPDN = getLDAPDN(repo, review.getAuthor());
            if (!StringUtils.isEmpty(authorLDAPDN)) {
                review.setAuthorLDAPDN(authorLDAPDN);
            }
            review.setCreatedAt(getTimeStampMills(str(node, "createdAt")));
            review.setUpdatedAt(getTimeStampMills(str(node, "updatedAt")));
            reviews.add(review);
        }
        return reviews;
    }

    private MergeEvent getMergeEvent(GitHubRepo repo, GitRequest pr, JSONObject timelineObject) throws RestClientException {
        if (timelineObject == null) {
            return null;
        }
        JSONArray edges = (JSONArray) timelineObject.get("edges");
        if (CollectionUtils.isEmpty(edges)) {
            return null;
        }

        for (Object e : edges) {
            JSONObject edge = (JSONObject) e;
            JSONObject node = (JSONObject) edge.get("node");
            if (node != null) {
                String typeName = str(node, "__typename");
                if ("MergedEvent".equalsIgnoreCase(typeName)) {
                    JSONObject timelinePrNbrObj = (JSONObject) node.get("pullRequest");
                    if (timelinePrNbrObj != null && pr.getNumber().equals(str(timelinePrNbrObj, "number"))) {
                        MergeEvent mergeEvent = new MergeEvent();
                        JSONObject commit = (JSONObject) node.get("commit");
                        mergeEvent.setMergeSha(str(commit, "oid"));
                        mergeEvent.setMergedAt(getTimeStampMills(str(node, "createdAt")));
                        JSONObject author = (JSONObject) node.get("actor");
                        if (author != null) {
                            mergeEvent.setMergeAuthor(str(author, "login"));
                            String mergeAuthorLDAPDN = getLDAPDN(repo, mergeEvent.getMergeAuthor());
                            if (!StringUtils.isEmpty(mergeAuthorLDAPDN)) {
                                mergeEvent.setMergeAuthorLDAPDN(mergeAuthorLDAPDN);
                            }
                        }
                        return mergeEvent;
                    }
                }
            }
        }
        return null;
    }

    private void connectCommitToPulls() {
        commits = CommitPullMatcher.matchCommitToPulls(commits, pullRequests);
    }


    /**
     * Get run date based off of firstRun boolean
     *
     * @param repo
     * @param firstRun
     * @return
     */
    private String getRunDate(GitHubRepo repo, boolean firstRun, boolean missingCommits) {
        if (missingCommits) {
            long repoOffsetTime = getRepoOffsetTime(repo);
            if (repoOffsetTime > 0) {
                return getDate(new DateTime(getRepoOffsetTime(repo)), 0, apiSettings.getGithubSyncSettings().getOffsetMinutes()).toString();
            } else {
                return getDate(new DateTime(repo.getLastUpdated()), 0, apiSettings.getGithubSyncSettings().getOffsetMinutes()).toString();
            }
        }
        if (firstRun) {
            int firstRunDaysHistory = apiSettings.getGithubSyncSettings().getFirstRunHistoryDays();
            if (firstRunDaysHistory > 0) {
                return getDate(new DateTime(), firstRunDaysHistory, 0).toString();
            } else {
                return getDate(new DateTime(), 14, 0).toString();
            }
        } else {
            return getDate(new DateTime(repo.getLastUpdated()), 0, apiSettings.getGithubSyncSettings().getOffsetMinutes()).toString();
        }
    }


    private GitHubPaging isThereNewPRorIssue(GitHubParsed gitHubParsed, GitHubRepo repo, String decryptedPassword, String personalAccessToken, Map<Long, String> existingMap, String type, boolean firstRun) throws MalformedURLException, HygieiaException {

        GitHubPaging paging = new GitHubPaging();
        paging.setLastPage(true);

        if (firstRun) {
            paging.setLastPage(false);
            return paging;
        }

        String queryString = "pull".equalsIgnoreCase(type) ? GithubGraphQLQuery.QUERY_NEW_PR_CHECK : GithubGraphQLQuery.QUERY_NEW_ISSUE_CHECK;
        JSONObject query = new JSONObject();
        JSONObject variableJSON = new JSONObject();
        variableJSON.put("owner", gitHubParsed.getOrgName());
        variableJSON.put("name", gitHubParsed.getRepoName());
        if ("pull".equalsIgnoreCase(type)) {
            variableJSON.put("branch", repo.getBranch());
        }
        query.put("query", queryString);
        query.put("variables", variableJSON.toString());

        JSONObject data = getDataFromRestCallPost(gitHubParsed, repo, decryptedPassword, personalAccessToken, query);

        if (data == null) return paging;
        JSONObject repository = (JSONObject) data.get("repository");
        JSONObject requestObject = "pull".equalsIgnoreCase(type) ? (JSONObject) repository.get("pullRequests") : (JSONObject) repository.get("issues");
        if (requestObject == null) return paging;

        JSONArray edges = getArray(requestObject, "edges");
        if (CollectionUtils.isEmpty(edges)) return paging;

        int index = 0;
        for (Object o : edges) {
            JSONObject node = (JSONObject) ((JSONObject) o).get("node");
            if (node == null) return paging;
            String updated = str(node, "updatedAt");
            long updatedTimestamp = getTimeStampMills(updated);
            String number = str(node, "number");
            boolean stop =
                    ((!MapUtils.isEmpty(existingMap) && existingMap.get(updatedTimestamp) != null) && (Objects.equals(existingMap.get(updatedTimestamp), number)));
            if (stop) {
                LOG.debug("------ Skipping issue processing. History check is met OR Found matching entry in existing issues. Issue#" + number);
            }
            index++;
        }
        paging.setLastPage(index == 0);
        return paging;
    }

    public void fireGraphQL(GitHubRepo repo, boolean firstRun, Map<Long, String> existingPRMap, Map<Long, String> existingIssueMap) throws RestClientException, MalformedURLException, HygieiaException, NullPointerException {
        // format URL
        String repoUrl = (String) repo.getOptions().get("url");
        GitHubParsed gitHubParsed = new GitHubParsed(repoUrl);

        commits = new LinkedList<>();
        pullRequests = new LinkedList<>();
        issues = new LinkedList<>();
        ldapMap = new HashMap<>();
        authorTypeMap = new HashMap<>();

        long historyTimeStamp = getTimeStampMills(getRunDate(repo, firstRun, false));

        String decryptedPassword = decryptString(repo.getPassword(), apiSettings.getKey());
        String personalAccessToken = (String) repo.getOptions().get("personalAccessToken");
        //  String decryptPersonalAccessToken = decryptString(personalAccessToken, apiSettings.getKey());
        String token = apiSettings.getGithubSyncSettings().getToken();

        boolean alldone = false;

        GitHubPaging dummyPRPaging = isThereNewPRorIssue(gitHubParsed, repo, decryptedPassword, token, existingPRMap, "pull", firstRun);
        GitHubPaging dummyIssuePaging = isThereNewPRorIssue(gitHubParsed, repo, decryptedPassword, token, existingIssueMap, "issue", firstRun);
        GitHubPaging dummyCommitPaging = new GitHubPaging();
        dummyCommitPaging.setLastPage(false);

        int loopCount = 1;
        JSONObject query = buildQuery(true, firstRun, false, gitHubParsed, repo, dummyCommitPaging, dummyPRPaging, dummyIssuePaging);
        while (!alldone) {
            LOG.debug("Executing loop " + loopCount + " for " + gitHubParsed.getOrgName() + "/" + gitHubParsed.getRepoName());
            JSONObject data = getDataFromRestCallPost(gitHubParsed, repo, decryptedPassword, token, query);

            if (data != null) {
                JSONObject repository = (JSONObject) data.get("repository");

                GitHubPaging pullPaging = processPullRequest((JSONObject) repository.get("pullRequests"), repo, existingPRMap, historyTimeStamp);
                LOG.debug("--- Processed " + pullPaging.getCurrentCount() + " of total " + pullPaging.getTotalCount() + " pull requests");

                GitHubPaging issuePaging = processIssues((JSONObject) repository.get("issues"), gitHubParsed, existingIssueMap, historyTimeStamp);
                LOG.debug("--- Processed " + issuePaging.getCurrentCount() + " of total " + issuePaging.getTotalCount() + " issues");

                GitHubPaging commitPaging = processCommits((JSONObject) repository.get("ref"), repo);
                LOG.debug("--- Processed " + commitPaging.getCurrentCount() + " commits");

                alldone = pullPaging.isLastPage() && commitPaging.isLastPage() && issuePaging.isLastPage();

                query = buildQuery(false, firstRun, false, gitHubParsed, repo, commitPaging, pullPaging, issuePaging);

                loopCount++;
            }
        }

        LOG.info("-- Collected " + commits.size() + " Commits, " + pullRequests.size() + " Pull Requests, " + issues.size() + " Issues since " + getRunDate(repo, firstRun, false));

        if (firstRun) {
            connectCommitToPulls();
            return;
        }

        List<GitRequest> allMergedPrs = pullRequests.stream().filter(pr -> "merged".equalsIgnoreCase(pr.getState())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allMergedPrs)) {
            connectCommitToPulls();
            return;
        }
        //find missing commits for subsequent runs
        alldone = false;

        dummyPRPaging = new GitHubPaging();
        dummyPRPaging.setLastPage(true);
        dummyIssuePaging = new GitHubPaging();
        dummyIssuePaging.setLastPage(true);
        dummyCommitPaging = new GitHubPaging();
        dummyCommitPaging.setLastPage(false);

        query = buildQuery(true, false, true, gitHubParsed, repo, dummyCommitPaging, dummyPRPaging, dummyIssuePaging);

        loopCount = 1;
        int missingCommitCount = 0;
        while (!alldone) {
            JSONObject data = getDataFromRestCallPost(gitHubParsed, repo, decryptedPassword, token, query);
            if (data != null) {
                JSONObject repository = (JSONObject) data.get("repository");

                GitHubPaging commitPaging = processCommits((JSONObject) repository.get("ref"), repo);
                LOG.debug("--- Processed " + commitPaging.getCurrentCount() + " commits");

                alldone = commitPaging.isLastPage();
                missingCommitCount += commitPaging.getCurrentCount();

                query = buildQuery(false, firstRun, true, gitHubParsed, repo, commitPaging, dummyPRPaging, dummyIssuePaging);

                loopCount++;
            }
        }
        LOG.info("-- Collected " + missingCommitCount + " Missing Commits, since " + getRunDate(repo, firstRun, true));

        connectCommitToPulls();
    }
}