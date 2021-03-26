package com.capitalone.dashboard.auth.openid;

import com.capitalone.dashboard.auth.AuthProperties;
import com.capitalone.dashboard.auth.ldap.CustomUserDetails;
import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.model.AuthType;
import com.capitalone.dashboard.model.UserRole;
import com.google.common.collect.Sets;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.MalformedJwtException;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Date;

@Component
public class OpenIdAuthenticationServiceImpl implements OpenIdAuthenticationService {

    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTH_PREFIX_SSO_W_SPACE = "ssoCode ";
    private static final String AUTH_RESPONSE_HEADER = "X-Authentication-Token";
    private static final String ROLES_CLAIM = "roles";
    private static final String DETAILS_CLAIM = "details";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String TOKEN_ENDPOINT = "/as/token.oauth2";
    private static final String USER_INFO_ENDPOINT = "/idp/userinfo.openid";

    private AuthProperties authProperties;
    private RestClient restClient;

    public OpenIdAuthenticationServiceImpl() {}

    @Autowired
    public OpenIdAuthenticationServiceImpl(AuthProperties authProperties, RestClient restClient) {
        this.authProperties = authProperties;
        this.restClient = restClient;
    }

    @Override
    public void addAuthentication(HttpServletResponse response, Authentication authentication) {
        String jwt = Jwts.builder().setSubject(authentication.getName())
                .claim(DETAILS_CLAIM, authentication.getDetails())
                .claim(ROLES_CLAIM, getRoles(authentication.getAuthorities()))
                .setExpiration(new Date(System.currentTimeMillis() + authProperties.getExpirationTime()))
                .signWith(SignatureAlgorithm.HS512, authProperties.getSecret()).compact();
        response.addHeader(AUTH_RESPONSE_HEADER, jwt);

    }

    @Override
    public Authentication getAuthentication(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (!isValid(authHeader)) return null;

        String authCode = getAuthCode(authHeader);
        try {
            JSONObject tokenObj = getOpenIdAccessTokenObject(authCode);
            String accessToken = (String) tokenObj.get(ACCESS_TOKEN);
            if (Objects.isNull(accessToken)) return null;

            JSONObject userInfoObj = getOpenIdUserInfo(accessToken);
            if (Objects.isNull(userInfoObj)) return null;

            CustomUserDetails principle = getUserDetails(userInfoObj);
            Collection<? extends GrantedAuthority> authorities = getAuthorities(Collections.singletonList(UserRole.ROLE_USER.name()));
            PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(principle, null, authorities);
            authentication.setDetails(AuthType.SSO);
            return authentication;

        } catch (ExpiredJwtException | SignatureException | MalformedJwtException | ParseException e) {
            return null;
        }
    }

    private CustomUserDetails getUserDetails(JSONObject userInfoObj) {
        CustomUserDetails customUserDetails = new CustomUserDetails();
        customUserDetails.setUsername((String) userInfoObj.get("sub"));
        customUserDetails.setFirstName((String) userInfoObj.get("FirstName"));
        customUserDetails.setLastName((String) userInfoObj.get("LastName"));
        customUserDetails.setEmailAddress((String) userInfoObj.get("Email"));
        return customUserDetails;
    }

    private boolean isValid(String authHeader) {
        return StringUtils.isNotBlank(authHeader) && StringUtils.startsWithIgnoreCase(authHeader, AUTH_PREFIX_SSO_W_SPACE)
                && StringUtils.isNotBlank(getAuthCode(authHeader));
    }

    private JSONObject getOpenIdUserInfo(String accessToken) throws ParseException {
        String userInfoEndPoint = authProperties.getOpenIdServerHost() + USER_INFO_ENDPOINT;
        ResponseEntity<String> userInfoResponse = restClient.makeRestCallPost(userInfoEndPoint, getUserInfoHeaders(accessToken), "");
        return restClient.parseAsObject(userInfoResponse);
    }

    private HttpHeaders getUserInfoHeaders(String accessToken) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(AUTHORIZATION, "Bearer "+ accessToken);
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return httpHeaders;
    }

    private JSONObject getOpenIdAccessTokenObject(String authCode) throws ParseException {
        String tokenHostEndPoint = authProperties.getOpenIdServerHost() + TOKEN_ENDPOINT;
        String grantType = authProperties.getOpenIdGrantType();
        String redirectUri = authProperties.getOpenIdRedirectUri();
        String scope = authProperties.getOpenIdScope();
        String tokenUrl = tokenHostEndPoint + "?code=" + authCode + "&grant_type=" + grantType + "&redirect_uri=" + redirectUri + "&scope="+ scope;
        ResponseEntity<String> tokenResponse = restClient.makeRestCallPost(tokenUrl, getHeaders(), "");
        return restClient.parseAsObject(tokenResponse);
    }

    private String getAuthCode(String authHeader) {
        return StringUtils.removeStart(authHeader, AUTH_PREFIX_SSO_W_SPACE);
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        String clientAuth = Base64.getEncoder().encodeToString((authProperties.getOpenIdClientId()
                + ":" + authProperties.getOpenIdClientSecret()).getBytes());
        httpHeaders.add(AUTHORIZATION, "Basic" + " " + clientAuth);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    private Collection<String> getRoles(Collection<? extends GrantedAuthority> authorities) {
        Collection<String> roles = Sets.newHashSet();
        authorities.forEach(authority -> {
            roles.add(authority.getAuthority());
        });
        return roles;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Collection<String> roles) {
        Collection<GrantedAuthority> authorities = Sets.newHashSet();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        return authorities;
    }
}
