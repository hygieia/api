package com.capitalone.dashboard.webhook.settings;

import com.capitalone.dashboard.client.RestUserInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Objects;

@Component
@ConfigurationPropertiesBinding
public class SonarDataSyncSettings {

    private String userId;
    private String passCode;
    private String token;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassCode() {
        return passCode;
    }

    public void setPassCode(String passCode) {
        this.passCode = passCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private HttpHeaders createHeaders(String username, String password){
        HttpHeaders headers = new HttpHeaders();
        if (username != null && !username.isEmpty()) {
            String auth = username + ":" + (password == null ? "" : password);
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(Charset.forName("US-ASCII"))
            );
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }
        return headers;
    }

    private HttpHeaders createHeaders(String token) {
        String auth = token.trim();
        auth = auth+":";
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII"))
        );
        String authHeader = "Basic " + new String(encodedAuth);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }

    public HttpHeaders getHeaders(RestUserInfo userInfo){
        if(Objects.isNull(userInfo)) return null;
        if(StringUtils.isNotBlank(userInfo.getUserId())&& StringUtils.isNotBlank(userInfo.getPassCode())){
            return createHeaders(userInfo.getUserId(),userInfo.getPassCode());
        }else if(StringUtils.isNotBlank(userInfo.getToken())){
            return createHeaders(userInfo.getToken());
        }
        return null;
    }
}
