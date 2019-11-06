package com.capitalone.dashboard.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;

@Document(
        collection = "test_results"
)
@CompoundIndexes({@CompoundIndex(
        name = "test_results_collItemId_ts_idx",
        def = "{'collectorItemId' : 1, 'timestamp': -1}"
)})
public class TestResultCucumber extends TestResult {


    public TestResultCucumber() {
    }

    private String line;

    private TestCucumber.Elements[] elements;

    private String name;

    private String keyword;

    private String buildJobId;

    private String applicationName;

    private String bapComponentName;


    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public TestCucumber.Elements[] getElements() {
        return elements;
    }

    public void setElements(TestCucumber.Elements[] elements) {
        this.elements = elements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getBuildJobId() {
        return buildJobId;
    }

    public void setBuildJobId(String buildJobId) {
        this.buildJobId = buildJobId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getBapComponentName() {
        return bapComponentName;
    }

    public void setBapComponentName(String bapComponentName) {
        this.bapComponentName = bapComponentName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("line", line)
                .append("elements", elements)
                .append("name", name)
                .append("keyword", keyword)
                .append("buildJobId", buildJobId)
                .append("applicationName", applicationName)
                .append("bapComponentName", bapComponentName)
                .toString();
    }
}
