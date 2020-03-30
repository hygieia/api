package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.request.DataSyncResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StaticSecurityDataSyncDelegate {
    private static final Log LOG = LogFactory.getLog(StaticSecurityDataSyncDelegate.class);
    private DataSyncServiceImpl dataSyncServiceImpl;
    private DataSyncUtils dataSyncUtils;

    public StaticSecurityDataSyncDelegate(DataSyncServiceImpl dataSyncServiceImpl, DataSyncUtils dataSyncUtils) {
        this.dataSyncServiceImpl = dataSyncServiceImpl;
        this.dataSyncUtils = dataSyncUtils;
    }

    public DataSyncResponse clean(Collector collector) {
        int pages = dataSyncUtils.pages(collector);
        String collectorName = collector.getName();
        List<CollectorItem> collectorItems = dataSyncUtils.getAllCollectorItems(collector, pages);
        if (CollectionUtils.isEmpty(collectorItems)) return dataSyncUtils.warn(collectorName,"No collector-items found");
        int componentCount = 0;
        int collectorItemsCount = 0;
        List<String> componentIds = new ArrayList<>();
        for (int idx = 0; idx <= collectorItems.size(); idx++) {
            if (idx == collectorItems.size()) break;
            Iterable<CollectorItem> suspects = dataSyncUtils.findAllCollectorItemsByOptions(collectorItems.get(idx), collector);
            if (IterableUtils.isEmpty(suspects)) continue;
            List<CodeQuality> cqs = new ArrayList<>();
            List<Component> components = new ArrayList<>();
            suspects.forEach(suspect -> {
                CodeQuality codeQuality = dataSyncServiceImpl.getCodeQualityRepository().findTop1ByCollectorItemIdOrderByTimestampDesc(suspect.getId());
                cqs.add(codeQuality);
                List<Component> cs = dataSyncServiceImpl.getComponentRepository().findByStaticSecurityScanCollectorItems(suspect.getId());
                components.addAll(cs);
            });
            LOG.info("collectorItem run +++" + idx + " of " + collectorItems.size());
            if (!CollectionUtils.isEmpty(cqs)) {
                List<CodeQuality> cq = cqs.stream().filter(Objects::nonNull).collect(Collectors.toList());
                cq.sort(Comparator.comparing(CodeQuality::getTimestamp).reversed());
                CodeQuality staticSecurity = cq.stream().filter(Objects::nonNull).findFirst().orElse(null);
                if (Objects.nonNull(staticSecurity)) {
                    CollectorItem collectorItem = dataSyncServiceImpl.getCollectorItemRepository().findOne(staticSecurity.getCollectorItemId());
                    List<CollectorItem> suspectCollectorItems = dataSyncUtils.deleteCollectorItems(collectorItems, collectorItem, suspects);
                    collectorItemsCount += suspectCollectorItems.size();
                    if (CollectionUtils.isEmpty(components)) continue;
                    components.forEach(component -> {
                        componentIds.add(component.getId().toString());
                    });
                    componentCount += components.size();
                    dataSyncUtils.updateComponents(collector, components, collectorItem, CollectorType.StaticSecurityScan);
                } else {
                    componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components, collector, CollectorType.StaticSecurityScan);

                }
            } else {
                componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components, collector, CollectorType.StaticSecurityScan);
            }
        }
        return new DataSyncResponse(componentIds,collectorItemsCount, collectorName + " refresh Successful==>> Updated " + componentCount + " components and " + collectorItemsCount + " collectorItems.");
    }
}