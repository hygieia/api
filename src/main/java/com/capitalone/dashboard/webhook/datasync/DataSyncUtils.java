package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.request.DataSyncResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DataSyncUtils {

    private DataSyncServiceImpl dataSyncServiceImpl;
    private static final int MAX_PAGE_SIZE = 500;
    private static final int ZERO = 0;


    private static final Log LOG = LogFactory.getLog(DataSyncUtils.class);

    public DataSyncUtils(DataSyncServiceImpl dataSyncServiceImpl) {
        this.dataSyncServiceImpl = dataSyncServiceImpl;
    }

    public int pages(Collector collector) {
        Page<CollectorItem> collectorItemsPage = dataSyncServiceImpl.getCollectorItemRepository().findByCollectorIdIn(Collections.singleton(collector.getId()), new PageRequest(MAX_PAGE_SIZE, MAX_PAGE_SIZE));
        if (Objects.isNull(collectorItemsPage)) return ZERO;
        return collectorItemsPage.getTotalPages();
    }

    public List<CollectorItem> getAllCollectorItems(Collector collector, int total) {
        LOG.info("starting - collecting collector items");
        List<CollectorItem> collectorItems = new ArrayList<>();
        IntStream.range(ZERO, total).forEach(idx -> {
            Page<CollectorItem> collectorItemsPage = dataSyncServiceImpl.getCollectorItemRepository().findByCollectorIdIn(Collections.singleton(collector.getId()), new PageRequest(idx, MAX_PAGE_SIZE));
            collectorItems.addAll(collectorItemsPage.getContent());
            LOG.info("completed " + idx + " run");
        });
        LOG.info("Finished collecting collector-items");
        return collectorItems;
    }

    public int updateComponents(Collector collector, List<Component> components, CollectorItem cI, CollectorType collectorType) {
        return (int) components.stream().peek(component -> {
            component = updateCollectorItem(component, collectorType, cI, collector);
            dataSyncServiceImpl.getComponentRepository().save(component);
        }).count();
    }

    public Component updateCollectorItem(Component component, CollectorType collectorType, CollectorItem collectorItem, Collector collector) {
        Map<CollectorType, List<CollectorItem>> collectorItems = component.getCollectorItems();
        if (MapUtils.isEmpty(collectorItems)) return component;
        if (CollectionUtils.isEmpty(collectorItems.get(collectorType))) return component;
        List<CollectorItem> existing = new ArrayList<>(collectorItems.get(collectorType));
        List<CollectorItem> found = matchCollectorItems(existing, collectorItem, collector);
        if (CollectionUtils.isEmpty(found)) return component;
        found.stream().forEach(ci -> {
            existing.remove(ci);
        });
        collectorItem.setLastUpdated(System.currentTimeMillis());
        existing.add(collectorItem);
        collectorItems.put(collectorType, existing);
        return component;
    }

    public Map<String, Object> getUniqueOptions(CollectorItem collectorItem, Map<String, Object> uniqueFields) {
        return uniqueFields.keySet().stream().collect(Collectors.toMap(idx -> idx, idx -> collectorItem.getOptions().get(idx)));
    }

    public int clearDuplicateCollectorItemsAndUpdateComponents(List<CollectorItem> collectorItems, int componentCount, Iterable<CollectorItem> suspects, List<Component> components, Collector collector, CollectorType collectorType) {
        List<CollectorItem> suspectsList = IterableUtils.toList(suspects);
        suspectsList.sort(Comparator.comparing(CollectorItem::getLastUpdated).reversed());
        CollectorItem first = suspectsList.stream().findFirst().get();
        suspectsList.removeIf(sci -> sci.getId().equals(first.getId()));
        suspectsList.forEach(sci -> {
            collectorItems.removeIf(cItem -> cItem.getId().equals(sci.getId()));
            dataSyncServiceImpl.getCollectorItemRepository().delete(sci.getId());
        });
        if (CollectionUtils.isEmpty(components)) return componentCount;
        int componentsUpdated = updateComponents(collector, components, first, collectorType);
        componentCount += componentsUpdated;
        return componentCount;
    }

    private List<CollectorItem> matchCollectorItems(List<CollectorItem> existing, CollectorItem item, Collector collector) {
        Map<String, Object> uniqueOptions = collector.getUniqueFields();
        Map<String, Object> itemOptions = collector.getUniqueFields().keySet().stream().collect(Collectors.toMap(idx -> idx, idx -> item.getOptions().get(idx)));
        return Optional.of(existing).orElse(new ArrayList<>()).stream().filter(ci -> compareMaps(itemOptions, uniqueOptions.keySet().stream().collect(Collectors.toMap(idx -> idx, idx -> ci.getOptions().get(idx)))))
                .collect(Collectors.toList());
    }

    private boolean compareMaps(Map<String, Object> incoming, Map<String, Object> existing) {
        if (incoming.size() != existing.size()) return false;
        return incoming.entrySet().stream().allMatch(e -> e.getValue().equals(existing.get(e.getKey())));
    }

    public Iterable<CollectorItem> findAllCollectorItemsByOptions(CollectorItem collectorItem,Collector collector){
        return dataSyncServiceImpl.getCollectorItemRepository().findAllByOptionMapAndCollectorIdsIn(getUniqueOptions(collectorItem, collector.getUniqueFields()), Stream.of(collector.getId()).collect(Collectors.toList()));
    }

    public List<CollectorItem> deleteCollectorItems(List<CollectorItem> collectorItems, CollectorItem collectorItem,Iterable<CollectorItem> suspects) {
        List<CollectorItem> suspectCollectorItems = IterableUtils.toList(suspects);
        suspectCollectorItems.removeIf(cItem -> cItem.getId().equals(collectorItem.getId()));
        suspectCollectorItems.forEach(colItem -> {
            collectorItems.removeIf(cItem -> cItem.getId().equals(colItem.getId()));
            dataSyncServiceImpl.getCollectorItemRepository().delete(colItem.getId());
        });
        return suspectCollectorItems;
    }

    public DataSyncResponse warn(String collectorName, String message){
        return new DataSyncResponse(new ArrayList<>(),ZERO,message+" "+collectorName);
    }
}