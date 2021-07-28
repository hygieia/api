package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.config.TestConfig;
import com.capitalone.dashboard.config.WebMVCConfig;
import com.capitalone.dashboard.model.AuthType;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.DashboardType;
import com.capitalone.dashboard.model.Owner;
import com.capitalone.dashboard.model.ScoreDisplayType;
import com.capitalone.dashboard.model.Widget;
import com.capitalone.dashboard.request.DashboardRequest;
import com.capitalone.dashboard.request.DashboardRequestTitle;
import com.capitalone.dashboard.request.WidgetRequest;
import com.capitalone.dashboard.service.DashboardService;
import com.capitalone.dashboard.util.TestUtil;
import com.capitalone.dashboard.util.WidgetOptionsBuilder;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.capitalone.dashboard.fixture.DashboardFixture.makeComponent;
import static com.capitalone.dashboard.fixture.DashboardFixture.makeDashboard;
import static com.capitalone.dashboard.fixture.DashboardFixture.makeDashboardRequest;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class, WebMVCConfig.class})
@WebAppConfiguration
public class DashboardControllerTest {
    private MockMvc mockMvc;
    private String configItemAppName = "ASVTEST";
    private String configItemComponentName = "BAPTEST";
    @Autowired private WebApplicationContext wac;
    @Autowired private DashboardService dashboardService;
    @Before
    public void before() {
    	SecurityContextHolder.clearContext();
    	mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }
    @Test
    public void dashboards() throws Exception {
        Dashboard d1 = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        when(dashboardService.all()).thenReturn(Collections.singletonList(d1));
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].template", is("t1")))
                .andExpect(jsonPath("$[0].title", is("title")))
                .andExpect(jsonPath("$[0].application.name", is("app")))
                .andExpect(jsonPath("$[0].application.components[0].name", is("comp")))
                .andExpect(jsonPath("$[0].configurationItemBusServName", is(configItemAppName)))
                .andExpect(jsonPath("$[0].configurationItemBusAppName", is(configItemComponentName)));
    }
    @Test
    public void createProductDashboard() throws Exception {
        DashboardRequest request = makeDashboardRequest("template", "dashboard title", null, null,"amit", null, "product", configItemAppName, configItemComponentName);
        initiateSecurityContext("amit", AuthType.STANDARD);
        mockMvc.perform(post("/dashboard")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isCreated());
    }
    @Test
    public void createTeamDashboard() throws Exception {
        DashboardRequest request = makeDashboardRequest("template", "dashboard title", "app", "comp","amit", null, "team", configItemAppName, configItemComponentName);
        initiateSecurityContext("amit", AuthType.STANDARD);
        mockMvc.perform(post("/dashboard")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
            .andExpect(status().isCreated());
    }

    @Test
    public void createDashboard_nullRequest() throws Exception {
        mockMvc.perform(post("/dashboard")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(new DashboardRequest())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.template", hasItems("must not be null")))
//            TODO:  These are no longer necessary in all cases.  Potentially add new class-level validator.
//            .andExpect(jsonPath("$.fieldErrors.componentName", hasItems("may not be null")))
//            .andExpect(jsonPath("$.fieldErrors.applicationName", hasItems("may not be null")))
            .andExpect(jsonPath("$.fieldErrors.type", hasItems("must not be null")))
            .andExpect(jsonPath("$.fieldErrors.dashboardRequestTitle", hasItems("must not be null")));
    }
    
    @Test
    public void createDashboard_emptyValues() throws Exception {
    	DashboardRequest dashboardRequest = new DashboardRequest();
    	dashboardRequest.setTitle(StringUtils.EMPTY);
    	dashboardRequest.setTemplate(StringUtils.EMPTY);
    	dashboardRequest.setType(StringUtils.EMPTY);
        MvcResult result = mockMvc.perform(post("/dashboard")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dashboardRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.template", hasItems("Please select a template")))
            .andExpect(jsonPath("$.fieldErrors.type", hasItems("Please select a type")))
            .andReturn();
        assertThat(getFieldErrors(result), hasEntry(is("dashboardRequestTitle.title"), contains(is("size must be between 6 and 50"))));
    }
    @Test
    public void createDashboard_specialCharacters_badRequest() throws Exception {
        DashboardRequest request = makeDashboardRequest("template", "bad/title", "app", "comp","amit", null, "team", configItemAppName, configItemComponentName);
        MvcResult result = mockMvc.perform(post("/dashboard")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
            .andExpect(status().isBadRequest())
            .andReturn();
        assertThat(getFieldErrors(result), hasEntry(is("dashboardRequestTitle.title"), contains(is("Special character(s) found"))));
    }
    
    @Test
    public void getDashboard() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard d1 = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        d1.setId(objectId);
        when(dashboardService.get(objectId)).thenReturn(d1);
        mockMvc.perform(get("/dashboard/" + objectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(objectId.toString())));
    }
    @Test
    public void updateTeamDashboard() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        DashboardRequest request = makeDashboardRequest("template", "dashboard title", "app", "comp","amit", null, "team", configItemAppName, configItemComponentName);

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.update(Matchers.any(Dashboard.class))).thenReturn(orig);
        initiateSecurityContext("amit", AuthType.STANDARD);
        
        mockMvc.perform(put("/dashboard/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void renameTeamDashboard() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "dashboard title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        orig.setId(objectId);
        DashboardRequestTitle request = makeDashboardRequestTitle("different title");

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.all()).thenReturn(Collections.singletonList(orig));

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
	
                .andExpect(status().isOk());
    }

    @Test
    public void renameTeamDashboard_sameTitle() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "dashboard title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        orig.setId(objectId);
        DashboardRequestTitle request = makeDashboardRequestTitle("dashboard title");

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.all()).thenReturn(Collections.singletonList(orig));

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))

                .andExpect(status().isOk());
    }

    @Test
    public void renameTeamDashboard_titleExists() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "t1 title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        orig.setId(objectId);

        ObjectId objectIdAnother = new ObjectId("54b981222364c80a6136c9f2");
        Dashboard another = makeDashboard("t2", "new title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        another.setId(objectIdAnother);

        List<Dashboard> allDashboards = new ArrayList<>();
        allDashboards.add(orig);
        allDashboards.add(another);

        DashboardRequestTitle request = makeDashboardRequestTitle("new title");

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.getByTitle("new title")).thenReturn(allDashboards);

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isBadRequest());
        verify(dashboardService, never()).update(orig);
    }
    
    @Test
    public void renameTeamDashboard_titleDoesntExist() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "t1 title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        orig.setId(objectId);

        ObjectId objectIdAnother = new ObjectId("54b981222364c80a6136c9f2");
        Dashboard another = makeDashboard("t2", "t2 title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        another.setId(objectIdAnother);

        List<Dashboard> allDashboards = new ArrayList<>();
        allDashboards.add(orig);
        allDashboards.add(another);

        DashboardRequestTitle request = makeDashboardRequestTitle("title exists");

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.all()).thenReturn(allDashboards);

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isOk());
        verify(dashboardService).update(orig);
    }
    
    @Test
    public void renameTeamDashboard_invalidTitle() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "dashboard title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        DashboardRequestTitle request = makeDashboardRequestTitle("bad / title");

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.all()).thenReturn(Collections.singletonList(orig));

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title", hasItems("Special character(s) found")))
                ;
    }
    
    @Test
    public void renameTeamDashboard_emptyTitle() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "dashboard title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        DashboardRequestTitle request = makeDashboardRequestTitle("");

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.all()).thenReturn(Collections.singletonList(orig));

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title", hasItems("size must be between 6 and 50")))
                ;
    }
    
    @Test
    public void renameTeamDashboard_nullTitle() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard orig = makeDashboard("t1", "dashboard title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        DashboardRequestTitle request = new DashboardRequestTitle();

        when(dashboardService.get(objectId)).thenReturn(orig);
        when(dashboardService.all()).thenReturn(Collections.singletonList(orig));

        mockMvc.perform(put("/dashboard/rename/" + objectId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title", hasItems("must not be null")))
                ;
    }
    
    
    @Test
    public void deleteDashboard() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        mockMvc.perform(delete("/dashboard/" + objectId.toString())).andExpect(status().isNoContent());
    }
    @Test
    public void addWidget() throws Exception {
        ObjectId dashId = ObjectId.get();
        ObjectId compId = ObjectId.get();
        ObjectId collId = ObjectId.get();
        List<ObjectId> collIds = Collections.singletonList(collId);
        Map<String, Object> options = new WidgetOptionsBuilder().put("option1", 1).put("option2", "2").get();
        WidgetRequest request = makeWidgetRequest("build", compId, collIds, options);
        Dashboard d1 = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        Widget widgetWithId = request.widget();
        widgetWithId.setId(ObjectId.get());
        Component component = makeComponent(compId, "Component", CollectorType.Build, collId);
        when(dashboardService.get(dashId)).thenReturn(d1);
        when(dashboardService.associateCollectorToComponent(compId, collIds, true)).thenReturn(component);
        when(dashboardService.addWidget(Matchers.any(Dashboard.class), Matchers.any(Widget.class))).thenReturn(widgetWithId);
        mockMvc.perform(post("/dashboard/" + dashId.toString() + "/widget")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.widget.id", is(widgetWithId.getId().toString())))
                .andExpect(jsonPath("$.widget.name", is("build")))
                .andExpect(jsonPath("$.widget.componentId", is(compId.toString())))
                .andExpect(jsonPath("$.component.id", is(component.getId().toString())))
                .andExpect(jsonPath("$.component.name", is(component.getName())))
                .andExpect(jsonPath("$.component.collectorItems.Build[0].id", is(collId.toString())))
        ;
    }
    @Test
    public void updateWidget() throws Exception {
        ObjectId dashId = ObjectId.get();
        ObjectId widgetId = ObjectId.get();
        ObjectId compId = ObjectId.get();
        ObjectId collId = ObjectId.get();
        List<ObjectId> collIds = Collections.singletonList(collId);
        Map<String, Object> options = new WidgetOptionsBuilder().put("option1", 2).put("option2", "3").get();
        WidgetRequest request = makeWidgetRequest("build", compId, collIds, options);
        Dashboard d1 = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        Widget widget = makeWidget(widgetId, "build", compId, options);
        when(dashboardService.get(dashId)).thenReturn(d1);
        when(dashboardService.getWidget(d1, widgetId)).thenReturn(widget);
        when(dashboardService.updateWidget(Matchers.any(Dashboard.class), Matchers.any(Widget.class))).thenReturn(widget);
        mockMvc.perform(put("/dashboard/" + dashId.toString() + "/widget/" + widgetId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteWidget() throws Exception {
        ObjectId dashId = ObjectId.get();
        ObjectId widgetId = ObjectId.get();
        ObjectId compId = ObjectId.get();
        ObjectId collId = ObjectId.get();
        List<ObjectId> collIds = Collections.singletonList(collId);
        Map<String, Object> options = new WidgetOptionsBuilder().put("option1", 2).put("option2", "3").get();
        WidgetRequest request = makeWidgetRequest("build", compId, collIds, options);
        Dashboard d1 = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        Widget widget = makeWidget(widgetId, "build", compId, options);
        when(dashboardService.get(dashId)).thenReturn(d1);
        when(dashboardService.getWidget(d1, widgetId)).thenReturn(widget);
        dashboardService.deleteWidget(Matchers.any(Dashboard.class), Matchers.any(Widget.class), Matchers.any(ObjectId.class), Matchers.anyListOf(ObjectId.class), Matchers.eq(true));
        mockMvc.perform(put("/dashboard/" + dashId.toString() + "/deleteWidget/" + widgetId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteWidgetQuality() throws Exception {
        ObjectId dashId = ObjectId.get();
        ObjectId widgetId = ObjectId.get();
        ObjectId compId = ObjectId.get();
        ObjectId collId = ObjectId.get();
        List<ObjectId> collIds = Collections.singletonList(collId);
        Map<String, Object> options = new WidgetOptionsBuilder().put("option1", 2).get();
        WidgetRequest request = makeWidgetRequest("codeanalysis", compId, collIds, options);
        Dashboard d1 = makeDashboard("t1", "title", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        Widget widget = makeWidget(widgetId, "codeanalysis", compId, options);
        when(dashboardService.get(dashId)).thenReturn(d1);
        when(dashboardService.getWidget(d1, widgetId)).thenReturn(widget);
        dashboardService.deleteWidget(Matchers.any(Dashboard.class), Matchers.any(Widget.class), Matchers.any(ObjectId.class), Matchers.anyListOf(ObjectId.class), Matchers.eq(false));
        mockMvc.perform(put("/dashboard/" + dashId.toString() + "/deleteWidget/" + widgetId.toString())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(request)))
                .andExpect(status().isOk());
    }
    
    @Test
    public void updateOwners() throws Exception {
    	ObjectId dashId = ObjectId.get();
    	Iterable<Owner> owners = Lists.newArrayList(new Owner("one", AuthType.STANDARD), new Owner("two", AuthType.LDAP));
    	when(dashboardService.updateOwners(dashId, owners)).thenReturn(owners);
    	mockMvc.perform(put("/dashboard/" + dashId.toString() + "/owners")
    			.contentType(MediaType.APPLICATION_JSON_UTF8)
    			.content(TestUtil.convertObjectToJsonBytes(owners)))
    			.andExpect(status().is2xxSuccessful())
    			.andExpect(jsonPath("$", hasSize(2)))
    			.andExpect(jsonPath("$[0].username", is("one")))
    			.andExpect(jsonPath("$[0].authType", is(AuthType.STANDARD.name())))
    			.andExpect(jsonPath("$[1].username", is("two")))
    			.andExpect(jsonPath("$[1].authType", is(AuthType.LDAP.name())));
    	verify(dashboardService).updateOwners(dashId, owners);
    		
    	
    }

    @Test
    public void updateScoreSettings() throws Exception {
        ObjectId objectId = new ObjectId("54b982620364c80a6136c9f2");
        Dashboard dashboard = makeDashboard("t1", "dashboard", "app", "comp","amit", DashboardType.Team, configItemAppName, configItemComponentName);
        dashboard.setId(objectId);

        when(dashboardService.updateScoreSettings(objectId, true, ScoreDisplayType.HEADER)).thenReturn(dashboard);

        mockMvc.perform(put("/dashboard/updateScoreSettings/" + objectId.toString() + "?scoreEnabled=true&scoreDisplay=HEADER"))
          .andExpect(status().isOk());
    }

    @Test
    public void cleanDashboardWidgets() throws Exception {
        mockMvc.perform(get("/dashboard/widgets/cleanup?isSave=false"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard/widgets/cleanup?isSave=true"))
                .andExpect(status().isOk());
    }

    
    private DashboardRequestTitle makeDashboardRequestTitle(String title) {
        DashboardRequestTitle request = new DashboardRequestTitle();
        request.setTitle(title);
        return request;
    }

    private Widget makeWidget(ObjectId widgetId, String name, ObjectId compId, Map<String, Object> options) {
        Widget widget = new Widget();
        widget.setId(widgetId);
        widget.setName(name);
        widget.setComponentId(compId);
        widget.getOptions().putAll(options);
        return widget;
    }
    private WidgetRequest makeWidgetRequest(String name, ObjectId componentId,
                                            List<ObjectId> collIds, Map<String, Object> options) {
        WidgetRequest request = new WidgetRequest();
        request.setName(name);
        request.setComponentId(componentId);
        request.setCollectorItemIds(collIds);
        request.setOptions(options);
        return request;
    }
    
    private Map<String, JSONArray> getFieldErrors(MvcResult result) throws UnsupportedEncodingException {
        String content = result.getResponse().getContentAsString();
        return JsonPath.read(content, "$.fieldErrors");
}
    
    private void initiateSecurityContext(String username, AuthType standard) {
    	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, "password");
    	authentication.setDetails(AuthType.STANDARD.name());
    	SecurityContextHolder.getContext().setAuthentication(authentication);
	}
    
}
