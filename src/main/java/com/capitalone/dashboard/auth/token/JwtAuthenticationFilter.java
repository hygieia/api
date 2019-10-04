package com.capitalone.dashboard.auth.token;
import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class);
	private TokenAuthenticationService tokenAuthenticationService;
	
	@Autowired
	public JwtAuthenticationFilter(TokenAuthenticationService tokenAuthenticationService){
		this.tokenAuthenticationService = tokenAuthenticationService;
	}
	
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.startsWith("apiToken ")) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        Authentication authentication = tokenAuthenticationService.getAuthentication(request);

        if (Objects.isNull(authentication)) {
            //Handle Expired or bad JWT tokens
            LOGGER.info("doFilter - Expired or bad JWT tokens, set response to SC_UNAUTHORIZED");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            filterChain.doFilter(request, response); // TODO: should we remove this?
        } else {
            // process properly authenticated requests
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            tokenAuthenticationService.addAuthentication(response, authentication);
        }
    }
}