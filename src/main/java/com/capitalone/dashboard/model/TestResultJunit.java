package com.capitalone.dashboard.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(
        collection = "test_results"
)
@CompoundIndexes({@CompoundIndex(
        name = "test_results_collItemId_ts_idx",
        def = "{'collectorItemId' : 1, 'timestamp': -1}"
)})
public class TestResultJunit extends TestResult {

    public TestResultJunit() {

    }

    private String tests;

    private String failures;

    private String name;

    private String time;

    private String errors;

    private TestJunit.Properties properties;

    private List<TestJunit.Testcase> testcase;

    private String skipped;

    private String buildJobId;

    private String serviceNowApplication;

    private String bapComponentName;

    public String getTests() {
        return tests;
    }

    public void setTests(String tests) {
        this.tests = tests;
    }

    public String getFailures() {
        return failures;
    }

    public void setFailures(String failures) {
        this.failures = failures;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public TestJunit.Properties getProperties() {
        return properties;
    }

    public void setProperties(TestJunit.Properties properties) {
        this.properties = properties;
    }

    public List<TestJunit.Testcase> getTestcase() {
        return testcase;
    }

    public void setTestcase(List<TestJunit.Testcase> testcase) {
        this.testcase = testcase;
    }

    public String getSkipped() {
        return skipped;
    }

    public void setSkipped(String skipped) {
        this.skipped = skipped;
    }

    public String getBuildJobId() {
        return buildJobId;
    }

    public void setBuildJobId(String buildJobId) {
        this.buildJobId = buildJobId;
    }

    public String getServiceNowApplication() {
        return serviceNowApplication;
    }

    public void setServiceNowApplication(String serviceNowApplication) {
        this.serviceNowApplication = serviceNowApplication;
    }

    public String getBapComponentName() {
        return bapComponentName;
    }

    public void setBapComponentName(String bapComponentName) {
        this.bapComponentName = bapComponentName;
    }

    @Override
    public String toString() {
        return "TestResultJunit{" +
                "tests='" + tests + '\'' +
                ", failures='" + failures + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                ", errors='" + errors + '\'' +
                ", properties=" + properties +
                ", testcase=" + testcase +
                ", skipped='" + skipped + '\'' +
                ", buildJobId='" + buildJobId + '\'' +
                ", serviceNowApplication='" + serviceNowApplication + '\'' +
                ", bapComponentName='" + bapComponentName + '\'' +
                '}';
    }
}
