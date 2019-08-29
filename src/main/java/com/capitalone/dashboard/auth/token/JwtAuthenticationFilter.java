package com.capitalone.dashboard.auth.token;
import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
public class JwtAuthenticationFilter extends GenericFilterBean {
	
	private TokenAuthenticationService tokenAuthenticationService;
	
	@Autowired
	public JwtAuthenticationFilter(TokenAuthenticationService tokenAuthenticationService){
		this.tokenAuthenticationService = tokenAuthenticationService;
	}
	
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        if (request != null) {
            String authHeader = ((HttpServletRequest) request).getHeader("Authorization");
            if (authHeader == null || authHeader.startsWith("apiToken ")) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        Authentication authentication = tokenAuthenticationService.getAuthentication((HttpServletRequest)request);
        //Handle Expired or bad JWT tokens
        if (Objects.isNull(authentication)) {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            filterChain.doFilter(request, response);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request,response);
    }
}