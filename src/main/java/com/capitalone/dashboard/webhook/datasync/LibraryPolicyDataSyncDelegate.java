package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.request.DataSyncResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LibraryPolicyDataSyncDelegate {
    private static final Log LOG = LogFactory.getLog(LibraryPolicyDataSyncDelegate.class);
    private  DataSyncServiceImpl dataSyncServiceImpl;
    private DataSyncUtils dataSyncUtils;

    public LibraryPolicyDataSyncDelegate(DataSyncServiceImpl dataSyncServiceImpl,DataSyncUtils dataSyncUtils) {
        this.dataSyncServiceImpl = dataSyncServiceImpl;
        this.dataSyncUtils = dataSyncUtils;
    }

    public DataSyncResponse clean(Collector collector) {
        int pages = dataSyncUtils.pages(collector);
        String collectorName = collector.getName();
        List<CollectorItem> collectorItems = dataSyncUtils.getAllCollectorItems(collector,pages);
        if (CollectionUtils.isEmpty(collectorItems)) return dataSyncUtils.warn(collectorName,"No collector-items found");
        int componentCount = 0;
        int collectorItemsCount = 0;
        List<String> componentIds = new ArrayList<>();
        for (int idx = 0; idx <= collectorItems.size(); idx++) {
            if(idx == collectorItems.size()) break;
            Iterable<CollectorItem> suspects = dataSyncUtils.findAllCollectorItemsByOptions(collectorItems.get(idx), collector);
            if (IterableUtils.isEmpty(suspects)) continue;
                List<LibraryPolicyResult> lps = new ArrayList<>();
                List<Component> components = new ArrayList<>();
                suspects.forEach(suspect -> {
                    LibraryPolicyResult libraryPolicyResult = dataSyncServiceImpl.getLibraryPolicyResultsRepository().findTopByCollectorItemIdOrderByTimestampDesc(suspect.getId());
                    lps.add(libraryPolicyResult);
                    List<Component> cs = dataSyncServiceImpl.getComponentRepository().findByLibraryPolicyCollectorItems(suspect.getId());
                    components.addAll(cs);
                });
                LOG.info("collectorItem run +++" + idx + " of " + collectorItems.size());
                if (!CollectionUtils.isEmpty(lps)) {
                    List<LibraryPolicyResult> lp = lps.stream().filter(Objects::nonNull).collect(Collectors.toList());
                    lp.sort(Comparator.comparing(LibraryPolicyResult::getTimestamp).reversed());
                    LibraryPolicyResult libraryPolicyResult = lp.stream().filter(Objects::nonNull).findFirst().orElse(null);
                    if (Objects.nonNull(libraryPolicyResult)) {
                        CollectorItem collectorItem = dataSyncServiceImpl.getCollectorItemRepository().findOne(libraryPolicyResult.getCollectorItemId());
                        List<CollectorItem> suspectCollectorItems = dataSyncUtils.deleteCollectorItems(collectorItems,collectorItem,suspects);
                        collectorItemsCount += suspectCollectorItems.size();
                        if (CollectionUtils.isEmpty(components)) continue;
                        components.forEach(component -> {
                            componentIds.add(component.getId().toString());
                        });
                        componentCount += components.size();
                        dataSyncUtils.updateComponents(collector,components,collectorItem,CollectorType.LibraryPolicy);
                    } else {
                        componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components,collector,CollectorType.LibraryPolicy);
                    }
                } else {
                    componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components,collector,CollectorType.LibraryPolicy);
                }

        }
        return new DataSyncResponse(componentIds,collectorItemsCount,collectorName + " refresh Successful==>> Updated " + componentCount + " components and " + collectorItemsCount + " collectorItems.");
    }
}