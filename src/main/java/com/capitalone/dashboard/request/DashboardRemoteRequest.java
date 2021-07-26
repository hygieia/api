package com.capitalone.dashboard.request;


import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Owner;
import com.capitalone.dashboard.util.GitHubParsedUrl;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardRemoteRequest extends BaseRequest {
    @Valid
    private DashboardMetaData metaData;

    @Valid
    private List<FeatureEntry> featureEntries = new ArrayList<>();

    @Valid
    private List<CodeRepoEntry> codeRepoEntries = new ArrayList<>();

    @Valid
    private List<BuildEntry> buildEntries = new ArrayList<>();

    @Valid
    private List<StaticCodeEntry> staticCodeEntries = new ArrayList<>();

    @Valid
    private List<SecurityScanEntry> securityScanEntries = new ArrayList<>();

    @Valid
    private List<DeploymentEntry> deploymentEntries = new ArrayList<>();

    @Valid
    private List<LibraryScanEntry> libraryScanEntries = new ArrayList<>();

    @Valid
    private List<FunctionalTestEntry> functionalTestEntries = new ArrayList<>();

    @Valid
    private List<ArtifactEntry> artifactEntries = new ArrayList<>();

    @Valid
    private List<PerformanceTestEntry> performanceTestEntries = new ArrayList<>();

    @Valid
    private List<InfraStructureScanEntry> infraStructureScanEntries = new ArrayList<>();

    /**
     * Dashboard Metadata
     */

    public static class DashboardMetaData {
        @NotNull
        private String template;

        @NotNull
        private String type;

        @NotNull
        @Size(min = 6, max = 50)
        @Pattern(message = "Special character(s) found", regexp = "^[a-zA-Z0-9 ]*$")
        private String title;

        @NotNull
        private String applicationName;

        @NotNull
        private String componentName;

        Owner owner;

        List<Owner> owners;

        private String businessService;

        private String businessApplication;

        private Map<String, String> properties = new HashMap<>();

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getComponentName() {
            return componentName;
        }

        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        public String getBusinessService() {
            return businessService;
        }

        public void setBusinessService(String businessService) {
            this.businessService = businessService;
        }

        public String getBusinessApplication() {
            return businessApplication;
        }

        public void setBusinessApplication(String businessApplication) {
            this.businessApplication = businessApplication;
        }

        public Owner getOwner() {
            return owner;
        }

        public List<Owner> getOwners() {
            return owners;
        }

        public void setOwner(Owner owner) {
            this.owner = owner;
        }

        public void setOwners(List<Owner> owners) {
            this.owners = owners;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, String> getProperties() { return properties; }

        public void setProperties(Map<String, String> properties) { this.properties = properties; }
    }

    /**
     * An abstract class to hold the entries: Jira project, github project, build job etc.
     */

    public static abstract class Entry {
        @NotNull
        String toolName;

        @NotNull
        String description;

        boolean pushed = false;

        String niceName;

        String altIdentifier;

        @NotEmpty
        Map<String, Object> options = new HashMap<>();

        public abstract CollectorType getType();

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isPushed() {
            return pushed;
        }

        public void setPushed(boolean pushed) {
            this.pushed = pushed;
        }

        public String getNiceName() { return niceName; }

        public void setNiceName(String niceName) { this.niceName = niceName; }

        public String getAltIdentifier() {
            return altIdentifier;
        }

        public void setAltIdentifier(String altIdentifier) {
            this.altIdentifier = altIdentifier;
        }

        public CollectorItem toCollectorItem(Collector collector) throws HygieiaException{
            if (options.keySet().containsAll(collector.getUniqueFields().keySet())) {
                CollectorItem collectorItem = new CollectorItem();
                collectorItem.setEnabled(true);
                collectorItem.setPushed(isPushed());
                collectorItem.setDescription(description);
                collectorItem.setNiceName(niceName);
                collectorItem.setAltIdentifier(altIdentifier);
                for (String key : options.keySet()) {
                    if (collector.getAllFields().keySet().contains(key)) {
                        collectorItem.getOptions().put(key, options.get(key));
                    } else {
                        throw new HygieiaException(toolName + " collector does not support field: " + key, HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
                    }
                }
                return collectorItem;
            } else {
                throw new HygieiaException("Missing required fields. " + toolName + " collector required fields are: " + String.join(", ", collector.getUniqueFields().keySet()), HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
            }
        }

        public abstract String getWidgetId();

        public abstract String getWidgetName();

        public abstract Map<String, Object> toWidgetOptions();

        public Map<String, Object> getOptions() {
            return options;
        }

        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
    }

    /**
     * Details for creating Feature widget
     */
    public static class FeatureEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.AgileTool;
        }

        @Override
        public String getWidgetId() {
            return "feature0";
        }

        @Override
        public String getWidgetName() {
            return "feature";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            options.keySet().forEach(key -> {
                opts.put(key, options.get(key));
            });
            return opts;
        }
    }

    /**
     * Details for creating Code Repo widget
     */
    public static class CodeRepoEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.SCM;
        }


        @Override
        public String getWidgetId() {
            return "repo0";
        }

        @Override
        public String getWidgetName() {
            return "repo";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("name", "repo");
            opts.put("id", "repo0");
            options.keySet().forEach(key -> {
                if ("url".equalsIgnoreCase(key)) {
                    GitHubParsedUrl gitHubParsed = new GitHubParsedUrl((String) options.get(key));
                    String repoUrl = gitHubParsed.getUrl();
                    opts.put(key, repoUrl);
                } else {
                    opts.put(key, options.get(key));
                }
            });
            Map<String, String> scm = new HashMap<>();
            scm.put("name", toolName);
            scm.put("value", toolName);
            opts.put("scm", scm);
            return opts;
        }
    }

    /**
     * Details for creating Build widget
     */
    public static class BuildEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.Build;
        }

        @Override
        public String getWidgetId() {
            return "build0";
        }

        @Override
        public String getWidgetName() {
            return "build";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            opts.put("buildDurationThreshold", 3);
            opts.put("consecutiveFailureThreshold",5);
            return opts;
        }

    }

    /**
     * Details for creating Static Code Analysis in Code Quality Widget
     */

    public static class StaticCodeEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.CodeQuality;
        }

        @Override
        public String getWidgetId() {
            return "codeanalysis0";
        }

        @Override
        public String getWidgetName() {
            return "codeanalysis";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }
    }

    /**
     * Entry to create Library Scan in Code Quality Widget
     *
     */
    public static class LibraryScanEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.LibraryPolicy;
        }

        @Override
        public String getWidgetId() {
            return "codeanalysis0";
        }

        @Override
        public String getWidgetName() {
            return "codeanalysis";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }

    }

    /**
     * Entry to create Security Scan in Code Quality Widget
     */
    public static class SecurityScanEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.StaticSecurityScan;
        }

        @Override
        public String getWidgetId() {
            return "codeanalysis0";
        }

        @Override
        public String getWidgetName() {
            return "codeanalysis";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }
    }

    /**
     * Entry to create Functional Test in Code Quality Widget
     */
    public static class FunctionalTestEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.Test;
        }

        @Override
        public String getWidgetId() {
            return "codeanalysis0";
        }

        @Override
        public String getWidgetName() {
            return "codeanalysis";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }
    }

    /**
     * Entry to create Functional Test in Code Quality Widget
     */
    public static class PerformanceTestEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.Test;
        }

        @Override
        public String getWidgetId() {
            return "codeanalysis0";
        }

        @Override
        public String getWidgetName() {
            return "codeanalysis";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }
    }


    /**
     * Entry to create Functional Test in Code Quality Widget
     */
    public static class ArtifactEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.Artifact;
        }

        @Override
        public String getWidgetId() {
            return "build0";
        }

        @Override
        public String getWidgetName() {
            return "build";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }
    }


    /**
     * Entry to create Deployment widget
     */
    public static class DeploymentEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.Deployment;
        }

        @Override
        public String getWidgetId() {
            return "deploy0";
        }

        @Override
        public String getWidgetName() {
            return "deploy";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }

    }

    /**
     * Entry to create InfraStructureScan widget
     */
    public static class InfraStructureScanEntry extends Entry {

        @Override
        public CollectorType getType() {
            return CollectorType.InfrastructureScan;
        }

        @Override
        public String getWidgetId() {
            return "infrascan0";
        }

        @Override
        public String getWidgetName() {
            return "infrascan";
        }

        @Override
        public Map<String, Object> toWidgetOptions() {
            Map<String, Object> opts = new HashMap<>();
            opts.put("id", getWidgetId());
            return opts;
        }
    }

    // Getters and setters

    public DashboardMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(DashboardMetaData metaData) {
        this.metaData = metaData;
    }

    public List<FeatureEntry> getFeatureEntries() {
        return featureEntries;
    }

    public void setFeatureEntries(List<FeatureEntry> featureEntries) {
        this.featureEntries = featureEntries;
    }

    public List<CodeRepoEntry> getCodeRepoEntries() {
        return codeRepoEntries;
    }

    public void setCodeRepoEntries(List<CodeRepoEntry> codeRepoEntries) {
        this.codeRepoEntries = codeRepoEntries;
    }

    public List<BuildEntry> getBuildEntries() {
        return buildEntries;
    }

    public void setBuildEntries(List<BuildEntry> buildEntries) {
        this.buildEntries = buildEntries;
    }

    public List<StaticCodeEntry> getStaticCodeEntries() {
        return staticCodeEntries;
    }

    public void setStaticCodeEntries(List<StaticCodeEntry> staticCodeEntries) {
        this.staticCodeEntries = staticCodeEntries;
    }

    public List<SecurityScanEntry> getSecurityScanEntries() {
        return securityScanEntries;
    }

    public void setSecurityScanEntries(List<SecurityScanEntry> securityScanEntries) {
        this.securityScanEntries = securityScanEntries;
    }

    public List<DeploymentEntry> getDeploymentEntries() {
        return deploymentEntries;
    }

    public void setDeploymentEntries(List<DeploymentEntry> deploymentEntries) {
        this.deploymentEntries = deploymentEntries;
    }

    public List<LibraryScanEntry> getLibraryScanEntries() {
        return libraryScanEntries;
    }

    public void setLibraryScanEntries(List<LibraryScanEntry> libraryScanEntries) {
        this.libraryScanEntries = libraryScanEntries;
    }

    public List<FunctionalTestEntry> getFunctionalTestEntries() {
        return functionalTestEntries;
    }

    public void setFunctionalTestEntries(List<FunctionalTestEntry> functionalTestEntries) {
        this.functionalTestEntries = functionalTestEntries;
    }

    public List<ArtifactEntry> getArtifactEntries() {
        return artifactEntries;
    }

    public void setArtifactEntries(List<ArtifactEntry> artifactEntries) {
        this.artifactEntries = artifactEntries;
    }

    public List<PerformanceTestEntry> getPerformanceTestEntries() {
        return performanceTestEntries;
    }

    public void setPerformanceTestEntries(List<PerformanceTestEntry> performanceTestEntries) {
        this.performanceTestEntries = performanceTestEntries;
    }

    public List<InfraStructureScanEntry> getInfraStructureScanEntries() {
        return infraStructureScanEntries;
    }

    public void setInfraStructureScanEntries(List<InfraStructureScanEntry> infraStructureScanEntries) {
        this.infraStructureScanEntries = infraStructureScanEntries;
    }


    public List<Entry> getAllEntries() {
        List<Entry> all = new ArrayList<>();
        all.addAll(buildEntries);
        all.addAll(codeRepoEntries);
        all.addAll(staticCodeEntries);
        all.addAll(libraryScanEntries);
        all.addAll(securityScanEntries);
        all.addAll(functionalTestEntries);
        all.addAll(deploymentEntries);
        all.addAll(featureEntries);
        all.addAll(artifactEntries);
        all.addAll(performanceTestEntries);
        all.addAll(infraStructureScanEntries);
        return all;
    }
}
