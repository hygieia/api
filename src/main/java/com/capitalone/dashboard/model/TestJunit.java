package com.capitalone.dashboard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestJunit {


    private String tests;

    private String failures;

    private String name;

    private String time;

    private String errors;


    private Properties properties;

    @JacksonXmlElementWrapper(localName = "testcase", useWrapping = false)
    private List<Testcase> testcase;

    private String skipped;

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

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<Testcase> getTestcase() {
        return testcase;
    }

    public void setTestcase(List<Testcase> testcase) {
        this.testcase = testcase;
    }

    public String getSkipped() {
        return skipped;
    }

    public void setSkipped(String skipped) {
        this.skipped = skipped;
    }

    @Override
    public String toString() {
        return "TestJunit{" +
                "tests='" + tests + '\'' +
                ", failures='" + failures + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                ", errors='" + errors + '\'' +
                ", properties=" + properties +
                ", testcase=" + testcase +
                ", skipped='" + skipped + '\'' +
                '}';
    }


    public static class Error {
        private String message;

        private String type;

        private String content;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "message='" + message + '\'' +
                    ", type='" + type + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    @JacksonXmlRootElement(localName = "testcase")
    public static class Testcase {

        @JacksonXmlProperty(localName = "classname")
        private String classname;

        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;

        @JacksonXmlProperty(localName = "time")
        private String time;

        public String getClassname() {
            return classname;
        }

        public void setClassname(String classname) {
            this.classname = classname;
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

        @Override
        public String toString() {
            return "Testcase{" +
                    "classname='" + classname + '\'' +
                    ", name='" + name + '\'' +
                    ", time='" + time + '\'' +
                    '}';
        }
    }


    @JacksonXmlRootElement(localName = "properties")
    public static class Properties {
        @JacksonXmlProperty(localName = "property")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Property> property;

        public List<Property> getProperty() {
            return property;
        }

        public void setProperty(List<Property> property) {
            this.property = property;
        }

        @Override
        public String toString() {
            return "Properties{" +
                    "property=" + property +
                    '}';
        }
    }

    @JacksonXmlRootElement(localName = "property")
    public static class Property {
        @JacksonXmlProperty(isAttribute = true)
        private String name;

        @JacksonXmlProperty
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Property{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }


}


