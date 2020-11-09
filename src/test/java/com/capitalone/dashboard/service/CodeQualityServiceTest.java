package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityMetric;
import com.capitalone.dashboard.model.CodeQualityMetricStatus;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.Cmdb;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.CmdbRepository;
import com.capitalone.dashboard.request.CodeQualityCreateRequest;
import com.capitalone.dashboard.request.CodeQualityRequest;
import com.google.common.collect.Lists;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CodeQualityServiceTest {

    @Mock private CodeQualityRepository codeQualityRepository;
    @Mock private CollectorRepository collectorRepository;
    @Mock private CollectorService collectorService;
    @Mock private ComponentRepository componentRepository;
    @Mock private DashboardRepository dashboardRepository;
    @Mock private CmdbRepository cmdbRepository;
    @InjectMocks private CodeQualityServiceImpl codeQualityService;
    @Rule public ExpectedException thrown = ExpectedException.none();


    @Test
    public void createWithGoodRequest() throws HygieiaException {
        ObjectId collectorId = ObjectId.get();

        CodeQualityCreateRequest request = makeCodeQualityRequest();

        when(collectorRepository.findOne(collectorId)).thenReturn(new Collector());
        when(collectorService.createCollector(any(Collector.class))).thenReturn(new Collector());
        when(collectorService.createCollectorItem(any(CollectorItem.class))).thenReturn(new CollectorItem());

        CodeQuality codeQuality = makeCodeQualityStatic();

        when(codeQualityRepository.save(any(CodeQuality.class))).thenReturn(codeQuality);
        String response = codeQualityService.create(request);
        String expected = codeQuality.getId().toString();
        assertEquals(response, expected);
    }

    @Test
    public void createV2WithGoodRequest() throws HygieiaException {
        ObjectId collectorId = ObjectId.get();

        CodeQualityCreateRequest request = makeCodeQualityRequest();

        when(collectorRepository.findOne(collectorId)).thenReturn(new Collector());
        when(collectorService.createCollector(any(Collector.class))).thenReturn(new Collector());
        when(collectorService.createCollectorItem(any(CollectorItem.class))).thenReturn(new CollectorItem());

        CodeQuality codeQuality = makeCodeQualityStatic();

        when(codeQualityRepository.save(any(CodeQuality.class))).thenReturn(codeQuality);
        String response = codeQualityService.createV2(request);
        String expected = codeQuality.getId().toString() + "," + codeQuality.getCollectorItemId();
        assertEquals(response, expected);
    }

    @Test
    public void getCollectorItemTest() {
        CodeQualityRequest request = new CodeQualityRequest();
        when(componentRepository.findOne(request.getComponentId())).thenReturn(null);

        CollectorItem item = codeQualityService.getCollectorItem(request);

        Assert.assertNull(item);
    }

    @Test
    public void test_getCmdbOfSonarProject_noCodeQuality() throws HygieiaException {
        when(codeQualityRepository.findByNameAndVersion(Matchers.anyString(), Matchers.anyString())).thenReturn(new ArrayList<>());
        thrown.expect(HygieiaException.class);
        thrown.expectMessage("code analysis data not exists");
        codeQualityService.getCmdb("", "");
    }

    @Test
    public void test_getCmdbOfSonarProject_noComponent() throws HygieiaException {
        when(codeQualityRepository.findByNameAndVersion(Matchers.anyString(), Matchers.anyString())).thenReturn(Lists.newArrayList(makeCodeQualityStatic()));
        when(componentRepository.findByCodeQualityCollectorItems(Matchers.any(ObjectId.class))).thenReturn(new ArrayList<>());
        thrown.expect(HygieiaException.class);
        thrown.expectMessage("dashboard component not exists");
        codeQualityService.getCmdb("MyTest", "1.0.0.1");
    }

    @Test
    public void test_getCmdbOfSonarProject_noDashboard() throws HygieiaException {
        when(codeQualityRepository.findByNameAndVersion(Matchers.anyString(), Matchers.anyString())).thenReturn(Lists.newArrayList(makeCodeQualityStatic()));
        when(componentRepository.findByCodeQualityCollectorItems(Matchers.any(ObjectId.class))).thenReturn(Lists.newArrayList(new Component("test-comp")));
        when(dashboardRepository.findByApplicationComponentIdsIn(Matchers.anyCollectionOf(ObjectId.class))).thenReturn(new ArrayList<>());
        thrown.expect(HygieiaException.class);
        thrown.expectMessage("dashboard not exists");
        codeQualityService.getCmdb("MyTest", "1.0.0.1");
    }

    @Test
    public void test_getCmdbOfSonarProject_noCmdb() throws HygieiaException {
        when(codeQualityRepository.findByNameAndVersion(Matchers.anyString(), Matchers.anyString())).thenReturn(Lists.newArrayList(makeCodeQualityStatic()));
        when(componentRepository.findByCodeQualityCollectorItems(Matchers.any(ObjectId.class))).thenReturn(Lists.newArrayList(new Component("test-comp")));
        Dashboard dashboard = new Dashboard(false,null,null,null,null,null,null,
                null,null,false,null);
        when(dashboardRepository.findByApplicationComponentIdsIn(Matchers.anyCollectionOf(ObjectId.class))).thenReturn(Lists.newArrayList(dashboard));
        when(cmdbRepository.findByConfigurationItemAndItemTypeAndValidConfigItem(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean())).thenReturn(null);
        thrown.expect(HygieiaException.class);
        thrown.expectMessage("valid cmdb not exists");
        codeQualityService.getCmdb("MyTest", "1.0.0.1");
    }

    @Test
    public void test_getCmdbOfSonarProject_validCmdb() throws HygieiaException {
        when(codeQualityRepository.findByNameAndVersion(Matchers.anyString(), Matchers.anyString())).thenReturn(Lists.newArrayList(makeCodeQualityStatic()));
        when(componentRepository.findByCodeQualityCollectorItems(Matchers.any(ObjectId.class))).thenReturn(Lists.newArrayList(new Component("test-comp")));
        Dashboard dashboard = new Dashboard(false,null,null,null,null,null,null,
                null,null,false,null);
        when(dashboardRepository.findByApplicationComponentIdsIn(Matchers.anyCollectionOf(ObjectId.class))).thenReturn(Lists.newArrayList(dashboard));
        when(cmdbRepository.findByConfigurationItemAndItemTypeAndValidConfigItem(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean()))
                .thenReturn(new Cmdb());
        codeQualityService.getCmdb("MyTest", "1.0.0.1");
    }

    private CodeQualityCreateRequest makeCodeQualityRequest() {
        CodeQualityCreateRequest quality = new CodeQualityCreateRequest();
        quality.setHygieiaId(ObjectId.get().toString());
        quality.setProjectId("1234");
        quality.setTimestamp(1);
        quality.setProjectName("MyTest");
        quality.setType(CodeQualityType.StaticAnalysis);
        quality.setProjectUrl("http://mycompany.sonar.com/MyTest");
        quality.setServerUrl("http://mycompany.sonar.com");
        quality.setProjectVersion("1.0.0.1");
        quality.getMetrics().add(makeMetric());
        return quality;
    }

    private CodeQuality makeCodeQualityStatic() {
        CodeQuality quality = new CodeQuality();
        quality.setId(ObjectId.get());
        quality.setCollectorItemId(ObjectId.get());
        quality.setTimestamp(1);
        quality.setName("MyTest");
        quality.setType(CodeQualityType.StaticAnalysis);
        quality.setUrl("http://mycompany.sonar.com/MyTest");
        quality.setVersion("1.0.0.1");
        quality.getMetrics().add(makeMetric());
        return quality;
    }


    private CodeQualityMetric makeMetric() {
        CodeQualityMetric metric = new CodeQualityMetric("critical");
        metric.setFormattedValue("10");
        metric.setStatus(CodeQualityMetricStatus.Ok);
        metric.setStatusMessage("Ok");
        metric.setValue("0");
        return metric;
    }

    private int intVal(long value) {
        return Long.valueOf(value).intValue();
    }


}
