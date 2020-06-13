package com.capitalone.dashboard.service;


import com.capitalone.dashboard.model.BinaryArtifact;
import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.GitRequest;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.model.SonarProject;
import com.capitalone.dashboard.model.webhook.github.GitHubRepo;
import com.capitalone.dashboard.repository.BinaryArtifactRepository;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.GitRequestRepository;
import com.capitalone.dashboard.repository.LibraryPolicyResultsRepository;
import com.capitalone.dashboard.repository.SonarProjectRepository;
import com.capitalone.dashboard.repository.TestResultRepository;
import com.capitalone.dashboard.request.DataSyncRequest;
import com.capitalone.dashboard.request.DataSyncResponse;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.webhook.datasync.DataSyncServiceImpl;
import com.capitalone.dashboard.webhook.settings.DataSyncSettings;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DataSyncServiceTest {

    private static final String ARTIFACTORY = "Artifactory";
    private static final String GITHUB = "Github";
    private static final String SONAR = "Sonar";
    private static final String LIBRARY_POLICY = "LibraryPolicy";
    private static final String STATIC_SECURITY = "StaticSecurity";
    private ApiSettings settings;
    private DataSyncSettings dataSyncSettings;
    private DataSyncServiceImpl dataSyncService;
    @Mock
    private CodeQualityRepository codeQualityRepository;
    @Mock
    private SonarProjectRepository sonarProjectRepository;
    @Mock
    private CollectorRepository collectorRepository;
    @Mock
    private ComponentRepository componentRepository;
    @Mock
    private CollectorItemRepository collectorItemRepository;
    @Mock
    private BinaryArtifactRepository binaryArtifactRepository;
    @Mock
    private GitRequestRepository gitRequestRepository;
    @Mock
    private LibraryPolicyResultsRepository libraryPolicyResultsRepository;
    @Mock
    private TestResultRepository testResultRepository;


    @Before
    public void init() {
        settings = new ApiSettings();
        dataSyncSettings = new DataSyncSettings();
        dataSyncSettings.setArtifact(ARTIFACTORY);
        dataSyncSettings.setScm(GITHUB);
        dataSyncSettings.setCodeQuality(SONAR);
        dataSyncSettings.setLibraryPolicy(LIBRARY_POLICY);
        dataSyncSettings.setStaticSecurity(STATIC_SECURITY);
        settings.setDataSyncSettings(dataSyncSettings);
        dataSyncService = new DataSyncServiceImpl(codeQualityRepository, sonarProjectRepository, collectorRepository, componentRepository, collectorItemRepository, binaryArtifactRepository,
                gitRequestRepository, libraryPolicyResultsRepository,testResultRepository, settings);

    }

    @Test
    public void dataCleanGIT() {
        when(dataSyncService.getCollectorRepository().findByName(GITHUB)).thenReturn(getCollector(GITHUB, CollectorType.SCM, getUniqueOptions("url", "", "branch", "")));
        ObjectId collectorItemId = ObjectId.get();
        List<CollectorItem> cis = Arrays.asList(getGitRepo(collectorItemId, "http://github.com/repo1", "master"));
        Page<CollectorItem> pagedCollectorItems = new PageImpl<>(cis);
        ObjectId collectorItemId2 = ObjectId.get();
        List<CollectorItem> suspects = Arrays.asList(getGitRepo(collectorItemId, "http://github.com/repo1", "master"), getGitRepo(collectorItemId2, "http://github.com/repo1", "master"));
        when(dataSyncService.getCollectorItemRepository().findByCollectorIdIn(any(), any())).thenReturn(pagedCollectorItems);
        when(dataSyncService.getCollectorItemRepository().findAllByOptionMapAndCollectorIdsIn(any(), any())).thenReturn(suspects);
        when(dataSyncService.getGitRequestRepository().findTopByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getGitRequest(collectorItemId, 1584127005000L));
        when(dataSyncService.getGitRequestRepository().findTopByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getGitRequest(collectorItemId2, 1581621405000L));
        when(dataSyncService.getComponentRepository().findBySCMCollectorItemId(any())).thenReturn(Arrays.asList(getComponent(collectorItemId)));
        when(dataSyncService.getCollectorItemRepository().findOne(any(ObjectId.class))).thenReturn(getGitRepo(collectorItemId, "http://github.com/repo1", "master"));
        DataSyncResponse response = dataSyncService.refresh(getDataSyncRequest(GITHUB));
        assertEquals(response.getCollectorItemCount(), 1);
        assertEquals(response.getComponentCount(), 2);
    }

    @Test
    public void dataCleanSonar() {
        when(dataSyncService.getCollectorRepository().findByName(SONAR)).thenReturn(getCollector(SONAR, CollectorType.CodeQuality, getUniqueOptions("projectName", "", "instanceUrl", "")));
        ObjectId collectorItemId = ObjectId.get();
        List<CollectorItem> cis = Arrays.asList(getSonarProject(collectorItemId, "sonarProjectName", "http://sonarqube.com"));
        Page<CollectorItem> pagedCollectorItems = new PageImpl<>(cis);
        ObjectId collectorItemId2 = ObjectId.get();
        List<CollectorItem> suspects = Arrays.asList(getSonarProject(collectorItemId, "sonarProjectName", "http://sonarqube.com"), getSonarProject(collectorItemId2, "sonarProjectName2", "http://sonarqube.com"));
        when(dataSyncService.getCollectorItemRepository().findByCollectorIdIn(any(), any())).thenReturn(pagedCollectorItems);
        when(dataSyncService.getCollectorItemRepository().findAllByOptionMapAndCollectorIdsIn(any(), any())).thenReturn(suspects);
        when(dataSyncService.getCodeQualityRepository().findTop1ByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getCodeQuality(collectorItemId, 1584127005000L));
        when(dataSyncService.getCodeQualityRepository().findTop1ByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getCodeQuality(collectorItemId2, 1581621405000L));
        when(dataSyncService.getComponentRepository().findByCodeQualityCollectorItems(any())).thenReturn(Arrays.asList(getComponent(collectorItemId)));
        when(dataSyncService.getCollectorItemRepository().findOne(any(ObjectId.class))).thenReturn(getSonarProject(collectorItemId, "sonarProjectName1", "http://sonarqube.com"));
        DataSyncResponse response = dataSyncService.refresh(getDataSyncRequest(SONAR));
        assertEquals(response.getCollectorItemCount(), 1);
        assertEquals(response.getComponentCount(), 2);
    }


    @Test
    public void dataCleanArtifact() {
        when(dataSyncService.getCollectorRepository().findByName(ARTIFACTORY)).thenReturn(getCollector(ARTIFACTORY, CollectorType.Artifact, getUniqueOptions("path", "", "instanceUrl", "")));
        ObjectId collectorItemId = ObjectId.get();
        List<CollectorItem> cis = Arrays.asList(getArtifactItem(collectorItemId, "artifactName", "repoName", "path", "http://artifactory.com"));
        Page<CollectorItem> pagedCollectorItems = new PageImpl<>(cis);
        ObjectId collectorItemId2 = ObjectId.get();
        List<CollectorItem> suspects = Arrays.asList(getArtifactItem(collectorItemId, "artifactName", "repoName", "path", "http://artifactory.com"), getArtifactItem(collectorItemId2, "artifactName2", "repoName2", "path2", "http://artifactory.com"));
        when(dataSyncService.getCollectorItemRepository().findByCollectorIdIn(any(), any())).thenReturn(pagedCollectorItems);
        when(dataSyncService.getCollectorItemRepository().findAllByOptionMapAndCollectorIdsIn(any(), any())).thenReturn(suspects);
        when(dataSyncService.getBinaryArtifactRepository().findTopByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getBinaryArtifact(collectorItemId, 1584127005000L));
        when(dataSyncService.getBinaryArtifactRepository().findTopByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getBinaryArtifact(collectorItemId2, 1581621405000L));
        when(dataSyncService.getComponentRepository().findByArtifactCollectorItems(any())).thenReturn(Arrays.asList(getComponent(collectorItemId)));
        when(dataSyncService.getCollectorItemRepository().findOne(any(ObjectId.class))).thenReturn(getArtifactItem(collectorItemId, "artifactName1", "repoName", "path", "http://artifactory.com"));
        DataSyncResponse response = dataSyncService.refresh(getDataSyncRequest(ARTIFACTORY));
        assertEquals(response.getCollectorItemCount(), 1);
        assertEquals(response.getComponentCount(), 2);
    }

    @Test
    public void dataCleanStaticSecurity() {
        when(dataSyncService.getCollectorRepository().findByName(STATIC_SECURITY)).thenReturn(getCollector(STATIC_SECURITY, CollectorType.StaticSecurityScan, getUniqueOptions("projectName", "", "instanceUrl", "")));
        ObjectId collectorItemId = ObjectId.get();
        List<CollectorItem> cis = Arrays.asList(getStaticSecurityCollectorItem(collectorItemId, "staticSecurityProjectName", "http://nexusiq.com"));
        Page<CollectorItem> pagedCollectorItems = new PageImpl<>(cis);
        ObjectId collectorItemId2 = ObjectId.get();
        List<CollectorItem> suspects = Arrays.asList(getStaticSecurityCollectorItem(collectorItemId, "staticSecurityProjectName", "http://nexusiq.com"), getStaticSecurityCollectorItem(collectorItemId2, "staticSecurityProjectName2", "http://nexusiq.com"));
        when(dataSyncService.getCollectorItemRepository().findByCollectorIdIn(any(), any())).thenReturn(pagedCollectorItems);
        when(dataSyncService.getCollectorItemRepository().findAllByOptionMapAndCollectorIdsIn(any(), any())).thenReturn(suspects);
        when(dataSyncService.getCodeQualityRepository().findTop1ByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getCodeQuality(collectorItemId, 1584127005000L));
        when(dataSyncService.getCodeQualityRepository().findTop1ByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getCodeQuality(collectorItemId2, 1581621405000L));
        when(dataSyncService.getComponentRepository().findByStaticSecurityScanCollectorItems(any())).thenReturn(Arrays.asList(getComponent(collectorItemId)));
        when(dataSyncService.getCollectorItemRepository().findOne(any(ObjectId.class))).thenReturn(getStaticSecurityCollectorItem(collectorItemId, "staticSecurityProjectName1", "http://nexusiq.com"));
        DataSyncResponse response = dataSyncService.refresh(getDataSyncRequest(STATIC_SECURITY));
        assertEquals(response.getCollectorItemCount(), 1);
        assertEquals(response.getComponentCount(), 2);
    }

    @Test
    public void dataCleanLibraryPolicy() {
        when(dataSyncService.getCollectorRepository().findByName(LIBRARY_POLICY)).thenReturn(getCollector(LIBRARY_POLICY, CollectorType.LibraryPolicy, getUniqueOptions("componentName", "", "instanceUrl", "")));
        ObjectId collectorItemId = ObjectId.get();
        List<CollectorItem> cis = Arrays.asList(getLibraryPolicyCollectorItem(collectorItemId, "libraryPolicyProjectName", "http://whitesource.com"));
        Page<CollectorItem> pagedCollectorItems = new PageImpl<>(cis);
        ObjectId collectorItemId2 = ObjectId.get();
        List<CollectorItem> suspects = Arrays.asList(getLibraryPolicyCollectorItem(collectorItemId, "libraryPolicyProjectName", "http://whitesource.com"), getLibraryPolicyCollectorItem(collectorItemId2, "libraryPolicyProjectName2", "http://whitesource.com"));
        when(dataSyncService.getCollectorItemRepository().findByCollectorIdIn(any(), any())).thenReturn(pagedCollectorItems);
        when(dataSyncService.getCollectorItemRepository().findAllByOptionMapAndCollectorIdsIn(any(), any())).thenReturn(suspects);
        when(dataSyncService.getLibraryPolicyResultsRepository().findTopByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getLibraryPolicy(collectorItemId, 1584127005000L));
        when(dataSyncService.getLibraryPolicyResultsRepository().findTopByCollectorItemIdOrderByTimestampDesc(any())).thenReturn(getLibraryPolicy(collectorItemId2, 1581621405000L));
        when(dataSyncService.getComponentRepository().findByLibraryPolicyCollectorItems(any())).thenReturn(Arrays.asList(getComponent(collectorItemId)));
        when(dataSyncService.getCollectorItemRepository().findOne(any(ObjectId.class))).thenReturn(getLibraryPolicyCollectorItem(collectorItemId, "libraryPolicyProjectName1", "http://whitesource.com"));
        DataSyncResponse response = dataSyncService.refresh(getDataSyncRequest(LIBRARY_POLICY));
        assertEquals(response.getCollectorItemCount(), 1);
        assertEquals(response.getComponentCount(), 2);
    }


    private GitHubRepo getGitRepo(ObjectId id, String url, String branch) {
        GitHubRepo repo = new GitHubRepo();
        repo.setId(id);
        repo.setBranch(branch);
        repo.setRepoUrl(url);
        return repo;
    }

    private SonarProject getSonarProject(ObjectId id, String projectName, String instanceUrl) {
        SonarProject sonarProject = new SonarProject();
        sonarProject.setId(id);
        sonarProject.setProjectName(projectName);
        sonarProject.setInstanceUrl(instanceUrl);
        return sonarProject;
    }

    private CollectorItem getArtifactItem(ObjectId id, String artifactName, String repoName, String path, String instanceUrl) {
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(id);
        Map<String, Object> options = getUniqueOptions("artifactName", artifactName, "repoName", repoName);
        options.put("path", path);
        options.put("instanceUrl", instanceUrl);
        collectorItem.setOptions(options);
        return collectorItem;
    }

    private CollectorItem getStaticSecurityCollectorItem(ObjectId id, String projectName, String instanceUrl) {
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(id);
        Map<String, Object> options = getUniqueOptions("projectName", projectName, "instanceUrl", instanceUrl);
        collectorItem.setOptions(options);
        return collectorItem;
    }

    private CollectorItem getLibraryPolicyCollectorItem(ObjectId id, String componentName, String instanceUrl) {
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(id);
        Map<String, Object> options = getUniqueOptions("componentName", componentName, "instanceUrl", instanceUrl);
        collectorItem.setOptions(options);
        return collectorItem;
    }


    private CodeQuality getCodeQuality(ObjectId collectorItemId, long timestamp) {
        CodeQuality codeQuality = new CodeQuality();
        codeQuality.setCollectorItemId(collectorItemId);
        codeQuality.setTimestamp(timestamp);
        return codeQuality;
    }

    private LibraryPolicyResult getLibraryPolicy(ObjectId collectorItemId, long timestamp) {
        LibraryPolicyResult libraryPolicyResult = new LibraryPolicyResult();
        libraryPolicyResult.setCollectorItemId(collectorItemId);
        libraryPolicyResult.setTimestamp(timestamp);
        return libraryPolicyResult;
    }

    private GitRequest getGitRequest(ObjectId collectorItemId, long timestamp) {
        GitRequest g = new GitRequest();
        g.setCollectorItemId(collectorItemId);
        g.setTimestamp(timestamp);
        return g;
    }

    private BinaryArtifact getBinaryArtifact(ObjectId id, long timestamp) {
        BinaryArtifact binaryArtifact = new BinaryArtifact();
        binaryArtifact.setTimestamp(timestamp);
        binaryArtifact.setCollectorItemId(id);
        return binaryArtifact;
    }

    private Component getComponent(ObjectId id) {
        Component component = new Component();
        component.setId(ObjectId.get());
        component.addCollectorItem(CollectorType.SCM, getGitRepo(id, "http://github.com/repo1", "master"));
        component.addCollectorItem(CollectorType.Artifact, getArtifactItem(id, "artifactName", "repoName", "path", "https://artifactory.com"));
        component.addCollectorItem(CollectorType.CodeQuality, getSonarProject(id, "sonarProject1", "http://"));
        component.addCollectorItem(CollectorType.StaticSecurityScan, getStaticSecurityCollectorItem(id, "staticSecurityProjectName1", "http://nexusiq.com"));
        component.addCollectorItem(CollectorType.LibraryPolicy, getLibraryPolicyCollectorItem(id, "libraryPolicyProjectName", "https://whitesource.com"));
        return component;
    }

    private Collector getCollector(String name, CollectorType collectorType, Map<String, Object> options) {
        Collector collector = new Collector(name, collectorType);
        collector.setUniqueFields(options);
        return collector;
    }

    private DataSyncRequest getDataSyncRequest(String github) {
        DataSyncRequest request = new DataSyncRequest();
        request.setCollectorName(github);
        return request;
    }

    private Map<String, Object> getUniqueOptions(String url, String s, String branch, String s2) {
        Map<String, Object> uniqueFields = new HashMap<>();
        uniqueFields.put(url, s);
        uniqueFields.put(branch, s2);
        return uniqueFields;
    }

}
