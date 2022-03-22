package com.capitalone.dashboard.webhook.github;

import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.client.RestOperationsSupplier;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.CommitType;
import com.capitalone.dashboard.model.GitHubCollector;
import com.capitalone.dashboard.model.GitRequest;
import com.capitalone.dashboard.model.webhook.github.GitHubParsed;
import com.capitalone.dashboard.model.webhook.github.GitHubRepo;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CommitRepository;
import com.capitalone.dashboard.repository.GitRequestRepository;
import com.capitalone.dashboard.repository.UserEntitlementsRepository;
import com.capitalone.dashboard.service.CollectorService;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.webhook.settings.GitHubWebHookSettings;
import com.capitalone.dashboard.webhook.settings.WebHookSettings;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitHubCommitV3Test {
    private static final Log LOG = LogFactory.getLog(GitHubCommitV3Test.class);

    @Mock private CollectorService collectorService;
    @Mock private CommitRepository commitRepository;
    @Mock private GitRequestRepository gitRequestRepository;
    @Mock private CollectorItemRepository collectorItemRepository;
    @Mock private UserEntitlementsRepository userEntitlementsRepository;
    @Mock private BaseCollectorRepository<GitHubCollector> collectorRepository;
    @Mock private ApiSettings apiSettings;
    @Mock private RestOperationsSupplier restOperationsSupplier;

    private GitHubCommitV3 gitHubCommitV3;
    private RestClient restClient;

    @Before
    public void init() {
        RestClient restClientTemp = new RestClient(restOperationsSupplier);
        restClient = Mockito.spy(restClientTemp);
        gitHubCommitV3 = new GitHubCommitV3 (collectorService, restClient, commitRepository, gitRequestRepository, collectorItemRepository, userEntitlementsRepository, apiSettings, collectorRepository);
    }

    @Test
    public void getCommitsTest() throws HygieiaException, ParseException {
        GitHubCommitV3 gitHubCommitV3 = Mockito.spy(this.gitHubCommitV3);

        String repoUrl = "http://hostName/OrgName/OwnerName/RepoName";
        String branch = "master";

        GitHubCollector collector = makeCollector();
        CollectorItem collectorItem = gitHubCommitV3.buildCollectorItem(collector.getId(), repoUrl, branch);
        String collectorItemId = createGuid("0123456789abcdee");
        collectorItem.setId(new ObjectId(collectorItemId));

        List<Map> commitsMapList = makeCommitsList();

        when(collectorRepository.save(any(GitHubCollector.class))).thenReturn(collector);
        when(gitHubCommitV3.buildCollectorItem(anyObject(), anyString(), anyString())).thenReturn(collectorItem);
        when(collectorService.createCollectorItem(anyObject())).thenReturn(collectorItem);
        try {
            when(gitHubCommitV3.getCollectorItem(anyString(), anyString())).thenReturn(collectorItem);
        } catch (HygieiaException e) {
            LOG.info(e.getMessage());
        }
        when(apiSettings.getWebHook()).thenReturn(makeWebHookSettings());
        try {
            when(restClient.parseAsObject(anyObject())).thenReturn(getData("GithubWebhook/commit-node-2.json"));
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        List<Commit> commitsList = null;
        JSONObject senderObj = new JSONObject();
        senderObj.put("senderLogin", "senderLogin");
        senderObj.put("authorLDAPDN", "authorLDAPDN");
        try {
            commitsList = gitHubCommitV3.getCommits(commitsMapList, repoUrl, branch, "token", senderObj);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        int size = commitsList.size();

        Assert.assertEquals(2, size);

        Commit commit1 = commitsList.get(0);
        Assert.assertEquals(repoUrl, commit1.getScmUrl());
        Assert.assertEquals("master", commit1.getScmBranch());
        Assert.assertEquals("commit1ID", commit1.getScmRevisionNumber());
        Assert.assertEquals("GitHub WebHook Commit 1", commit1.getScmCommitLog());
        Assert.assertEquals("author1Name", commit1.getScmAuthor());
        Assert.assertEquals(7, commit1.getNumberOfChanges());
        Assert.assertEquals(collectorItemId, commit1.getCollectorItemId().toString());
        verify(restClient, times(1)).makeRestCallPost(anyObject(), anyString(), anyString(), anyObject());

        Commit commit2 = commitsList.get(1);
        Assert.assertEquals(repoUrl, commit2.getScmUrl());
        Assert.assertEquals("master", commit2.getScmBranch());
        Assert.assertEquals("commit2ID", commit2.getScmRevisionNumber());
        Assert.assertEquals("GitHub WebHook Commit 2", commit2.getScmCommitLog());
        Assert.assertEquals("author2Name", commit2.getScmAuthor());
        Assert.assertEquals(3, commit2.getNumberOfChanges());
        Assert.assertEquals(collectorItemId, commit2.getCollectorItemId().toString());
    }

    @Test
    public void setCollectorItemIdExistingCommitTest() throws MalformedURLException, HygieiaException {
        GitHubCommitV3 gitHubCommitV3 = Mockito.spy(this.gitHubCommitV3);

        List<Commit> commitList = new ArrayList<>();
        Commit existingCommit = new Commit();
        commitList.add(existingCommit);
        String id = createGuid("0123456789abcdef");
        existingCommit.setId(new ObjectId(id));

        String collectorItemId = createGuid("0123456789abcdee");
        existingCommit.setCollectorItemId(new ObjectId(collectorItemId));

        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(new ObjectId(collectorItemId));

        Commit newCommit = new Commit();

        when(commitRepository.findAllByScmRevisionNumberAndScmUrlIgnoreCaseAndScmBranchIgnoreCaseOrderByTimestampAsc(anyString(), anyString(), anyString())).thenReturn(commitList);
        when(collectorService.getCollectorItem(existingCommit.getCollectorItemId())).thenReturn(collectorItem);

        gitHubCommitV3.setCollectorItemId(newCommit);

        Assert.assertEquals(new ObjectId(id), newCommit.getId());
        Assert.assertEquals(new ObjectId(collectorItemId), newCommit.getCollectorItemId());
        Assert.assertTrue(collectorItem.isPushed());
    }

    @Test
    public void setCollectorItemIdNewCommitTest() throws MalformedURLException, HygieiaException {
        GitHubCommitV3 gitHubCommitV3 = Mockito.spy(this.gitHubCommitV3);

        Commit newCommit = new Commit();
        String repoUrl = "http://hostName/orgName/repoName";
        String branch = "master";
        newCommit.setScmUrl(repoUrl);
        newCommit.setScmBranch(branch);

        GitHubCollector collector = makeCollector();
        CollectorItem collectorItem = gitHubCommitV3.buildCollectorItem(collector.getId(), repoUrl, branch);
        String collectorItemId = createGuid("0123456789abcdee");
        collectorItem.setId(new ObjectId(collectorItemId));

        when(commitRepository.findAllByScmRevisionNumberAndScmUrlIgnoreCaseAndScmBranchIgnoreCaseOrderByTimestampAsc(anyString(), anyString(), anyString())).thenReturn(null);
        when(collectorRepository.save(any(GitHubCollector.class))).thenReturn(collector);
        when(gitHubCommitV3.buildCollectorItem(anyObject(), anyString(), anyString())).thenReturn(collectorItem);
        when(collectorService.createCollectorItem(anyObject())).thenReturn(collectorItem);
        try {
            when(gitHubCommitV3.getCollectorItem(anyString(), anyString())).thenReturn(collectorItem);
        } catch (HygieiaException e) {
            LOG.info(e.getMessage());
        }

        gitHubCommitV3.setCollectorItemId(newCommit);

        Assert.assertEquals(new ObjectId(collectorItemId), newCommit.getCollectorItemId());
    }

    @Test
    public void checkForErrors() {
        JSONObject objectWithErrors = new JSONObject();
        JSONArray errors = new JSONArray();
        objectWithErrors.put("errors", errors);

        JSONObject error = new JSONObject();
        errors.add(error);

        Exception exception = null;
        try {
            gitHubCommitV3.checkForErrors(objectWithErrors);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull(exception);
    }

    @Test
    public void getCommitNodeTest() {
        JSONObject responseJsonObject = makeRepositoryResponseObject();

        GitHubParsed gitHubParsed = null;
        try {
            gitHubParsed = new GitHubParsed("http://hostName/ownerName/orgName/repoName");
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        try {
            when(restClient.parseAsObject(anyObject())).thenReturn(responseJsonObject);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        Object node = null;
        try {
            node = gitHubCommitV3.getCommitNode(gitHubParsed, "oid1", "token");
        } catch (Exception e){
            LOG.error(e.getMessage());
        }
        String sha = restClient.getString(node, "oid");

        Assert.assertEquals(sha, "oid1");
    }

    @Test
    public void getCommitTypeTest() {
        List<Pattern> commitExclusionPatterns = new ArrayList<>();
        List<String> notBuiltCommits = new ArrayList<>();
        notBuiltCommits.add("test1");
        notBuiltCommits.add("test2");
        notBuiltCommits.stream().map(regExStr -> Pattern.compile(regExStr, Pattern.CASE_INSENSITIVE)).forEach(commitExclusionPatterns::add);

        CommitType commitType = gitHubCommitV3.getCommitType(2, "commit message", new GitHubWebHookSettings(), commitExclusionPatterns);
        Assert.assertEquals(CommitType.Merge, commitType);

        commitType = gitHubCommitV3.getCommitType(1, "commit message", new GitHubWebHookSettings(), commitExclusionPatterns);
        Assert.assertEquals(CommitType.New, commitType);

        GitHubWebHookSettings gitHubWebHookSettings = new GitHubWebHookSettings();
        List<String> notBuiltCommitsList = new ArrayList<>();
        notBuiltCommitsList.add("some value");
        gitHubWebHookSettings.setNotBuiltCommits(notBuiltCommitsList);
        commitType = gitHubCommitV3.getCommitType(1, "test1", gitHubWebHookSettings, commitExclusionPatterns);
        Assert.assertEquals(CommitType.NotBuilt, commitType);
    }

    @Test
    public void setCommitPullNumbersForRebaseAndMergeCommitTest() {
        List<Commit> commitList = new ArrayList<>();
        Commit commit1 = new Commit();
        commitList.add(commit1);

        Commit commit2 = new Commit();
        commitList.add(commit2);
        commit2.setPullNumber("2");

        gitHubCommitV3.setCommitPullNumbersForRebaseAndMergeCommit(commitList);

        Assert.assertEquals("2", commit1.getPullNumber());

        commit1.setPullNumber(null);
        Commit commit3 = new Commit();
        commitList.add(commit3);
        commit2.setPullNumber("3");

        Assert.assertNull(commit1.getPullNumber());
    }

    @Test
    public void setCommitPullNumberTest() {
        Commit commit = new Commit();
        commit.setScmRevisionNumber("1");

        GitRequest pr = new GitRequest();
        pr.setNumber("2");

        when(gitRequestRepository.findByScmRevisionNumberOrScmMergeEventRevisionNumber(commit.getScmRevisionNumber())).thenReturn(pr);
        when(gitRequestRepository.findByCommitScmRevisionNumber(commit.getScmRevisionNumber())).thenReturn(null);

        gitHubCommitV3.setCommitPullNumber(commit);

        Assert.assertEquals("2", commit.getPullNumber());

        commit.setPullNumber(null);
        when(gitRequestRepository.findByScmRevisionNumberOrScmMergeEventRevisionNumber(commit.getScmRevisionNumber())).thenReturn(null);
        when(gitRequestRepository.findByCommitScmRevisionNumber(commit.getScmRevisionNumber())).thenReturn(pr);
        gitHubCommitV3.setCommitPullNumber(commit);

        Assert.assertEquals("2", commit.getPullNumber());

        commit.setPullNumber(null);
        when(gitRequestRepository.findByScmRevisionNumberOrScmMergeEventRevisionNumber(commit.getScmRevisionNumber())).thenReturn(null);
        when(gitRequestRepository.findByCommitScmRevisionNumber(commit.getScmRevisionNumber())).thenReturn(null);
        gitHubCommitV3.setCommitPullNumber(commit);

        Assert.assertNull(commit.getPullNumber());
    }

    @Test
    public void getRepositoryTokenTest() {
        GitHubCommitV3 gitHubCommitV3 = Mockito.spy(this.gitHubCommitV3);

        String scmUrl = "http://hostName/ownerName/orgName/repoName";

        GitHubCollector collector = makeCollector();

        List<CollectorItem> gitHubRepoList = new ArrayList<>();
        GitHubRepo repo1 = new GitHubRepo();
        gitHubRepoList.add(repo1);
        repo1.setPersonalAccessToken("1");

        GitHubRepo repo2 = new GitHubRepo();
        gitHubRepoList.add(repo2);
        repo1.setPersonalAccessToken("1");

        when(collectorRepository.save(any(GitHubCollector.class))).thenReturn(collector);
        when(gitHubCommitV3.getCollectorItemRepository().findAllByOptionNameValueAndCollectorIdsIn(anyString(), anyString(), anyObject())).thenReturn(gitHubRepoList);

        String result = gitHubCommitV3.getRepositoryToken(scmUrl);

        Assert.assertEquals("1", result);
    }

    @Test
    public void getParentShasTest() {
        JSONObject commitObject = new JSONObject();
        JSONObject parents = new JSONObject();
        commitObject.put("parents", parents);
        JSONArray nodes = new JSONArray();
        parents.put("nodes", nodes);
        JSONObject node1 = new JSONObject();
        nodes.add(node1);
        node1.put("oid", "node1Oid");
        JSONObject node2 = new JSONObject();
        nodes.add(node2);
        node2.put("oid", "node2Oid");

        List<String> parentShas = gitHubCommitV3.getParentShas(commitObject);
        Assert.assertEquals(2, parentShas.size());
    }

    @Test
    public void testRejectUnregisteredRepoCommitFail() throws Exception {
        GitHubCollector collector = makeCollector();

        when(collectorRepository.save(any(GitHubCollector.class))).thenReturn(collector);
        when(collectorService.createCollector(anyObject())).thenReturn(collector);
        when(gitHubCommitV3.getCollectorItemRepository().findRepoByUrlAndBranch(anyObject(), anyString(), anyString())).thenReturn(null);

        JSONObject commitPayload = getData("GithubWebhook/commit-payload-fail.json");

        String acutalResponse = gitHubCommitV3.process(commitPayload);
        String expectedResponse = "Repo: <https://github.com/chzhanpeng/WebhookTest> Branch: <dev> is not registered in Hygieia";
        Assert.assertEquals(expectedResponse, acutalResponse);
    }

    @Test
    public void testRejectUnregisteredRepoCommitPass() throws Exception {
        GitHubCollector collector = makeCollector();

        CollectorItem repo = makeCollectorItem("https://github.com/chzhanpeng/WebhookTest", "master");
        JSONObject commitNode = getData("GithubWebhook/commit-node-1.json");

        when(collectorRepository.save(any(GitHubCollector.class))).thenReturn(collector);
        when(gitHubCommitV3.getCollectorItemRepository().findRepoByUrlAndBranch(anyObject(), anyString(), anyString(), anyBoolean())).thenReturn(repo);
        when(gitHubCommitV3.getCollectorItemRepository().findRepoByUrlAndBranch(anyObject(), anyString(), anyString())).thenReturn(repo);
        when(apiSettings.getWebHook()).thenReturn(makeWebHookSettings());
        when(restClient.parseAsObject(anyObject())).thenReturn(commitNode);

        JSONObject commitPayload = getData("GithubWebhook/commit-payload.json");

        String acutalResponse = gitHubCommitV3.process(commitPayload);
        String expectedResponse = "Commits Processed Successfully";
        Assert.assertEquals(expectedResponse, acutalResponse);
    }

    private static String createGuid(String hex) {
        byte[]  bytes = new byte[12];
        new Random().nextBytes(bytes);

        char[] hexArray = hex.toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private GitHubCollector makeCollector() {
        GitHubCollector col = new GitHubCollector();
        col.setId(new ObjectId(createGuid("0123456789abcdef")));
        col.setName("GitHub");
        col.setCollectorType(CollectorType.SCM);
        col.setOnline(true);
        col.setEnabled(true);

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(GitHubRepo.REPO_URL, "");
        allOptions.put(GitHubRepo.BRANCH, "");
        allOptions.put(GitHubRepo.USER_ID, "");
        allOptions.put(GitHubRepo.PASSWORD, "");
        allOptions.put(GitHubRepo.PERSONAL_ACCESS_TOKEN, "");
        allOptions.put(GitHubRepo.TYPE, "");
        col.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(GitHubRepo.REPO_URL, "");
        uniqueOptions.put(GitHubRepo.BRANCH, "");
        col.setUniqueFields(uniqueOptions);
        return col;
    }

    private JSONObject makeRepositoryResponseObject() {
        JSONObject responseJsonObject = new JSONObject();
        JSONObject data = new JSONObject();
        responseJsonObject.put("data", data);

        JSONObject repository = new JSONObject();
        data.put("repository", repository);

        JSONObject node = new JSONObject();
        node.put("oid", "oid1");

        repository.put("object", node);
        
        return responseJsonObject;
    }

    private List<Map> makeCommitsList() {
        List<Map> commitsList = new ArrayList<>();

        Map commitsMap1 = new HashMap();
        commitsList.add(commitsMap1);

        commitsMap1.put("id", "commit1ID");
        commitsMap1.put("message", "GitHub WebHook Commit 1");
        commitsMap1.put("timestamp", "2018-09-22T11:18:56-05:00");
        commitsMap1.put("url", "https://host/commit/commit1ID");

        List<Integer> modifiedList1 = new ArrayList<>();
        modifiedList1.add(1);
        commitsMap1.put("modified", modifiedList1);

        Map author1 = new HashMap();
        commitsMap1.put("author", author1);

        author1.put("name", "author1Name");
        author1.put("login", "senderLogin");

        commitsMap1.put("added",Arrays.asList("pom.xml", "cucumber.json", "Test.java"));
        commitsMap1.put("removed",Arrays.asList(".gitignore", "style.css"));
        commitsMap1.put("modified",Arrays.asList("Readme.md", "gulp.js"));

        Map commitsMap2 = new HashMap();
        commitsList.add(commitsMap2);

        commitsMap2.put("id", "commit2ID");
        commitsMap2.put("message", "GitHub WebHook Commit 2");
        commitsMap2.put("timestamp", "2018-09-22T11:18:56-05:00");
        commitsMap2.put("url", "https://host/commit/commit2ID");

        List<Integer> modifiedList2 = new ArrayList<>();
        modifiedList2.add(1);
        commitsMap2.put("modified", modifiedList2);

        Map author2 = new HashMap();
        commitsMap2.put("author", author2);

        author2.put("name", "author2Name");
        author2.put("login", "senderLogin");

        commitsMap2.put("added",null);
        commitsMap2.put("removed",Arrays.asList(".gitignore", "style.css"));
        commitsMap2.put("modified",Arrays.asList(""));



        return commitsList;
    }

    private WebHookSettings makeWebHookSettings() {
        WebHookSettings webHookSettings = new WebHookSettings();
        GitHubWebHookSettings gitHubWebHookSettings = new GitHubWebHookSettings();
        webHookSettings.setGitHub(gitHubWebHookSettings);

        gitHubWebHookSettings.setToken("c74782b3ca2b57a5230ae7812a");
        gitHubWebHookSettings.setCommitTimestampOffset(5);
        gitHubWebHookSettings.setUserAgent("GitHub-Hookshot");

        List<String> githubEnterpriseHosts = new ArrayList<>();
        gitHubWebHookSettings.setGithubEnterpriseHosts(githubEnterpriseHosts);
        githubEnterpriseHosts.add("github.com");

        return webHookSettings;
    }

    private CollectorItem makeCollectorItem(String repoUrl, String branch) {
        GitHubCollector collector = makeCollector();
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setCollectorId(collector.getId());
        collectorItem.setEnabled(true);
        collectorItem.setPushed(true);
        collectorItem.setLastUpdated(System.currentTimeMillis());
        collectorItem.getOptions().put("url", repoUrl);
        collectorItem.getOptions().put("branch", branch);
        return collectorItem;
    }

    private JSONObject getData(String filename) throws Exception {
        String data = IOUtils.toString(Resources.getResource(filename));
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(data);
    }

    private JSONArray getJsonArray(String filename) throws Exception {
        String data = IOUtils.toString(Resources.getResource(filename));
        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse(data);

    }

}
