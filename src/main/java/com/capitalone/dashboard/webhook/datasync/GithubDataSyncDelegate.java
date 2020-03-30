package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.GitRequest;
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

public class GithubDataSyncDelegate {
    private static final Log LOG = LogFactory.getLog(GithubDataSyncDelegate.class);

    private DataSyncServiceImpl dataSyncServiceImpl;
    private DataSyncUtils dataSyncUtils;
    private static final int ZERO = 0;

    public GithubDataSyncDelegate(DataSyncServiceImpl dataSyncServiceImpl, DataSyncUtils dataSyncUtils) {
        this.dataSyncServiceImpl = dataSyncServiceImpl;
        this.dataSyncUtils = dataSyncUtils;
    }

    public DataSyncResponse clean(Collector collector) {
        int pages = dataSyncUtils.pages(collector);
        String collectorName = collector.getName();
        List<CollectorItem> collectorItems = dataSyncUtils.getAllCollectorItems(collector, pages);
        if (CollectionUtils.isEmpty(collectorItems)) return dataSyncUtils.warn(collectorName,"No collector-items found");
        int componentCount = ZERO;
        int collectorItemsCount = ZERO;
        List<String> componentIds = new ArrayList<>();
        for (int idx = ZERO; idx <= collectorItems.size(); idx++) {
            if (idx == collectorItems.size()) break;
            Iterable<CollectorItem> suspects = dataSyncUtils.findAllCollectorItemsByOptions(collectorItems.get(idx), collector);
            if (IterableUtils.isEmpty(suspects)) continue;
            List<GitRequest> grs = new ArrayList<>();
            List<Component> components = new ArrayList<>();
            suspects.forEach(suspect -> {
                GitRequest gitRequest = dataSyncServiceImpl.getGitRequestRepository().findTopByCollectorItemIdOrderByTimestampDesc(suspect.getId());
                grs.add(gitRequest);
                List<Component> cs = dataSyncServiceImpl.getComponentRepository().findBySCMCollectorItemId(suspect.getId());
                components.addAll(cs);
            });
            LOG.info("collectorItem run +++" + idx + " of " + collectorItems.size());
            if (!CollectionUtils.isEmpty(grs)) {
                List<GitRequest> gs = grs.stream().filter(Objects::nonNull).collect(Collectors.toList());
                gs.sort(Comparator.comparing(GitRequest::getTimestamp).reversed());
                GitRequest pullRequest = gs.stream().filter(Objects::nonNull).findFirst().orElse(null);
                if (Objects.nonNull(pullRequest)) {
                    CollectorItem collectorItem = dataSyncServiceImpl.getCollectorItemRepository().findOne(pullRequest.getCollectorItemId());
                    List<CollectorItem> suspectCollectorItems = dataSyncUtils.deleteCollectorItems(collectorItems, collectorItem, suspects);
                    collectorItemsCount += suspectCollectorItems.size();
                    if (CollectionUtils.isEmpty(components)) continue;
                    components.forEach(component -> {
                        componentIds.add(component.getId().toString());
                    });
                    int comp = dataSyncUtils.updateComponents(collector, components, collectorItem, CollectorType.SCM);
                    componentCount = componentCount + comp;
                } else {
                    componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components, collector, CollectorType.SCM);
                }
            } else {
                componentCount = dataSyncUtils.clearDuplicateCollectorItemsAndUpdateComponents(collectorItems, componentCount, suspects, components, collector, CollectorType.SCM);
            }
        }
        return new DataSyncResponse(componentIds, collectorItemsCount,collectorName + " refresh Successful==>> Updated " + componentCount + " components and " + collectorItemsCount + " collectorItems.");
    }
}