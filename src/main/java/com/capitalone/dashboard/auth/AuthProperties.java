package com.capitalone.dashboard.auth;

import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.capitalone.dashboard.model.AuthType;
import com.google.common.collect.Lists;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthProperties.class);

	private Long expirationTime;
	private String secret;
	private String ldapUserDnPattern;
	private String ldapServerUrl;
	private List<AuthType> authenticationProviders = Lists.newArrayList();

	private String openIdClientId;
	private String openIdClientSecret;
	private String openIdServerHost;
	private String openIdRedirectUri;
	private String openIdGrantType;
	private String openIdScope;

	private String adDomain;
	private String adRootDn;
	private String adUserRootDn;
	private String adSvcRootDn;
	private String adUrl;

	private String ldapBindUser;
	private String ldapBindPass;

	private boolean ldapDisableGroupAuthorization = false;

	/**
	 * The LDAP filter used to search for users (optional). For example "(&(objectClass=user)(sAMAccountName={0}))". The
	 * substituted parameter is the user's login name.
	 **/
	private String ldapUserSearchFilter;

	/**
	 * Username (DN) of the "manager" user identity (i.e. "uid=admin,ou=system") which
	 * will be used to authenticate to a (non-embedded) LDAP server. If omitted,
	 * anonymous access will be used.
	 **/
	private String ldapManagerDn;

	/**
	 * The password for the manager DN. This is required if the ldapManagerDn is
	 * specified.
	 **/
	private String ldapManagerPassword;

	// -- SSO properties
	private String userEid;
	private String userEmail;
	private String userFirstName;
	private String userLastName;
	private String userMiddelInitials;
	private String userDisplayName;
	//-- end SSO properties

	public Long getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(Long expirationTime) {
		this.expirationTime = expirationTime;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getLdapUserDnPattern() {
		return ldapUserDnPattern;
	}

	public void setLdapUserDnPattern(String ldapUserDnPattern) {
		this.ldapUserDnPattern = ldapUserDnPattern;
	}

	public String getLdapServerUrl() {
		return ldapServerUrl;
	}

	public void setLdapServerUrl(String ldapServerUrl) {
		this.ldapServerUrl = ldapServerUrl;
	}

	public List<AuthType> getAuthenticationProviders() {
		return authenticationProviders;
	}

	public void setAuthenticationProviders(List<AuthType> authenticationProviders) {
		this.authenticationProviders = authenticationProviders;
	}

	public String getAdDomain() {
		return adDomain;
	}

	public void setAdDomain(String adDomain) {
		this.adDomain = adDomain;
	}

	public String getAdRootDn() {
		return adRootDn;
	}

	public void setAdRootDn(String adRootDn) {
		this.adRootDn = adRootDn;
	}

	public String getAdUserRootDn() {
		return adUserRootDn;
	}

	public void setAdUserRootDn(String adUserRootDn) {
		this.adUserRootDn = adUserRootDn;
	}

	public String getAdSvcRootDn() {
		return adSvcRootDn;
	}

	public void setAdSvcRootDn(String adSvcRootDn) {
		this.adSvcRootDn = adSvcRootDn;
	}

	public String getAdUrl() {
		return adUrl;
	}

	public void setAdUrl(String adUrl) {
		this.adUrl = adUrl;
	}

	public String getLdapBindUser() {
		return ldapBindUser;
	}

	public void setLdapBindUser(String ldapBindUser) {
		this.ldapBindUser = ldapBindUser;
	}

	public String getLdapBindPass() {
		return ldapBindPass;
	}

	public void setLdapBindPass(String ldapBindPass) {
		this.ldapBindPass = ldapBindPass;
	}

	public boolean isLdapDisableGroupAuthorization() {
		return ldapDisableGroupAuthorization;
	}

	public void setLdapDisableGroupAuthorization(boolean ldapDisableGroupAuthorization) {
		this.ldapDisableGroupAuthorization = ldapDisableGroupAuthorization;
	}

	public String getLdapUserSearchFilter() {
		return ldapUserSearchFilter;
	}

	public void setLdapUserSearchFilter(String ldapUserSearchFilter) {
		this.ldapUserSearchFilter = ldapUserSearchFilter;
	}

	public String getLdapManagerDn() {
		return ldapManagerDn;
	}

	public void setLdapManagerDn(String ldapManagerDn) {
		this.ldapManagerDn = ldapManagerDn;
	}

	public String getLdapManagerPassword() {
		return ldapManagerPassword;
	}

	public void setLdapManagerPassword(String ldapManagerPassword) {
		this.ldapManagerPassword = ldapManagerPassword;
	}

	@PostConstruct
	public void applyDefaultsIfNeeded() {
		if (getSecret() == null) {
			LOGGER.info("No JWT secret found in configuration, generating random secret by default.");
			setSecret(UUID.randomUUID().toString().replace("-", ""));
		}

		if (getExpirationTime() == null) {
			LOGGER.info("No JWT expiration time found in configuration, setting to 30 minutes.");
			setExpirationTime((long) 1000 * 60 * 30);
		}

		if (CollectionUtils.isEmpty(authenticationProviders)) {
			authenticationProviders.add(AuthType.STANDARD);
		}
	}

	public String getUserEid() {
		return userEid;
	}

	public void setUserEid(String userEid) {
		this.userEid = userEid;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserFirstName() {
		return userFirstName;
	}

	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}

	public String getUserLastName() {
		return userLastName;
	}

	public void setUserLastName(String userLastName) {
		this.userLastName = userLastName;
	}

	public String getUserMiddelInitials() {
		return userMiddelInitials;
	}

	public void setUserMiddelInitials(String userMiddelInitials) {
		this.userMiddelInitials = userMiddelInitials;
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String userDisplayName) {
		this.userDisplayName = userDisplayName;
	}

	public String getOpenIdServerHost() {
		return openIdServerHost;
	}

	public void setOpenIdServerHost(String openIdServerHost) {
		this.openIdServerHost = openIdServerHost;
	}

	public String getOpenIdClientId() {
		return openIdClientId;
	}

	public void setOpenIdClientId(String openIdClientId) {
		this.openIdClientId = openIdClientId;
	}

	public String getOpenIdClientSecret() {
		return openIdClientSecret;
	}

	public void setOpenIdClientSecret(String openIdClientSecret) {
		this.openIdClientSecret = openIdClientSecret;
	}

	public String getOpenIdRedirectUri() {
		return openIdRedirectUri;
	}

	public void setOpenIdRedirectUri(String openIdRedirectUri) {
		this.openIdRedirectUri = openIdRedirectUri;
	}

	public String getOpenIdGrantType() {
		return openIdGrantType;
	}

	public void setOpenIdGrantType(String openIdGrantType) {
		this.openIdGrantType = openIdGrantType;
	}

	public String getOpenIdScope() {
		return openIdScope;
	}

	public void setOpenIdScope(String openIdScope) {
		this.openIdScope = openIdScope;
	}

}
