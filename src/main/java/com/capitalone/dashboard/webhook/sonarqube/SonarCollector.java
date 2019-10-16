package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.SonarProject;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SonarCollector extends Collector {
    private List<String> sonarServers = new ArrayList<>();
    private List<Double> sonarVersions = new ArrayList<>();
    private List<String> sonarMetrics = new ArrayList<>();
    private List<String> niceNames = new ArrayList<>();
    private static final String NICE_NAME = "niceName";
    private static final String PROJECT_NAME = "options.projectName";

    public List<String> getSonarServers() {
        return sonarServers;
    }

    public List<Double> getSonarVersions() {
        return sonarVersions;
    }

    public List<String> getSonarMetrics() {
        return sonarMetrics;
    }

    public List<String> getNiceNames() {
        return niceNames;
    }


    public void setNiceNames(List<String> niceNames) {
        this.niceNames = niceNames;
    }

}
