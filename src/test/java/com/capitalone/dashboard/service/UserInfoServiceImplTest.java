package com.capitalone.dashboard.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Collection;

import com.capitalone.dashboard.auth.AuthProperties;
import com.capitalone.dashboard.settings.ApiSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.capitalone.dashboard.auth.exceptions.DeleteLastAdminException;
import com.capitalone.dashboard.auth.exceptions.UserNotFoundException;
import com.capitalone.dashboard.model.AuthType;
import com.capitalone.dashboard.model.UserInfo;
import com.capitalone.dashboard.model.UserRole;
import com.capitalone.dashboard.repository.UserInfoRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.naming.directory.SearchControls;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

@RunWith(MockitoJUnitRunner.class)
public class UserInfoServiceImplTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private AuthProperties authProperties;

    @Mock
    private InitialDirContext context;

    @InjectMocks
    private UserInfoServiceImpl service;

    @Mock private ApiSettings apiSettings;

    @Before
    public void setup(){
        authProperties = new AuthProperties();
        authProperties.setLdapBindUser("dummy");
        authProperties.setLdapBindPass("password");
        authProperties.setAdUrl("ldap://company.com:398");
        authProperties.setAdDomain("company.com");
    }

    @Test
    public void shouldGetAuthorities() {
        UserInfo user = new UserInfo();
        user.setUsername("user");
        user.setAuthType(AuthType.STANDARD);
        user.setAuthorities(Lists.newArrayList(UserRole.ROLE_ADMIN));
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        when(userInfoRepository.findByUsernameAndAuthType("user", AuthType.STANDARD)).thenReturn(user);
        Collection<? extends GrantedAuthority> authorities = service.getAuthorities("user", "", "", "", "", "", AuthType.STANDARD);
        
        assertTrue(authorities.contains(authority));
    }
    
    @Test
    public void shouldGetUserInfoFromExistingUser() {
        UserInfo user = new UserInfo();
        user.setUsername("user");
        user.setAuthType(AuthType.STANDARD);
        
        when(userInfoRepository.findByUsernameAndAuthType("user", AuthType.STANDARD)).thenReturn(user);
        
        UserInfo result = service.getUserInfo("user", "", "", "", "", "", AuthType.STANDARD);
        
        assertSame(result, user);
    }
    
    @Test
    public void shouldGetUserInfoFromNonExistingUser() {
        String username = "user";
        AuthType authType = AuthType.STANDARD;
        when(userInfoRepository.findByUsernameAndAuthType(username, authType)).thenReturn(null);
        
        UserInfo result = service.getUserInfo(username, "", "", "", "", "", authType);
        
        assertEquals(username, result.getUsername());
        assertEquals(authType, result.getAuthType());
        assertTrue(result.getAuthorities().contains(UserRole.ROLE_USER));
        verify(userInfoRepository).save(isA(UserInfo.class));
    }
    
    @Test
    public void standardAdminUserShouldBeAdmin() {
        String username = "admin";
        AuthType authType = AuthType.STANDARD;
        when(userInfoRepository.findByUsernameAndAuthType(username, authType)).thenReturn(null);
        
        UserInfo result = service.getUserInfo(username, "", "", "", "", "", authType);
        
        assertEquals(username, result.getUsername());
        assertEquals(authType, result.getAuthType());
        assertTrue(result.getAuthorities().contains(UserRole.ROLE_USER));
        assertTrue(result.getAuthorities().contains(UserRole.ROLE_ADMIN));
    }
    
    @Test
    public void shouldGetAllUsers() {
        UserInfo user = new UserInfo();
        Collection<UserInfo> users = Sets.newHashSet(user, user);
        when(userInfoRepository.findAll()).thenReturn(users);
        
        Collection<UserInfo> result = service.getUsers();
        
        assertTrue(result.contains(user));
        assertTrue(result.size() == 1);
    }
    
    @Test(expected=UserNotFoundException.class)
    public void shouldNotPromoteNonExistingUserToAdmin() {
        String username = "user";
        AuthType authType = AuthType.STANDARD;
        when(userInfoRepository.findByUsernameAndAuthType(username, authType)).thenReturn(null);
        
        UserInfo result = service.promoteToAdmin(username, authType);
        
        fail("Exception should have been thrown.");
    }
    
    @Test
    public void shouldPromoteExistingUserToAdmin() {
        String username = "user";
        AuthType authType = AuthType.STANDARD;
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setAuthType(authType);
        when(userInfoRepository.findByUsernameAndAuthType(username, authType)).thenReturn(user);
        when(userInfoRepository.save(isA(UserInfo.class))).thenReturn(user);
        UserInfo result = service.promoteToAdmin(username, authType);
        
        assertNotNull(result);
        assertTrue(result.getAuthorities().contains(UserRole.ROLE_ADMIN));
        verify(userInfoRepository).save(user);
    }
    
    @Test(expected=DeleteLastAdminException.class)
    public void shouldNotDeleteLastAdmin() {
        String username = "user";
        AuthType authType = AuthType.STANDARD;
        Collection<UserInfo> users = new ArrayList<>();
        when(userInfoRepository.findByAuthoritiesIn(UserRole.ROLE_ADMIN)).thenReturn(users);
        
        service.demoteFromAdmin(username, authType);
        
        fail("Should have thrown an exception");
        
    }
    
    @Test(expected=UserNotFoundException.class)
    public void shouldNotRemoveAdminFromNonExistingUSer() {
        String username = "user";
        AuthType authType = AuthType.STANDARD;
        Collection<UserInfo> users = Lists.newArrayList(new UserInfo(), new UserInfo());
        when(userInfoRepository.findByAuthoritiesIn(UserRole.ROLE_ADMIN)).thenReturn(users);
        when(userInfoRepository.findByUsernameAndAuthType(username, authType)).thenReturn(null);
        
        service.demoteFromAdmin(username, authType);
        
        fail("Exception should have been thrown.");
    }
    
    @Test
    public void shouldRemoveAdminFromExistingUser() {
        String username = "user";
        AuthType authType = AuthType.STANDARD;
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setAuthType(authType);
        user.getAuthorities().add(UserRole.ROLE_ADMIN);
        Collection<UserInfo> users = Lists.newArrayList(new UserInfo(), new UserInfo());
        when(userInfoRepository.findByAuthoritiesIn(UserRole.ROLE_ADMIN)).thenReturn(users);
        when(userInfoRepository.findByUsernameAndAuthType(username, authType)).thenReturn(user);
        when(userInfoRepository.save(isA(UserInfo.class))).thenReturn(user);
        
        UserInfo result = service.demoteFromAdmin(username, authType);
        
        assertNotNull(result);
        assertFalse(result.getAuthorities().contains(UserRole.ROLE_ADMIN));
        verify(userInfoRepository).save(user);
        
    }

    @Test
    public void shouldValidateUser() throws NamingException {
        UserInfo user = new UserInfo();
        user.setUsername("standarduser");
        user.setAuthType(AuthType.STANDARD);

        when(userInfoRepository.findByUsernameAndAuthType("abc123", AuthType.STANDARD)).thenReturn(null);
        boolean result = service.isUserValid("abc123", AuthType.STANDARD);
        assertFalse(result);

        when(userInfoRepository.findByUsernameAndAuthType("standarduser", AuthType.STANDARD)).thenReturn(user);
        result = service.isUserValid("standarduser", AuthType.STANDARD);
        assertTrue(result);
    }

    @Test
    public void shouldSearchLdapUser() throws NamingException {
        UserInfo user = new UserInfo();
        user.setUsername("standarduser");
        user.setAuthType(AuthType.LDAP);

        UserInfo svc = new UserInfo();
        svc.setUsername("svc");
        svc.setAuthType(AuthType.LDAP);

        // setup authProperties
        setup();
        service = new UserInfoServiceImpl(userInfoRepository,authProperties, apiSettings);

        context = Mockito.mock(InitialDirContext.class);
        service.setInitialContext(context);
        NamingEnumeration<SearchResult> results = Mockito.mock(NamingEnumeration.class);
        SearchResult searchResult = Mockito.mock(SearchResult.class);
        Attributes attrs = Mockito.mock(Attributes.class);
        Attribute att = Mockito.mock(Attribute.class);

        when(context.search(any(String.class),any(String.class),any(SearchControls.class))).thenReturn(results);
        when(results.hasMore()).thenReturn(true);
        when(results.next()).thenReturn(searchResult);
        when(searchResult.getAttributes()).thenReturn(attrs);
        when(searchResult.getAttributes().get(any(String.class))).thenReturn(att);

        // service account
        assertNotNull(att);
        assertTrue(service.searchLdapUser(svc.getUsername()));

        // valid user
        assertNotNull(att);
        when(results.hasMore()).thenReturn(false, true);
        assertTrue(service.searchLdapUser(user.getUsername()));

        // unknown user
        when(results.hasMore()).thenReturn(false);
        assertFalse(service.searchLdapUser("invalidUser"));

    }

}
