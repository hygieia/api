package com.capitalone.dashboard.auth.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@RunWith(MockitoJUnitRunner.class)
public class JwtAuthenticationFilterTest {

	private JwtAuthenticationFilter filter;
	
	@Mock
	private TokenAuthenticationService authService;
	
	@Mock
	private FilterChain filterChain;

	@Mock
	private Authentication authentication;

	@Mock
	private HttpServletResponse response;

	@Mock
	private HttpServletRequest request;

	@Before
	public void setup() {
		SecurityContextHolder.clearContext();
		filter = new JwtAuthenticationFilter(authService);
		when(authService.getAuthentication(any(HttpServletRequest.class))).thenReturn(authentication);
	}
	
	@Test
	public void testDoFilter_readOnly() throws Exception {
		when(request.getHeader(anyString())).thenReturn("DUMMY_TOKEN");
		filter.doFilter(request, response, filterChain);
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
	
		verify(authService).getAuthentication(any(HttpServletRequest.class));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	public void testDoFilter_apiToken() throws Exception {
		when(request.getHeader(anyString())).thenReturn("apiToken blah blah");
		filter.doFilter(request, response, filterChain);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
