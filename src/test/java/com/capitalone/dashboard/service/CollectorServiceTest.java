package com.capitalone.dashboard.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CmdbRepository;

import com.google.common.collect.Lists;

import org.bson.types.ObjectId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class CollectorServiceTest {
	private static final String FILTER_STRING = "";

	@Rule public ExpectedException thrown = ExpectedException.none();
	@Mock private DashboardRepository dashboardRepository;
	@Mock private ComponentRepository componentRepository;
	@Mock private CollectorRepository collectorRepository;
	@Mock private CollectorItemRepository collectorItemRepository;
	@Mock private CmdbRepository cmdbRepository;
	@InjectMocks private CollectorServiceImpl collectorService;

	@Test
	@SuppressWarnings("unchecked")
	public void collectorItemsByType() {
		Collector c = makeCollector();
		CollectorItem item1 = makeCollectorItem(true);
		CollectorItem item2 = makeCollectorItem(true);
		when(collectorRepository.findByCollectorType(CollectorType.Build)).thenReturn(Arrays.asList(c));

		Page<CollectorItem> page = new PageImpl<CollectorItem>(Arrays.asList(item1, item2), null, 2);
		when(collectorItemRepository.findByCollectorIdAndSearchField(any(List.class),any(String.class), any(String.class),
				any(Pageable.class))).thenReturn(page);
		Page<CollectorItem> items = collectorService.collectorItemsByTypeWithFilter(CollectorType.Build, FILTER_STRING,
				null);
		assertThat(items.getTotalElements(), is(2L));
		assertTrue(items.getContent().contains(item1));
		assertTrue(items.getContent().contains(item2));
	}

	private Collector makeCollector() {
		Collector collector = new Collector();
		collector.setId(ObjectId.get());
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","");
		uniqueOptions.put("instanceUrl","");
		collector.setUniqueFields(uniqueOptions);
		return collector;
	}



	private CollectorItem makeCollectorItem(boolean enabled) {
		CollectorItem item = new CollectorItem();
		item.setId(ObjectId.get());
		item.setEnabled(enabled);
		return item;
	}

	private CollectorItem makeCollectorItemWithOptions(boolean enabled, Map<String,Object> options,ObjectId id,long timestamp) {
		CollectorItem item = new CollectorItem();
		item.setId(id);
		item.setEnabled(enabled);
		item.setOptions(options);
		item.setLastUpdated(timestamp);
		return item;
	}


	@Test
	public void testCreateCollectorItem(){
		Collector c = makeCollector();
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","A");
		uniqueOptions.put("instanceUrl","https://a.com");

		Map<String, Object> uniqueOptions2 = new HashMap<>();
		uniqueOptions2.put("projectName","A");
		uniqueOptions2.put("instanceUrl","https://a.com");
		uniqueOptions2.put("appName","Appname");
		uniqueOptions2.put("appId","appId");

		CollectorItem c1 = makeCollectorItemWithOptions(true,uniqueOptions,ObjectId.get(),1557332269095L);
		CollectorItem c2 = makeCollectorItemWithOptions(true,uniqueOptions2,ObjectId.get(),1557947220000L);
		when(collectorRepository.findOne(any(ObjectId.class))).thenReturn(c);
		when(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList())).thenReturn(Arrays.asList(c2));
		when(collectorItemRepository.save(any(CollectorItem.class))).thenReturn(c1);
		CollectorItem actual = collectorService.createCollectorItem(c1);
		assertTrue(actual.getId().equals(c2.getId()));
		verify(collectorRepository,times(1)).findOne(any(ObjectId.class));
		verify(collectorItemRepository,times(1)).findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList());
		verify(collectorItemRepository,times(1)).save(any(CollectorItem.class));
	}

	@Test
	public void testCreateCollectorItemWithMoreOptions(){
		Collector c = makeCollector();
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","A");
		uniqueOptions.put("instanceUrl","https://a.com");

		Map<String, Object> uniqueOptions2 = new HashMap<>();
		uniqueOptions2.put("projectName","A");
		uniqueOptions2.put("instanceUrl","https://a.com");
		uniqueOptions2.put("appName","Appname");
		uniqueOptions2.put("appId","appId");

		CollectorItem c1 = makeCollectorItemWithOptions(true,uniqueOptions,ObjectId.get(),1557947220000L);
		CollectorItem c2 = makeCollectorItemWithOptions(true,uniqueOptions2,ObjectId.get(),1557332269095L);
		when(collectorRepository.findOne(any(ObjectId.class))).thenReturn(c);
		when(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList())).thenReturn(Arrays.asList(c1));
		when(collectorItemRepository.save(any(CollectorItem.class))).thenReturn(c2);
		CollectorItem actual = collectorService.createCollectorItem(c2);
		assertTrue(actual.getId().equals(c1.getId()));
		verify(collectorRepository,times(1)).findOne(any(ObjectId.class));
		verify(collectorItemRepository,times(1)).findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList());
		verify(collectorItemRepository,times(1)).save(any(CollectorItem.class));
	}

	@Test
	public void testCreateCollectorItemWithMoreOptionsWithDisabled(){
		Collector c = makeCollector();
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","A");
		uniqueOptions.put("instanceUrl","https://a.com");

		Map<String, Object> uniqueOptions2 = new HashMap<>();
		uniqueOptions2.put("projectName","A");
		uniqueOptions2.put("instanceUrl","https://a.com");
		uniqueOptions2.put("appName","Appname");
		uniqueOptions2.put("appId","appId");

		CollectorItem c1 = makeCollectorItemWithOptions(false,uniqueOptions,ObjectId.get(),1557332269095L);
		CollectorItem c2 = makeCollectorItemWithOptions(true,uniqueOptions2,ObjectId.get(),1557332269095L);
		when(collectorRepository.findOne(any(ObjectId.class))).thenReturn(c);
		when(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList())).thenReturn(Arrays.asList(c1));
		when(collectorItemRepository.save(any(CollectorItem.class))).thenReturn(c2);
		CollectorItem actual = collectorService.createCollectorItem(c2);
		assertTrue(actual.getId().equals(c1.getId()));
		verify(collectorRepository,times(1)).findOne(any(ObjectId.class));
		verify(collectorItemRepository,times(1)).findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList());
		verify(collectorItemRepository,times(1)).save(any(CollectorItem.class));
	}


	@Test
	public void testCreateCollectorItemWithNonExisting(){
		Collector c = makeCollector();
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","A");
		uniqueOptions.put("instanceUrl","https://a.com");

		CollectorItem c1 = makeCollectorItemWithOptions(false,uniqueOptions,ObjectId.get(),1557332269095L);
		when(collectorRepository.findOne(any(ObjectId.class))).thenReturn(c);
		when(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList())).thenReturn(null);
		when(collectorItemRepository.save(any(CollectorItem.class))).thenReturn(c1);
		CollectorItem actual = collectorService.createCollectorItem(c1);
		assertNotNull(actual);
		verify(collectorRepository,times(1)).findOne(any(ObjectId.class));
		verify(collectorItemRepository,times(1)).findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList());
		verify(collectorItemRepository,times(1)).save(any(CollectorItem.class));
	}


	@Test
	public void testCreateCollectorItemWithMoreOptionsWithEnabledAndDisabled(){
		Collector c = makeCollector();
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","A");
		uniqueOptions.put("instanceUrl","https://a.com");

		Map<String, Object> uniqueOptions2 = new HashMap<>();
		uniqueOptions2.put("projectName","A");
		uniqueOptions2.put("instanceUrl","https://a.com");
		uniqueOptions2.put("appName","Appname");
		uniqueOptions2.put("appId","appId");


		Map<String, Object> uniqueOptions3 = new HashMap<>();
		uniqueOptions3.put("projectName","A");
		uniqueOptions3.put("instanceUrl","https://a.com");
		uniqueOptions3.put("appId","appId");


		CollectorItem c1 = makeCollectorItemWithOptions(false,uniqueOptions,ObjectId.get(),1557947220000L);
		CollectorItem c2 = makeCollectorItemWithOptions(true,uniqueOptions2,ObjectId.get(),1557947220000L);
		CollectorItem c3 = makeCollectorItemWithOptions(true,uniqueOptions3,ObjectId.get(),1557332269095L);
		when(collectorRepository.findOne(any(ObjectId.class))).thenReturn(c);
		when(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList())).thenReturn(Arrays.asList(c1,c3));
		when(collectorItemRepository.save(any(CollectorItem.class))).thenReturn(c2);
		CollectorItem actual = collectorService.createCollectorItem(c2);
		assertTrue(actual.getId().equals(c3.getId()));
		verify(collectorRepository,times(1)).findOne(any(ObjectId.class));
		verify(collectorItemRepository,times(1)).findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList());
		verify(collectorItemRepository,times(1)).save(any(CollectorItem.class));
	}

	@Test
	public void testCreateCollectorItemWithLastUpdatedAndEnabled(){
		Collector c = makeCollector();
		Map<String, Object> uniqueOptions = new HashMap<>();
		uniqueOptions.put("projectName","A");
		uniqueOptions.put("instanceUrl","https://a.com");

		Map<String, Object> uniqueOptions2 = new HashMap<>();
		uniqueOptions2.put("projectName","A");
		uniqueOptions2.put("instanceUrl","https://a.com");
		uniqueOptions2.put("appName","Appname");
		uniqueOptions2.put("appId","appId");


		Map<String, Object> uniqueOptions3 = new HashMap<>();
		uniqueOptions3.put("projectName","A");
		uniqueOptions3.put("instanceUrl","https://a.com");
		uniqueOptions3.put("appId","appId");


		CollectorItem c1 = makeCollectorItemWithOptions(true,uniqueOptions,ObjectId.get(),1557947220000L);
		CollectorItem c2 = makeCollectorItemWithOptions(true,uniqueOptions2,ObjectId.get(),1557947220000L);
		CollectorItem c3 = makeCollectorItemWithOptions(true,uniqueOptions3,ObjectId.get(),1557860820000L);
		CollectorItem c4 = makeCollectorItemWithOptions(true,uniqueOptions3,ObjectId.get(),1557949459000L);
		when(collectorRepository.findOne(any(ObjectId.class))).thenReturn(c);

		when(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList())).thenReturn(Arrays.asList(c1,c3,c4));
		when(collectorItemRepository.save(any(CollectorItem.class))).thenReturn(c2);
		CollectorItem actual = collectorService.createCollectorItem(c2);
		assertTrue(actual.getId().equals(c4.getId()));
		verify(collectorRepository,times(1)).findOne(any(ObjectId.class));
		verify(collectorItemRepository,times(1)).findAllByOptionMapAndCollectorIdsIn(anyMap(),anyList());
		verify(collectorItemRepository,times(1)).save(any(CollectorItem.class));
	}

	@Test
	public void test_getCmdbOfSonarProject_invalidProjectName() throws HygieiaException {
		thrown.expect(HygieiaException.class);
		thrown.expectMessage("invalid collectorName or projectName");
		collectorService.getCmdbByStaticAnalysis(null, null);
	}

	@Test
	public void test_getCmdbOfSonarProject_noCollector() throws HygieiaException {
		when(collectorRepository.findByName(Matchers.anyString())).thenReturn(null);
		thrown.expect(HygieiaException.class);
		thrown.expectMessage("collector not exists");
		collectorService.getCmdbByStaticAnalysis("colName","test");
	}

	@Test
	public void test_getCmdbOfSonarProject_noCollectorItems() throws HygieiaException {
		when(collectorRepository.findByName(Matchers.anyString())).thenReturn(new Collector("colName", CollectorType.CodeQuality));
		when(collectorItemRepository
				.findAllByOptionNameValueAndCollectorIdsIn(Matchers.anyString(), Matchers.anyString(), Matchers.anyList())).thenReturn(null);
		thrown.expect(HygieiaException.class);
		thrown.expectMessage("collector item not exists");
		collectorService.getCmdbByStaticAnalysis("colName","test");
	}

	@Test
	public void test_getCmdbOfSonarProject_noComponent() throws HygieiaException {
		when(collectorRepository.findByName(Matchers.anyString())).thenReturn(new Collector("colName", CollectorType.CodeQuality));
		when(collectorItemRepository
				.findAllByOptionNameValueAndCollectorIdsIn(Matchers.anyString(), Matchers.anyString(), Matchers.anyList()))
				.thenReturn(Lists.newArrayList(makeCollectorItem(false)));
		when(componentRepository.findByCollectorTypeAndItemIdIn(Matchers.any(CollectorType.class), Matchers.anyListOf(ObjectId.class)))
				.thenReturn(null);
		thrown.expect(HygieiaException.class);
		thrown.expectMessage("dashboard component not exists");
		collectorService.getCmdbByStaticAnalysis("colName","test");
	}

	@Test
	public void test_getCmdbOfSonarProject_noDashboard() throws HygieiaException {
		when(collectorRepository.findByName(Matchers.anyString())).thenReturn(new Collector("colName", CollectorType.CodeQuality));
		when(collectorItemRepository
				.findAllByOptionNameValueAndCollectorIdsIn(Matchers.anyString(), Matchers.anyString(), Matchers.anyList()))
				.thenReturn(Lists.newArrayList(makeCollectorItem(false)));
		when(componentRepository.findByCollectorTypeAndItemIdIn(Matchers.any(CollectorType.class), Matchers.anyListOf(ObjectId.class)))
				.thenReturn(Lists.newArrayList(new Component()));
		when(dashboardRepository.findByApplicationComponentIdsIn(Matchers.anyListOf(ObjectId.class)))
				.thenReturn(null);
		thrown.expect(HygieiaException.class);
		thrown.expectMessage("dashboard not exists");
		collectorService.getCmdbByStaticAnalysis("colName","test");
	}

	@Test
	public void test_getCmdbOfSonarProject_noCmdb() throws HygieiaException {
		when(collectorRepository.findByName(Matchers.anyString())).thenReturn(new Collector("colName", CollectorType.CodeQuality));
		when(collectorItemRepository
				.findAllByOptionNameValueAndCollectorIdsIn(Matchers.anyString(), Matchers.anyString(), Matchers.anyList()))
				.thenReturn(Lists.newArrayList(makeCollectorItem(false)));
		when(componentRepository.findByCollectorTypeAndItemIdIn(Matchers.any(CollectorType.class), Matchers.anyListOf(ObjectId.class)))
				.thenReturn(Lists.newArrayList(new Component()));
		Dashboard dashboard = new Dashboard(false,null,"testDbd",null,null,null,null,
				null,null,false,null);
		when(dashboardRepository.findByApplicationComponentIdsIn(Matchers.anyListOf(ObjectId.class)))
				.thenReturn(Lists.newArrayList(dashboard));
		when(cmdbRepository.findByConfigurationItemAndItemTypeAndValidConfigItem(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean()))
				.thenReturn(null);
		thrown.expect(HygieiaException.class);
		thrown.expectMessage("valid cmdb not exists");
		collectorService.getCmdbByStaticAnalysis("colName","test");
	}
}
