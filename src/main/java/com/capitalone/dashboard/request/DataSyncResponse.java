package com.capitalone.dashboard.request;

import com.capitalone.dashboard.model.CollectorItem;

import java.util.List;

public class DataSyncResponse {
    int componentCount;
    List<String> components;
    int collectorItemCount;
    List<CollectorItem> collectorItems;
    String message;

    public DataSyncResponse(int componentCount, int collectorItemCount, List<String> components,List<CollectorItem> collectorItems,String message){
        this.collectorItemCount = collectorItemCount;
        this.componentCount = componentCount;
        this.collectorItems = collectorItems;
        this.components = components;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public int getComponentCount() {
        return componentCount;
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

    public List<CollectorItem> getCollectorItems() {
        return collectorItems;
    }

    public void setCollectorItems(List<CollectorItem> collectorItems) {
        this.collectorItems = collectorItems;
    }

}
