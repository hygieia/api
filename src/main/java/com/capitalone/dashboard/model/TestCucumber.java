package com.capitalone.dashboard.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Arrays;

public class TestCucumber {


    private String line;

    private Elements[] elements;

    private String name;

    private String description;

    private String id;

    private String keyword;

    private String uri;

    private long timestamp;

    private String buildJobId;

    private String applicationName;

    private String bapComponentName;


    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public Elements[] getElements() {
        return elements;
    }

    public void setElements(Elements[] elements) {
        this.elements = elements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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


    public static class Elements {
        private Comments[] comments;

        private Before[] before;

        private String line;

        private String name;

        private String description;

        private String id;

        private After[] after;

        private String keyword;

        private String type;

        private Steps[] steps;

        private Tags[] tags;

        public Comments[] getComments() {
            return comments;
        }

        public void setComments(Comments[] comments) {
            this.comments = comments;
        }

        public Before[] getBefore() {
            return before;
        }

        public void setBefore(Before[] before) {
            this.before = before;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public After[] getAfter() {
            return after;
        }

        public void setAfter(After[] after) {
            this.after = after;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Steps[] getSteps() {
            return steps;
        }

        public void setSteps(Steps[] steps) {
            this.steps = steps;
        }

        public Tags[] getTags() {
            return tags;
        }

        public void setTags(Tags[] tags) {
            this.tags = tags;
        }


    }

    public static class After {
        private Result result;

        private Match match;

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public Match getMatch() {
            return match;
        }

        public void setMatch(Match match) {
            this.match = match;
        }


    }


    public static class Steps {
        private Result result;

        private String line;

        private String name;

        private Match match;

        private String keyword;

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Match getMatch() {
            return match;
        }

        public void setMatch(Match match) {
            this.match = match;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }


    }

    public static class Match {
        private String location;

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        public String toString() {
            return "Match [location = " + location + "]";
        }
    }


    public static class Before {
        private Result result;

        private Match match;

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public Match getMatch() {
            return match;
        }

        public void setMatch(Match match) {
            this.match = match;
        }


    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("line", line)
                .append("elements", elements)
                .append("name", name)
                .append("description", description)
                .append("id", id)
                .append("keyword", keyword)
                .append("uri", uri)
                .append("timestamp", timestamp)
                .append("buildJobId", buildJobId)
                .append("applicationName", applicationName)
                .append("bapComponentName", bapComponentName)
                .toString();
    }


    public static class Result {
        private String duration;

        private String status;

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("duration", duration)
                    .append("status", status)
                    .toString();
        }
    }


    public static class Comments {
        private String line;

        private String value;

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("line", line)
                    .append("value", value)
                    .toString();
        }
    }

    public static class Tags {
        private String line;

        private String name;

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("line", line)
                    .append("name", name)
                    .toString();
        }
    }


}
