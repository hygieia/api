package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.CommitRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.request.CommitRequest;
import com.querydsl.core.types.Predicate;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CommitServiceTest {
    @Mock
    private ComponentRepository componentRepository;
    @Mock
    private CollectorRepository collectorRepository;
    @Mock
    private CommitRepository commitRepository;
    @InjectMocks
    private CommitServiceImpl commitService;

    @Test
    public void searchTest() {
        ObjectId componentId = ObjectId.get();
        ObjectId collectorItemId = ObjectId.get();
        ObjectId collectorId = ObjectId.get();

        Collector collector = new Collector();
        collector.setId(collectorId);

        CommitRequest request = new CommitRequest();
        request.setComponentId(componentId);

        when(componentRepository.findById(request.getComponentId())).thenReturn(java.util.Optional.of(makeComponent(collectorItemId, collectorId, true)));
        when(collectorRepository.findById(collectorId)).thenReturn(java.util.Optional.of(collector));

        commitService.search(request);

        verify(commitRepository, times(1)).findAll((Predicate) anyObject());
    }

    @Test
    @Ignore
    public void search_Empty_Response_No_Component() {
        CommitRequest request = new CommitRequest();

        when(componentRepository.findById(request.getComponentId())).thenReturn(null);

        DataResponse<Iterable<Commit>> response = commitService.search(request);

        List<Commit> result = (List<Commit>) response.getResult();
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void search_Empty_Response_No_CollectorItems() {
        ObjectId componentId = ObjectId.get();
        ObjectId collectorItemId = ObjectId.get();
        ObjectId collectorId = ObjectId.get();

        CommitRequest request = new CommitRequest();
        request.setComponentId(componentId);

        when(componentRepository.findById(request.getComponentId())).thenReturn(java.util.Optional.of(makeComponent(collectorItemId, collectorId, false)));

        DataResponse<Iterable<Commit>> response = commitService.search(request);

        List<Commit> result = (List<Commit>) response.getResult();
        Assert.assertEquals(0, result.size());
    }

    private Component makeComponent(ObjectId collectorItemId, ObjectId collectorId, boolean populateCollectorItems) {
        CollectorItem item = new CollectorItem();
        item.setId(collectorItemId);
        item.setCollectorId(collectorId);
        Component c = new Component();
        if (populateCollectorItems) {
            c.getCollectorItems().put(CollectorType.SCM, Collections.singletonList(item));
        }
        return c;
    }
}
