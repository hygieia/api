package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.TestResult;
import com.capitalone.dashboard.request.DataSyncResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class TestDataSyncDelegate {
    private static final Log LOG = LogFactory.getLog(TestDataSyncDelegate.class);
    public static final String FUNCTIONAL = "Functional";
    public static final String PERFORMANCE = "Performance";
    public static final String TEST_TYPE = "testType";
    private DataSyncServiceImpl dataSyncServiceImpl;
    private DataSyncUtils dataSyncUtils;

    public TestDataSyncDelegate(DataSyncServiceImpl dataSyncServiceImpl, DataSyncUtils dataSyncUtils) {
        this.dataSyncServiceImpl = dataSyncServiceImpl;
        this.dataSyncUtils = dataSyncUtils;
    }

    public DataSyncResponse clean(Collector collector) {
        int total = dataSyncUtils.pages(collector);
        String collectorName = collector.getName();
        List<CollectorItem> collectorItems = dataSyncUtils.getAllCollectorItems(collector, total);
        if (CollectionUtils.isEmpty(collectorItems))
            return dataSyncUtils.warn(collectorName, "No collector-items found");
        int count = 0;
        int collectorItemsCount = 0;
        List<String> componentIds = new ArrayList<>();
        for (CollectorItem c : collectorItems) {
            TestResult testResult = dataSyncServiceImpl.getTestResultRepository().findTop1ByCollectorItemIdOrderByTimestampDesc(c.getId());
            LOG.info("collectorItem run +++" + count + " of " + collectorItems.size());
            if (Objects.nonNull(testResult)) {
                CollectorItem collectorItem = dataSyncServiceImpl.getCollectorItemRepository().findOne(testResult.getCollectorItemId());
                collectorItem.getOptions().put(TEST_TYPE, testResult.getType());
                dataSyncServiceImpl.getCollectorItemRepository().save(collectorItem);
                collectorItemsCount++;
            } else {
                //set functional and performance test type
                setTestType(c, dataSyncServiceImpl.getSettings().getDataSyncSettings().getRegexTestFunctional(), FUNCTIONAL);
                setTestType(c, dataSyncServiceImpl.getSettings().getDataSyncSettings().getRegexTestPerformance(), PERFORMANCE);
            }
            count++;
        }
        return new DataSyncResponse(componentIds, collectorItemsCount, collectorName + " refresh Successful==>> Updated " + collectorItemsCount + " collectorItems.");
    }

    private boolean isMatches(String regex, String charSequence) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(charSequence).matches();
    }

    private void setTestType(CollectorItem c, String regex, String type) {
        if (Objects.isNull(regex)) return;
        if (isMatches(regex, c.getDescription())) {
            c.getOptions().put(TEST_TYPE, type);
            dataSyncServiceImpl.getCollectorItemRepository().save(c);
        }
    }
}