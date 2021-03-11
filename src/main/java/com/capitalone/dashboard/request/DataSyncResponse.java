package com.capitalone.dashboard.request;

import java.util.List;

public class DataSyncResponse extends BaseRequest {
    int componentCount;
    List<String> components;
    int collectorItemCount;
    String message;

    public DataSyncResponse(List<String> components,int collectorItemCount,String message){
        this.components = components;
        this.collectorItemCount = collectorItemCount;
        this.message = message;
    }


    public int getComponentCount() {
        return getComponents().size();
    }

    public void setComponentCount(int componentCount) {
        this.componentCount = componentCount;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public int getCollectorItemCount() {
        return collectorItemCount;
    }

    public void setCollectorItemCount(int collectorItemCount) {
        this.collectorItemCount = collectorItemCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
