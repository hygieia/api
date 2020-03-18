package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.BinaryArtifact;
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

public class ArtifactDataSyncDelegate {
    private static final Log LOG = LogFactory.getLog(ArtifactDataSyncDelegate.class);
    private DataSyncServiceImpl dataSyncServiceImpl;
    private DataSyncUtils dataSyncUtils;

    public ArtifactDataSyncDelegate(DataSyncServiceImpl dataSyncServiceImpl, DataSyncUtils dataSyncUtils) {
        this.dataSyncServiceImpl = dataSyncServiceImpl;
        this.dataSyncUtils = dataSyncUtils;
    }

    public DataSyncResponse clean(Collector collector) {
        int total = dataSyncUtils.pages(collector);
        String collectorName = collector.getName();
        List<CollectorItem> collectorItems = dataSyncUtils.getAllCollectorItems(collector, total);
        if (CollectionUtils.isEmpty(collectorItems)) return dataSyncUtils.warn(collectorName,"No collector-items found");
        int componentCount = 0;
        int collectorItemsCount = 0;
        List<String> componentIds = new ArrayList<>();
        for (int idx = 0; idx <= collectorItems.size(); idx++) {
            if (idx == collectorItems.size()) break;
            Iterable<CollectorItem> suspects = dataSyncUtils.findAllCollectorItemsByOptions(collectorItems.get(idx), collector);
            if (IterableUtils.isEmpty(suspects)) continue;
            List<BinaryArtifact> bas = new ArrayList<>();
            List<Component> components = new ArrayList<>();
            suspects.forEach(suspect -> {
                BinaryArtifact binaryArtifact = dataSyncServiceImpl.getBinaryArtifactRepository().findTopByCollectorItemIdOrderByTimestampDesc(suspect.getId());
                bas.add(binaryArtifact);
                List<Component> cs = dataSyncServiceImpl.getComponentRepository().findByArtifactCollectorItems(suspect.getId());
                components.addAll(cs);
            });
            LOG.info("collectorItem run +++" + idx + " of " + collectorItems.size());
            if (!CollectionUtils.isEmpty(bas)) {
                List<BinaryArtifact> bs = bas.stream().filter(Objects::nonNull).collect(Collectors.toList());
                bs.sort(Comparator.comparing(BinaryArtifact::getTimestamp).reversed());
                BinaryArtifact binaryArtifact = bs.stream().filter(Objects::nonNull).findFirst().orElse(null);
                if (Objects.nonNull(binaryArtifact)) {
                    CollectorItem collectorItem = dataSyncServiceImpl.getCollectorItemRepository().findOne(binaryArtifact.getCollectorItemId());
                    List<CollectorItem> suspectCollectorItems = dataSyncUtils.deleteCollectorItems(collectorItems, collectorItem, suspects);
                    collectorItemsCount += suspectCollectorItems.size();
                    if (CollectionUtils.isEmpty(components)) continue;
                    components.forEach(component -> {
                        componentIds.add(component.getId().toString());
                    });
                    componentCount += components.size();
                    dataSyncUtils.updateComponents(collector, components, collectorItem, CollectorType.Artifact);
                } else {
                    componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components, collector, CollectorType.Artifact);
                }
            } else {
                componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components, collector, CollectorType.Artifact);
            }
        }
        return new DataSyncResponse(componentIds, collectorItemsCount, collectorName + " refresh Successful==>> Updated " + componentCount + " components and " + collectorItemsCount + " collectorItems.");
    }
}