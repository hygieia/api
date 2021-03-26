package com.capitalone.dashboard.auth.openid;

import com.capitalone.dashboard.auth.AuthProperties;
import com.capitalone.dashboard.auth.AuthenticationFixture;
import com.capitalone.dashboard.client.RestClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenIdAuthenticationServiceImplTest {
    private static final String USERNAME = "username";
    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTH_PREFIX_W_SPACE = "ssoCode ";
    private static final String AUTH_RESPONSE_HEADER = "X-Authentication-Token";

    private OpenIdAuthenticationServiceImpl service;
    private AuthProperties authProperties;

    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private RestClient restClient;

    @Before
    public void setup() {
        authProperties = new AuthProperties();
        authProperties.setExpirationTime(100000L);
        authProperties.setSecret("someSecretValue");
        service = new OpenIdAuthenticationServiceImpl(authProperties, restClient);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testAddAuthentication() {
        service.addAuthentication(response, AuthenticationFixture.getAuthentication(USERNAME));
        verify(response).addHeader(eq(AUTH_RESPONSE_HEADER), anyString());
    }

    @Test
    public void testGetAuthentication_noHeader() {
        when(request.getHeader(AUTHORIZATION)).thenReturn(null);
        assertNull(service.getAuthentication(request));
    }

    @Test
    public void testGetAuthentication_MalformedJWT() {
        when(request.getHeader(AUTHORIZATION)).thenReturn("null");
        assertNull(service.getAuthentication(request));
    }

    @Test
    public void testGetAuthentication_expiredToken() {
        when(request.getHeader(AUTHORIZATION)).thenReturn(AUTH_PREFIX_W_SPACE + "");
        assertNull(service.getAuthentication(request));
    }

    @Test
    public void testGetAuthentication() throws ParseException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("access_token", "someEncodedToken");
        when(request.getHeader(AUTHORIZATION)).thenReturn(AUTH_PREFIX_W_SPACE + "someEncodedAuthCode");
        when(restClient.makeRestCallPost("", new HttpHeaders(), "")).thenReturn(new ResponseEntity<String>(HttpStatus.OK));
        jsonObject.put("sub", USERNAME);
        when(restClient.parseAsObject(any(ResponseEntity.class))).thenReturn(jsonObject);
        Authentication result = service.getAuthentication(request);

        assertNotNull(result);
        assertEquals(USERNAME, result.getName());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertNotNull(result.getDetails());
        verify(request).getHeader(AUTHORIZATION);
    }
}
