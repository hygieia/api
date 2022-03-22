package com.capitalone.dashboard.request;

import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;

public class GitRequestRequest extends BaseRequest {
    @NotNull
    private ObjectId componentId;
    private Integer numberOfDays;
    private ObjectId collectorItemId;


    public ObjectId getComponentId() {
        return componentId;
    }

    public void setComponentId(ObjectId componentId) {
        this.componentId = componentId;
    }

    public Integer getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(Integer numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public ObjectId getCollectorItemId() { return collectorItemId; }

    public void setCollectorItemId(ObjectId collectorItemId) { this.collectorItemId = collectorItemId;  }
}
