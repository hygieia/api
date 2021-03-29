package com.capitalone.dashboard.auth.openid;

import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface OpenIdAuthenticationService {
    void addAuthentication(HttpServletResponse response, Authentication authentication);
    Authentication getAuthentication(HttpServletRequest request);
}
