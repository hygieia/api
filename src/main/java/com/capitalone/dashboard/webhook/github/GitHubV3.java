package com.capitalone.dashboard.webhook.github;

import com.capitalone.dashboard.model.AuthType;
import com.capitalone.dashboard.model.GitHubCollector;
import com.capitalone.dashboard.model.UserEntitlements;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.model.webhook.github.GitHubRepo;
import com.capitalone.dashboard.repository.UserEntitlementsRepository;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.model.webhook.github.GitHubParsed;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.service.CollectorService;
import com.capitalone.dashboard.util.HygieiaUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GitHubV3 {
    private static final Log LOG = LogFactory.getLog(GitHubV3.class);

    private static final String REPO_URL = "url";
    private static final String BRANCH = "branch";
    private static final String USER_ID = "userID";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "personalAccessToken";

    protected final CollectorService collectorService;
    protected final RestClient restClient;
    protected final ApiSettings apiSettings;
    protected final CollectorItemRepository collectorItemRepository;
    private final BaseCollectorRepository<GitHubCollector> collectorRepository;
    private UserEntitlementsRepository userEntitlementsRepository;
    private static final String ENTITLEMENT_TYPE = "distinguishedName";
    private Map<String, String> authorTypeMap;


    private Map<String, String> ldapMap;


    public GitHubV3(CollectorService collectorService,
                    RestClient restClient,
                    ApiSettings apiSettings,
                    CollectorItemRepository collectorItemRepository,
                    UserEntitlementsRepository userEntitlementsRepository,
                    BaseCollectorRepository<GitHubCollector> collectorRepository
    ) {
        this.collectorService = collectorService;
        this.restClient = restClient;
        this.apiSettings = apiSettings;
        this.collectorItemRepository = collectorItemRepository;
        this.collectorRepository = collectorRepository;
        ldapMap = new HashMap<>();
        authorTypeMap = new HashMap<>();
        this.userEntitlementsRepository = userEntitlementsRepository;
    }

    public GitHubCollector getCollector() {
        GitHubCollector existingCollector = null;
        /**
         * ClassCastException maybe happen when first migrating from collector to Github collector, once we run the collector
         * the data will get updated
         */
        try {
            existingCollector = collectorRepository.findByName("GitHub");
        } catch (ClassCastException ignore) {}

        if (existingCollector != null ) {
            return existingCollector;
        }
        GitHubCollector protoType = new GitHubCollector();
        protoType.setName("GitHub");
        protoType.setCollectorType(CollectorType.SCM);
        protoType.setOnline(true);
        protoType.setEnabled(true);

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(GitHubRepo.REPO_URL, "");
        allOptions.put(GitHubRepo.BRANCH, "");
        allOptions.put(GitHubRepo.USER_ID, "");
        allOptions.put(GitHubRepo.PASSWORD, "");
        allOptions.put(GitHubRepo.PERSONAL_ACCESS_TOKEN, "");
        allOptions.put(GitHubRepo.TYPE, "");
        protoType.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(GitHubRepo.REPO_URL, "");
        uniqueOptions.put(GitHubRepo.BRANCH, "");
        protoType.setUniqueFields(uniqueOptions);

        return collectorRepository.save(protoType);
    }

    public abstract String process(JSONObject jsonObject) throws MalformedURLException, HygieiaException, ParseException;
    public abstract CollectorItemRepository getCollectorItemRepository();

    protected CollectorItem buildCollectorItem (ObjectId collectorId, String repoUrl, String branch) {
        if (HygieiaUtils.checkForEmptyStringValues(repoUrl, branch)) { return null; }

        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setCollectorId(collectorId);
        collectorItem.setEnabled(true);
        collectorItem.setPushed(true);
        collectorItem.setLastUpdated(System.currentTimeMillis());
        collectorItem.getOptions().put(REPO_URL, repoUrl);
        collectorItem.getOptions().put(BRANCH, branch);

        return collectorItem;
    }

    private boolean checkForEmptyStringValues(String ... values) {
        for (String value: values) {
            if (StringUtils.isEmpty(value)) { return true; }
        }

        return false;
    }

    protected CollectorItem getCollectorItem(String repoUrl, String branch) throws HygieiaException {
        GitHubCollector col = getCollector();

        if (col == null)
            throw new HygieiaException("Failed creating collector.", HygieiaException.COLLECTOR_CREATE_ERROR);

        CollectorItem item = collectorItemRepository.findRepoByUrlAndBranch(col.getId(), repoUrl, branch);
        if (item != null)
            return item;

        item = buildCollectorItem(col.getId(), repoUrl, branch);
        if (item == null)
            throw new HygieiaException("Failed creating collector item. Invalid repo url and/or branch", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);

        CollectorItem colItem = collectorService.createCollectorItem(item);

        if (colItem == null)
            throw new HygieiaException("Failed creating collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);

        return colItem;
    }

    // Update lastUpdated field in collector item
    protected void updateCollectorItemLastUpdated(String repoUrl, String branch) throws HygieiaException {
        CollectorItem item = getCollectorItem(repoUrl, branch);
        item.setLastUpdated(System.currentTimeMillis());
        collectorItemRepository.save(item);
    }

    protected String getRepositoryToken(String scmUrl) {
        GitHubCollector collector = getCollector();

        List<ObjectId> collectorIdList = new ArrayList<>();
        collectorIdList.add(collector.getId());

        Iterable<CollectorItem> collectorItemIterable
                = getCollectorItemRepository().findAllByOptionNameValueAndCollectorIdsIn(REPO_URL, scmUrl, collectorIdList);
        if (collectorItemIterable == null) { return null; }

        String tokenValue = null;
        for (CollectorItem collectorItem : collectorItemIterable) {
            String collectorItemTokenValue = String.valueOf(collectorItem.getOptions().get(TOKEN));
            if (!StringUtils.isEmpty(collectorItemTokenValue)
                    && !"null".equalsIgnoreCase(collectorItemTokenValue)) {
                tokenValue = collectorItemTokenValue;
                break;
            }
        }
        return tokenValue;
    }

    protected void getUser(String repoUrl, String user, String token) {
        if (StringUtils.isEmpty(user)) return;
        // This is weird. Github does replace the _ in commit author with - in the user api!!!
        String formattedUser = user.replace("_", "-");
        int retryCount = 0;
        ResponseEntity<String> response;
        while(true) {
            try {
                long start = System.currentTimeMillis();

                GitHubParsed gitHubParsed = new GitHubParsed(repoUrl);
                String apiUrl = gitHubParsed.getBaseApiUrl();
                String queryUrl = apiUrl.concat("users/").concat(formattedUser);
                response = restClient.makeRestCallGet(queryUrl, "token", token);
                JSONObject userJson = restClient.parseAsObject(response);
                String ldapDN = restClient.getString(userJson, "ldap_dn");
                String authorTypeStr = restClient.getString(userJson, "type");
                if (StringUtils.isNotEmpty(ldapDN)) {
                    ldapMap.put(user, ldapDN);
                }
                if (StringUtils.isNotEmpty(authorTypeStr)) {
                    authorTypeMap.put(user, authorTypeStr);
                }

                long end = System.currentTimeMillis();
                LOG.info("Time to make the LDAP call = "+(end-start));
                return;
            } catch (ResourceAccessException e) {
                retryCount++;
                if (retryCount > apiSettings.getWebHook().getGitHub().getMaxRetries()) {
                    LOG.error("Error getting LDAP_DN For user " + user + " after " + apiSettings.getWebHook().getGitHub().getMaxRetries() + " tries.", e);
                    return;
                }
            } catch (MalformedURLException | HygieiaException | ParseException | RestClientException e) {
                LOG.error("LDAP user not found " + user, e);
                return;
            }
        }
    }

    protected String getLDAPDN(String repoUrl, String user, String token) {
        if (StringUtils.isEmpty(user) || "unknown".equalsIgnoreCase(user)) return null;
        if (ldapMap == null) { ldapMap = new HashMap<>(); }

        if(apiSettings.isOptimizeUserCallsToGithub()) {
            UserEntitlements entitlements = userEntitlementsRepository.findTopByAuthTypeAndEntitlementTypeAndUsername(AuthType.LDAP,
                    ENTITLEMENT_TYPE, user);
            return (entitlements == null) ?  "" : entitlements.getEntitlements();
        }

        //This is weird. Github does replace the _ in commit author with - in the user api!!!
        String formattedUser = user.replace("_", "-");
        if(ldapMap.containsKey(formattedUser)) {
            return ldapMap.get(formattedUser);
        }
        this.getUser(repoUrl, formattedUser, token);
        return ldapMap.get(formattedUser);
    }

    protected String getAuthorType(String repoUrl, String user, String token) {
        if (StringUtils.isEmpty(user) || "unknown".equalsIgnoreCase(user)) return null;
        if (authorTypeMap == null) { authorTypeMap = new HashMap<>(); }
        //This is weird. Github does replace the _ in commit author with - in the user api!!!
        String formattedUser = user.replace("_", "-");
        if(authorTypeMap.containsKey(formattedUser)) {
            return authorTypeMap.get(formattedUser);
        }
        this.getUser(repoUrl, formattedUser, token);
        return authorTypeMap.get(formattedUser);
    }



    protected void checkForErrors(JSONObject responseJsonObject) throws HygieiaException, ParseException {
        JSONArray errors = restClient.getArray(responseJsonObject, "errors");

        if (!CollectionUtils.isEmpty(errors)) {
            throw new HygieiaException("Error in GraphQL query:" + errors.toJSONString(), HygieiaException.JSON_FORMAT_ERROR);
        }
    }

    protected boolean isRegistered(String repoUrl, String branch) throws HygieiaException{
        GitHubCollector col = getCollector();
        if (col == null)
            throw new HygieiaException("Failed creating collector.", HygieiaException.COLLECTOR_CREATE_ERROR);

        CollectorItem existingItem = getCollectorItemRepository().findRepoByUrlAndBranch(col.getId(), repoUrl, branch, true);
        return existingItem != null;
    }
}