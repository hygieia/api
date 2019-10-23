package com.capitalone.dashboard.model;

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

    @Override
    public String toString() {
        return "TestPerformance [performanceMetrics = " + performanceMetrics + ", testType = " + testType + ", testId = " + testId + ", testAgentType = " + testAgentType + ", componentName = " + componentName + ", status = " + status + ", testRequestId = " + testRequestId + "]";
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
            return "PerformanceMetrics [actualResults = " + actualResults + ", benchmarkUsed = " + benchmarkUsed + ", testRunStatus = " + testRunStatus + ", startTime = " + startTime + ", endTime = " + endTime + ", message = " + message + ", testSetName = " + testSetName + ", testDuration = " + testDuration + "]";
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
            return "BenchmarkUsed [minResponseTime = " + minResponseTime + ", responseTime = " + responseTime + ", maxResponseTime = " + maxResponseTime + ", errorPercent = " + errorPercent + ", throughPut = " + throughPut + "]";
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
            return "ActualResults [minResponseTime = " + minResponseTime + ", responseTime = " + responseTime + ", maxResponseTime = " + maxResponseTime + ", errorPercent = " + errorPercent + ", throughPut = " + throughPut + "]";
        }
    }

}
