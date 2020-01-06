package com.capitalone.dashboard.model;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

public class TestCreateRequest {

    /**
     * Type of test
     */

    @NotNull
    @ApiModelProperty(notes = "Example values functional,unit")
    private String testType;

    @NotNull
    @ApiModelProperty(notes = "Example values  cucumber,junit")
    private String sourceFormat;

    @NotNull
    private String source;

    /**
     * Creation timestamp
     */

    @NotNull
    private long timestamp;


    private String configurationItem;
    private String targetAppName;
    private String targetServiceName;
    private String payload;
    private String jobUrl;


    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getConfigurationItem() {
        return configurationItem;
    }

    public void setConfigurationItem(String configurationItem) {
        this.configurationItem = configurationItem;
    }
    
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getTargetAppName() {
        return targetAppName;
    }

    public void setTargetAppName(String targetAppName) {
        this.targetAppName = targetAppName;
    }

    public String getTargetServiceName() {
        return targetServiceName;
    }

    public void setTargetServiceName(String targetServiceName) {
        this.targetServiceName = targetServiceName;
    }
}
