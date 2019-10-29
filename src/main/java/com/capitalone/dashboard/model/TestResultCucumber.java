package com.capitalone.dashboard.model;

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


    @Override
    public String toString() {
        return "TestResultCucumber{" +
                "line='" + line + '\'' +
                ", elements=" + Arrays.toString(elements) +
                ", name='" + name + '\'' +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
