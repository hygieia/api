package com.capitalone.dashboard.auth.openid;

import com.capitalone.dashboard.auth.AuthenticationResultHandler;
import com.capitalone.dashboard.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OpenIdAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    @Autowired
    private OpenIdAuthenticationService openIdAuthenticationService;
    protected RestClient restClient;

    @Autowired
    public OpenIdAuthenticationFilter(String defaultFilterProcessesUrl, AuthenticationManager authenticationManager,
                                      AuthenticationResultHandler authenticationResultHandler, RestClient restClient) {
        super(defaultFilterProcessesUrl);
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(authenticationResultHandler);
        this.restClient = restClient;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return openIdAuthenticationService.getAuthentication(httpServletRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) {
        SecurityContextHolder.getContext().setAuthentication(authResult);
        openIdAuthenticationService.addAuthentication(response, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OpenId SSO Authentication Failed");
    }
}
