package com.capitalone.dashboard.service;


import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.client.RestOperationsSupplier;
import com.capitalone.dashboard.model.webhook.github.GitHubRepo;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.CommitRepository;
import com.capitalone.dashboard.repository.GitHubRepoRepository;
import com.capitalone.dashboard.repository.GitRequestRepository;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.webhook.github.GitHubSyncServiceImpl;
import com.capitalone.dashboard.webhook.settings.GithubSyncSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class GithubSyncServiceTest {

    private static final String URL_USER = "http://mygithub.com/api/v3/users/";

    @Mock private RestOperationsSupplier restOperationsSupplier;
    @Mock private RestOperations rest;
    private ApiSettings settings;
    private GithubSyncSettings githubSyncSettings;
    private GitHubSyncServiceImpl gitHubSyncService;
    @Mock private CommitRepository commitRepository;
    @Mock private GitRequestRepository gitRequestRepository;
    @Mock private CollectorItemRepository collectorItemRepository;
    @Mock private GitHubRepoRepository gitHubRepoRepository;
    @Mock private CollectorRepository collectorRepository;

    @Before
    public void init() {
        when(restOperationsSupplier.get()).thenReturn(rest);
        settings = new ApiSettings();
        githubSyncSettings = new GithubSyncSettings();
        settings.setGithubSyncSettings(githubSyncSettings);
        gitHubSyncService = new GitHubSyncServiceImpl(commitRepository,gitRequestRepository,collectorItemRepository,gitHubRepoRepository,collectorRepository,settings, new RestClient(restOperationsSupplier));
        gitHubSyncService.setLdapMap(new HashMap<>());

    }

    @Test
    public void getLDAPDN_With_Underscore() {
        String userhyphen = "this-has-underscore";
        String userUnderscore = "this_has_underscore";

        when(rest.exchange(eq(URL_USER + userhyphen), eq(HttpMethod.GET),
                eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>(goodLdapResponse(), HttpStatus.OK));
        String ldapUser = gitHubSyncService.getLDAPDN(getGitRepo(),userUnderscore);
        assertEquals(ldapUser, "CN=ldapUser,OU=Developers,OU=All Users,DC=test,DC=ds,DC=mycompany,DC=com");
        assertEquals(gitHubSyncService.getLdapMap().containsKey(userhyphen), true);
        assertEquals(gitHubSyncService.getLdapMap().containsKey(userUnderscore), false);
        assertEquals(gitHubSyncService.getLdapMap().get(userhyphen), "CN=ldapUser,OU=Developers,OU=All Users,DC=test,DC=ds,DC=mycompany,DC=com");
        assertEquals(gitHubSyncService.getLdapMap().get(userUnderscore), null);
    }

    @Test
    public void getLDAPDN_With_Hyphen() {
        String userhyphen = "this-has-hyphen";

        when(rest.exchange(eq(URL_USER + userhyphen), eq(HttpMethod.GET),
                eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>(goodLdapResponse(), HttpStatus.OK));
        String ldapUser = gitHubSyncService.getLDAPDN(getGitRepo(),userhyphen);
        assertEquals(ldapUser, "CN=ldapUser,OU=Developers,OU=All Users,DC=test,DC=ds,DC=mycompany,DC=com");
        assertEquals(gitHubSyncService.getLdapMap().containsKey(userhyphen), true);
    }

    @Test
    public void getLDAPDNSimple() {
        String user = "someuser";

        when(rest.exchange(eq(URL_USER + user), eq(HttpMethod.GET),
                eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>(goodLdapResponse(), HttpStatus.OK));
        String ldapUser = gitHubSyncService.getLDAPDN(getGitRepo(),user);
        assertEquals(ldapUser, "CN=ldapUser,OU=Developers,OU=All Users,DC=test,DC=ds,DC=mycompany,DC=com");
        assertEquals(gitHubSyncService.getLdapMap().containsKey(user), true);
    }

    @Test
    public void getLDAPDN_NotFound() {
        String user = "someuser-unknown";

        when(rest.exchange(eq(URL_USER + user), eq(HttpMethod.GET),
                eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
        String ldapUser = gitHubSyncService.getLDAPDN(getGitRepo(),user);
        assertEquals(ldapUser, null);
        assertEquals(gitHubSyncService.getLdapMap().containsKey(user), false);
    }



    @Test
    public void getLDAPDN_OtherCharacters() {
        String user = "someuser@#$%&($@#---unknown";

        when(rest.exchange(eq(URL_USER + user), eq(HttpMethod.GET),
                eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>(goodLdapResponse(), HttpStatus.OK));
        String ldapUser = gitHubSyncService.getLDAPDN(getGitRepo(),user);
        assertEquals(ldapUser, "CN=ldapUser,OU=Developers,OU=All Users,DC=test,DC=ds,DC=mycompany,DC=com");
        assertEquals(gitHubSyncService.getLdapMap().containsKey(user), true);
    }


    private GitHubRepo getGitRepo() {
        GitHubRepo repo = new GitHubRepo();
        repo.setBranch("master");
        repo.setRepoUrl("http://mygithub.com/user/repo");
        return repo;
    }

    private String goodLdapResponse() {
        return "{ \"ldap_dn\": \"CN=ldapUser,OU=Developers,OU=All Users,DC=test,DC=ds,DC=mycompany,DC=com\"}";
    }
    
}
