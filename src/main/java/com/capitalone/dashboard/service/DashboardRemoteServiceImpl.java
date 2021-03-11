package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Application;
import com.capitalone.dashboard.model.Cmdb;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.DashboardType;
import com.capitalone.dashboard.model.Owner;
import com.capitalone.dashboard.model.ScoreDisplayType;
import com.capitalone.dashboard.model.Widget;
import com.capitalone.dashboard.repository.CmdbRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CustomRepositoryQuery;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.request.DashboardRemoteRequest;
import com.capitalone.dashboard.request.WidgetRequest;
import com.capitalone.dashboard.settings.ApiSettings;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardRemoteServiceImpl implements DashboardRemoteService {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardRemoteServiceImpl.class);
    private final CollectorRepository collectorRepository;
    private final CustomRepositoryQuery customRepositoryQuery;
    private final DashboardRepository dashboardRepository;
    private final DashboardService dashboardService;
    private final CollectorService collectorService;
    private final UserInfoService userInfoService;
    private final CmdbRepository cmdbRepository;
    private final ComponentRepository componentRepository;
    private final CollectorItemRepository collectorItemRepository;
    private final ApiSettings apiSettings;
    private final EncryptionService encryptionService;

    public static final String PASSWORD_OPTION = "password";
    public static final String PERSONAL_ACCESS_TOKEN_OPTION = "personalAccessToken";


    @Autowired
    public DashboardRemoteServiceImpl(
            CollectorRepository collectorRepository,
            CustomRepositoryQuery customRepositoryQuery,
            DashboardRepository dashboardRepository, DashboardService dashboardService, CollectorService collectorService,
            UserInfoService userInfoService, CmdbRepository cmdbRepository, ComponentRepository componentRepository,
            CollectorItemRepository collectorItemRepository, ApiSettings apiSettings, EncryptionService encryptionService) {
        this.collectorRepository = collectorRepository;
        this.customRepositoryQuery = customRepositoryQuery;
        this.dashboardRepository = dashboardRepository;
        this.dashboardService = dashboardService;
        this.collectorService = collectorService;
        this.userInfoService = userInfoService;
        this.cmdbRepository = cmdbRepository;
        this.componentRepository = componentRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.apiSettings = apiSettings;
        this.encryptionService = encryptionService;
    }

    /**
     * Creates a list of unique owners from the owner and owners requests
     * @param request
     * @return List<Owner> list of owners to be added to the dashboard
     * @throws HygieiaException
     */
    private List<Owner> getOwners(DashboardRemoteRequest request) throws HygieiaException {
        DashboardRemoteRequest.DashboardMetaData metaData = request.getMetaData();
        Owner owner = metaData.getOwner();
        List<Owner> owners = metaData.getOwners();

        if (owner == null && CollectionUtils.isEmpty(owners)) {
            throw new HygieiaException("There are no owner/owners field in the request", HygieiaException.INVALID_CONFIGURATION);
        }

        if (owners == null) {
            owners = new ArrayList<Owner>();
            owners.add(owner);
        } else if (owner != null) {
            owners.add(owner);
        }

        Set<Owner> uniqueOwners = new HashSet<Owner>(owners);
        return new ArrayList<Owner>(uniqueOwners);
    }

    @Override
    public Dashboard remoteCreate(DashboardRemoteRequest request, boolean isUpdate) throws HygieiaException {
        final String METHOD_NAME = "DashboardRemoteServiceImpl.remoteCreate";
        Dashboard dashboard;
        Map<String, Widget> existingWidgets = new HashMap<>();

        List<Owner> owners = getOwners(request);
        List<Owner> validOwners = Lists.newArrayList();
        for (Owner owner : owners) {
            if (userInfoService.isUserValid(owner.getUsername(), owner.getAuthType())) {
                validOwners.add(owner);
            } else {
                LOG.warn(" correlation_id=" + request.getClientReference() + " Invalid owner passed in the request dashboard_invalid_owner=" + owner.getUsername());
            }
        }

        if (validOwners.isEmpty()) {
            throw new HygieiaException("There are no valid owner/owners in the request", HygieiaException.INVALID_CONFIGURATION);
        }

        // if true password or personalAccessToken in SCM widgets needs to be encrypted
        if(apiSettings.isEncryptRemoteCreatePayload()) {
            encryptSCMWidgets(request);
        }

        List<Dashboard> dashboards = findExistingDashboardsFromRequest( request );
        if (!CollectionUtils.isEmpty(dashboards)) {
            if (dashboards.size()==1) {
                dashboard = dashboards.get(0);
            } else {
                dashboard = chooseDashboard(dashboards, request);
            }
            Set<Owner> uniqueOwners = new HashSet<Owner>(validOwners);
            uniqueOwners.addAll(dashboard.getOwners());
            dashboard.setOwners(new ArrayList<Owner>(uniqueOwners));
            dashboard.setConfigurationItemBusAppName(request.getMetaData().getBusinessApplication());
            dashboard.setConfigurationItemBusServName(request.getMetaData().getBusinessService());
            dashboard.setClientReference(request.getClientReference());
//            if (!isUpdate) {
//                throw new HygieiaException("Dashboard " + dashboard.getTitle() + " (id =" + dashboard.getId() + ") already exists", HygieiaException.DUPLICATE_DATA);
//            }
            dashboardService.update(dashboard);
            //Save the widgets
            for (Widget w : dashboard.getWidgets()) {
                existingWidgets.put(w.getName(), w);
            }

        } else {
            if (isUpdate) {
                throw new HygieiaException("Dashboard " + request.getMetaData().getTitle() +  " does not exist.", HygieiaException.BAD_DATA);
            }
            request.getMetaData().setOwners(validOwners);
            dashboard = dashboardService.create(requestToDashboard(request));
        }

        Set<CollectorType> incomingTypes = new HashSet<>();
        List<DashboardRemoteRequest.Entry> entries = request.getAllEntries();
        Map<String, WidgetRequest> allWidgetRequests = generateRequestWidgetList( entries, dashboard, incomingTypes);
        Component component = componentRepository.findOne(dashboard.getApplication().getComponents().get(0).getId());
        Set<CollectorType> existingTypes = new HashSet<>(component.getCollectorItems().keySet());

        //adds widgets
        for (String key : allWidgetRequests.keySet()) {
            WidgetRequest widgetRequest = allWidgetRequests.get(key);

            component = dashboardService.associateCollectorToComponent(dashboard.getApplication().getComponents().get(0).getId(), widgetRequest.getCollectorItemIds(),component,true);
            Widget newWidget = widgetRequest.widget();
            if (isUpdate) {
                Widget oldWidget = existingWidgets.get(newWidget.getName());
                if (oldWidget == null) {
                    dashboardService.addWidget(dashboard, newWidget);
                } else {
                    Widget widget = widgetRequest.updateWidget(dashboardService.getWidget(dashboard, oldWidget.getId()));
                    dashboardService.updateWidget(dashboard, widget);
                }
            } else {
                dashboardService.addWidget(dashboard, newWidget);
            }
        }

        // Delete collector item types that are not in the incoming types
        Set<CollectorType> deleteSet = new HashSet<>();
        for (CollectorType existingType : existingTypes) {
            if (existingType==CollectorType.Audit) continue;    // Audit is used by NFRR, not present in incoming types
            if (existingType==CollectorType.Artifact) continue; // right now we cannot fully trust BladeRunner on this,
                                                                // as they do not have the parsing logic implemented
            if (!incomingTypes.contains(existingType)) {
                deleteSet.add(existingType);
                component.getCollectorItems().remove(existingType);
            }
        }

        // Delete widgets that do not have collector items, except the quality widget (which may have more than one type)
        for (CollectorType type: deleteSet) {
            if (!DashboardServiceImpl.QualityWidget.contains(type)) {
                dashboardService.deleteWidget(dashboard,type);
            }
        }
        // delete code analysis widget if no collector item types is incoming
        if (incomingTypes.stream().noneMatch(DashboardServiceImpl.QualityWidget::contains)) {
            dashboardService.deleteWidget(dashboard,CollectorType.CodeQuality);
        }

        LOG.info("correlation_id="+ request.getClientReference() + ", dashboard_title=" + dashboard.getTitle() + ", existing_widget_types=" + existingTypes.size() +
                " " + existingTypes + ", incoming_widget_types=" + incomingTypes.size() + " " + incomingTypes
                + ", deleted_widgets_set=" + deleteSet.size() + " " + deleteSet);

        componentRepository.save(component);
        return (dashboard != null) ? dashboardService.get(dashboard.getId()) : null;
    }

    private Dashboard chooseDashboard(List<Dashboard> dashboards, DashboardRemoteRequest request) {
        Dashboard dashboard = null;
        String businessService = request.getMetaData().getBusinessService();
        String businessApplication = request.getMetaData().getBusinessApplication();
        String title = request.getMetaData().getTitle();
        for (Dashboard one : dashboards) {
            if (dashboard==null) {
                dashboard = one;
            } else if (one.getUpdatedAt()>dashboard.getUpdatedAt()) {
                dashboard = one;
            } else if (one.getUpdatedAt()==dashboard.getUpdatedAt()) {
                if (one.getCreatedAt()>dashboard.getCreatedAt()) {
                    dashboard = one;
                }
            }
        }
        LOG.warn(String.format("correlation_id=%s, count_dashboards=%d, ba=%s, component=%s, dashboard_title=%s, selected_dashboard_id=%s",
                request.getClientReference(), dashboards.size(), businessService, businessApplication, title, dashboard.getId()));
        return dashboard;
    }

    /**
     * Generates a Widget Request list of Widgets to be created from the request
     * @param entries
     * @param dashboard
     * @return Map< String, WidgetRequest > list of Widgets to be created
     * @throws HygieiaException
     */
    private  Map < String, WidgetRequest > generateRequestWidgetList( List < DashboardRemoteRequest.Entry > entries, Dashboard dashboard, Set<CollectorType> incomingTypes) throws HygieiaException {
        Map< String, WidgetRequest > allWidgetRequests = new HashMap<>();
        List< Collector > collectors = new ArrayList<>();
        Collector collector = null;
        //builds widgets
        for ( DashboardRemoteRequest.Entry entry : entries ) {
            // get collector from database
            if( collector == null || collector.getCollectorType() != entry.getType() ||
                    !StringUtils.equalsIgnoreCase(collector.getName(), entry.getToolName()) ) {
                collectors = collectorRepository.findByCollectorTypeAndName( entry.getType(), entry.getToolName() );
                if ( CollectionUtils.isEmpty( collectors ) ) {
                    throw new HygieiaException( entry.getToolName() + " collector is not available.", HygieiaException.BAD_DATA );
                }
                collector = collectors.get( 0 );
                incomingTypes.add(collector.getCollectorType());
            }

            WidgetRequest widgetRequest = allWidgetRequests.get( entry.getWidgetName() );
            if ( widgetRequest == null ) {
                widgetRequest = entryToWidgetRequest( dashboard, entry, collector) ;
                allWidgetRequests.put( entry.getWidgetName(), widgetRequest );
            } else {
                CollectorItem item = entryToCollectorItem( entry, collector );
                if ( item != null ) {
                    widgetRequest.getCollectorItemIds().add( item.getId() );
                }
            }
        }
        return allWidgetRequests;
    }
    /**
     * Takes a DashboardRemoteRequest. If the request contains a Business Service and Business Application then returns dashboard. Otherwise,
     * Checks dashboards for existing Title and returns dashboards.
     * @param request
     * @return  List< Dashboard >
     */
    private List< Dashboard > findExistingDashboardsFromRequest( DashboardRemoteRequest request ) {
        String businessService = request.getMetaData().getBusinessService();
        String businessApplication = request.getMetaData().getBusinessApplication();
        String title = request.getMetaData().getTitle();
        List<Dashboard> existing = new ArrayList<>();
        if( !StringUtils.isEmpty( businessService ) && !StringUtils.isEmpty( businessApplication ) ){
           existing.addAll(dashboardRepository.findAllByConfigurationItemBusServNameContainingIgnoreCaseAndConfigurationItemBusAppNameContainingIgnoreCase( businessService, businessApplication ));
        } if (CollectionUtils.isEmpty(existing) && StringUtils.isNotEmpty(title)) {
           existing.addAll(dashboardRepository.findByTitle( request.getMetaData().getTitle() ));
        }
        return existing;
    }


    private CollectorItem entryToCollectorItem(DashboardRemoteRequest.Entry entry, Collector collector) throws HygieiaException {
        CollectorItem item = entry.toCollectorItem(collector);
        item.setCollectorId(collector.getId());
        return collectorService.createCollectorItemSelectOptions(item, collector, collector.getAllFields(), item.getOptions());
    }

    /**
     * Creates a widget from entry
     * @param dashboard
     * @param entry
     * @return WidgetRequest
     */
    private WidgetRequest entryToWidgetRequest(Dashboard dashboard, DashboardRemoteRequest.Entry entry, Collector collector) throws HygieiaException {
        WidgetRequest request = new WidgetRequest();
        CollectorItem item = entryToCollectorItem(entry, collector);
        if (item != null) {
            request.setName(entry.getWidgetName());
            request.setComponentId(dashboard.getApplication().getComponents().get(0).getId());
            request.setOptions(entry.toWidgetOptions());
            List<ObjectId> ids = new ArrayList<>();
            ids.add(item.getId());
            request.setCollectorItemIds(ids);
        }
        return request;
    }



    /**
     * Creates a Dashboard object from the request.
     * @param request
     * @return Dashboard
     * @throws HygieiaException
     */
    private Dashboard requestToDashboard(DashboardRemoteRequest request) throws HygieiaException {
        DashboardRemoteRequest.DashboardMetaData metaData = request.getMetaData();
        Application application = new Application(metaData.getApplicationName(), new Component(metaData.getComponentName()));
        String appName = null;
        String serviceName = null;
        if (!StringUtils.isEmpty(metaData.getBusinessApplication())) {
            Cmdb app = cmdbRepository.findByConfigurationItemAndItemType(metaData.getBusinessApplication(), "component");
            if (app == null) throw new HygieiaException("Invalid Business Application Name.", HygieiaException.BAD_DATA);
            appName = app.getConfigurationItem();
        }
        if (!StringUtils.isEmpty(metaData.getBusinessService())) {
            Cmdb service = cmdbRepository.findByConfigurationItemAndItemType(metaData.getBusinessService(), "app");
            if (service == null) throw new HygieiaException("Invalid Business Service Name.", HygieiaException.BAD_DATA);
            serviceName = service.getConfigurationItem();
        }
        List<String> activeWidgets = new ArrayList<>();
        return new Dashboard(true, metaData.getTemplate(), metaData.getTitle(), application, metaData.getOwners(), DashboardType.fromString(metaData.getType()), serviceName, appName,activeWidgets, false, ScoreDisplayType.HEADER);
    }

    private void encryptSCMWidgets (DashboardRemoteRequest request) {
        if(request == null) return;
        if(CollectionUtils.isEmpty(request.getCodeRepoEntries())) return;
        List<DashboardRemoteRequest.CodeRepoEntry> scmEntries = request.getCodeRepoEntries();

        for (DashboardRemoteRequest.CodeRepoEntry scmEntry : scmEntries) {

            String password = (String) scmEntry.getOptions().get(PASSWORD_OPTION);
            String accessToken = (String) scmEntry.getOptions().get(PERSONAL_ACCESS_TOKEN_OPTION);
            if(StringUtils.isNotEmpty(password)) {
                //encrypt the password
                scmEntry.getOptions().put(PASSWORD_OPTION, encryptionService.encrypt(password));
            } else if (StringUtils.isNotEmpty(accessToken)) {
                scmEntry.getOptions().put(PERSONAL_ACCESS_TOKEN_OPTION, encryptionService.encrypt(accessToken));
            }
        }
    }
}
