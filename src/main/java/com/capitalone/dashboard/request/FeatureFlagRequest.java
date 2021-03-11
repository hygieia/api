package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

public class FeatureFlagRequest extends BaseRequest {

    @NotNull
    private String json;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
