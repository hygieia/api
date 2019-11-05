package com.capitalone.dashboard.model;

import org.apache.commons.lang.builder.ToStringBuilder;

public class TestPerformance {


    private PerformanceMetrics performanceMetrics;

    private String testType;

    private String testId;

    private String testAgentType;

    private String componentName;

    private String status;

    private String testRequestId;

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    private long timestamp;

    private String buildJobId;

    private String applicationName;

    private String bapComponentName;


    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestAgentType() {
        return testAgentType;
    }

    public void setTestAgentType(String testAgentType) {
        this.testAgentType = testAgentType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTestRequestId() {
        return testRequestId;
    }

    public void setTestRequestId(String testRequestId) {
        this.testRequestId = testRequestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
                .append("performanceMetrics", performanceMetrics)
                .append("testType", testType)
                .append("testId", testId)
                .append("testAgentType", testAgentType)
                .append("componentName", componentName)
                .append("status", status)
                .append("testRequestId", testRequestId)
                .append("timestamp", timestamp)
                .append("buildJobId", buildJobId)
                .append("applicationName", applicationName)
                .append("bapComponentName", bapComponentName)
                .toString();
    }

    public static class PerformanceMetrics {
        private ActualResults actualResults;

        private BenchmarkUsed benchmarkUsed;

        private String testRunStatus;

        private String startTime;

        private String endTime;

        private String message;

        private String testSetName;

        private String testDuration;

        public ActualResults getActualResults() {
            return actualResults;
        }

        public void setActualResults(ActualResults actualResults) {
            this.actualResults = actualResults;
        }

        public BenchmarkUsed getBenchmarkUsed() {
            return benchmarkUsed;
        }

        public void setBenchmarkUsed(BenchmarkUsed benchmarkUsed) {
            this.benchmarkUsed = benchmarkUsed;
        }

        public String getTestRunStatus() {
            return testRunStatus;
        }

        public void setTestRunStatus(String testRunStatus) {
            this.testRunStatus = testRunStatus;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTestSetName() {
            return testSetName;
        }

        public void setTestSetName(String testSetName) {
            this.testSetName = testSetName;
        }

        public String getTestDuration() {
            return testDuration;
        }

        public void setTestDuration(String testDuration) {
            this.testDuration = testDuration;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("actualResults", actualResults)
                    .append("benchmarkUsed", benchmarkUsed)
                    .append("testRunStatus", testRunStatus)
                    .append("startTime", startTime)
                    .append("endTime", endTime)
                    .append("message", message)
                    .append("testSetName", testSetName)
                    .append("testDuration", testDuration)
                    .toString();
        }
    }

    public static class BenchmarkUsed {
        private String minResponseTime;

        private String responseTime;

        private String maxResponseTime;

        private String errorPercent;

        private String throughPut;

        public String getMinResponseTime() {
            return minResponseTime;
        }

        public void setMinResponseTime(String minResponseTime) {
            this.minResponseTime = minResponseTime;
        }

        public String getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(String responseTime) {
            this.responseTime = responseTime;
        }

        public String getMaxResponseTime() {
            return maxResponseTime;
        }

        public void setMaxResponseTime(String maxResponseTime) {
            this.maxResponseTime = maxResponseTime;
        }

        public String getErrorPercent() {
            return errorPercent;
        }

        public void setErrorPercent(String errorPercent) {
            this.errorPercent = errorPercent;
        }

        public String getThroughPut() {
            return throughPut;
        }

        public void setThroughPut(String throughPut) {
            this.throughPut = throughPut;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("minResponseTime", minResponseTime)
                    .append("responseTime", responseTime)
                    .append("maxResponseTime", maxResponseTime)
                    .append("errorPercent", errorPercent)
                    .append("throughPut", throughPut)
                    .toString();
        }
    }

    public static class ActualResults {
        private String minResponseTime;

        private String responseTime;

        private String maxResponseTime;

        private String errorPercent;

        private String throughPut;

        public String getMinResponseTime() {
            return minResponseTime;
        }

        public void setMinResponseTime(String minResponseTime) {
            this.minResponseTime = minResponseTime;
        }

        public String getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(String responseTime) {
            this.responseTime = responseTime;
        }

        public String getMaxResponseTime() {
            return maxResponseTime;
        }

        public void setMaxResponseTime(String maxResponseTime) {
            this.maxResponseTime = maxResponseTime;
        }

        public String getErrorPercent() {
            return errorPercent;
        }

        public void setErrorPercent(String errorPercent) {
            this.errorPercent = errorPercent;
        }

        public String getThroughPut() {
            return throughPut;
        }

        public void setThroughPut(String throughPut) {
            this.throughPut = throughPut;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("minResponseTime", minResponseTime)
                    .append("responseTime", responseTime)
                    .append("maxResponseTime", maxResponseTime)
                    .append("errorPercent", errorPercent)
                    .append("throughPut", throughPut)
                    .toString();
        }
    }

}
