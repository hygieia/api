package com.capitalone.dashboard.request;

import com.capitalone.dashboard.model.CollectorItem;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class CollectorItemRequest extends BaseRequest {
    @NotNull
    private ObjectId collectorId;

    private ObjectId id;

    private String description;
    private Map<String,Object> options = new HashMap<>();

    private Map<String, Object> uniqueOptions = new HashMap<>();

    private boolean deleteFromComponent = true;

    public ObjectId getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(ObjectId collectorId) {
        this.collectorId = collectorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Map<String, Object> getUniqueOptions() {
        return uniqueOptions;
    }

    public void setUniqueOptions(Map<String, Object> uniqueOptions) {
        this.uniqueOptions = uniqueOptions;
    }

    public boolean isDeleteFromComponent() { return deleteFromComponent; }

    public void setDeleteFromComponent(boolean deleteFromComponent) { this.deleteFromComponent = deleteFromComponent; }

    public ObjectId getId() { return id; }

    public void setId(ObjectId id) { this.id = id; }

    public CollectorItem toCollectorItem() {
        CollectorItem item = new CollectorItem();
        item.setCollectorId(collectorId);
        item.setEnabled(true);
        item.setDescription(description);
        item.getOptions().putAll(options);
        return item;
    }
}
