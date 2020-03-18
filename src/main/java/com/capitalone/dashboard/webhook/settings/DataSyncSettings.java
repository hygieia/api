package com.capitalone.dashboard.webhook.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class DataSyncSettings {
    @Value("${dataSyncSettings.scm:GitHub}")
    String scm;
    @Value("${dataSyncSettings.codeQuality:Sonar}")
    String codeQuality;
    @Value("${dataSyncSettings.artifact:Artifactory}")
    String artifact;
    @Value("${dataSyncSettings.staticSecurity:StaticSecurity}")
    String staticSecurity;
    @Value("${dataSyncSettings.libraryPolicy:LibraryPolicy}")
    String libraryPolicy;

    public String getScm() {
        return scm;
    }

    public void setScm(String scm) {
        this.scm = scm;
    }

    public String getCodeQuality() {
        return codeQuality;
    }

    public void setCodeQuality(String codeQuality) {
        this.codeQuality = codeQuality;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getStaticSecurity() {
        return staticSecurity;
    }

    public void setStaticSecurity(String staticSecurity) {
        this.staticSecurity = staticSecurity;
    }

    public String getLibraryPolicy() {
        return libraryPolicy;
    }

    public void setLibraryPolicy(String libraryPolicy) {
        this.libraryPolicy = libraryPolicy;
    }

}
