package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
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
import com.capitalone.dashboard.webhook.settings.DataSyncSettings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DataSyncServiceImpl implements DataSyncService {

    private static final Log LOG = LogFactory.getLog(DataSyncServiceImpl.class);
    private final CodeQualityRepository codeQualityRepository;
    private final SonarProjectRepository sonarProjectRepository;
    private final CollectorRepository collectorRepository;
    private final ComponentRepository componentRepository;
    private final CollectorItemRepository collectorItemRepository;
    private final BinaryArtifactRepository binaryArtifactRepository;
    private final GitRequestRepository gitRequestRepository;
    private final LibraryPolicyResultsRepository libraryPolicyResultsRepository;
    private final TestResultRepository testResultRepository;
    private final DataSyncUtils dataSyncUtils = new DataSyncUtils(this);
    @Autowired
    private ApiSettings settings;


    @Autowired
    public DataSyncServiceImpl(CodeQualityRepository codeQualityRepository, SonarProjectRepository sonarProjectRepository,
                               CollectorRepository collectorRepository, ComponentRepository componentRepository,
                               CollectorItemRepository collectorItemRepository,
                               BinaryArtifactRepository binaryArtifactRepository,
                               GitRequestRepository gitRequestRepository,
                               LibraryPolicyResultsRepository libraryPolicyResultsRepository,
                               TestResultRepository testResultRepository,
                               ApiSettings settings) {
        this.codeQualityRepository = codeQualityRepository;
        this.sonarProjectRepository = sonarProjectRepository;
        this.collectorRepository = collectorRepository;
        this.componentRepository = componentRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.binaryArtifactRepository = binaryArtifactRepository;
        this.gitRequestRepository = gitRequestRepository;
        this.libraryPolicyResultsRepository = libraryPolicyResultsRepository;
        this.testResultRepository = testResultRepository;
        this.settings = settings;

    }

    public DataSyncResponse refresh(DataSyncRequest request) {
        DataSyncSettings dataSyncSettings = settings.getDataSyncSettings();
        if (Objects.isNull(request) || Objects.isNull(request.getCollectorName()))
            return dataSyncUtils.warn("null", "Collector name is null");
        String collectorName = request.getCollectorName();
        Collector collector = getCollectorRepository().findByName(collectorName);
        if (Objects.isNull(collector)) return dataSyncUtils.warn(collectorName, "Invalid collector name");
        if (dataSyncSettings.getArtifact().equalsIgnoreCase(collectorName))
            return new ArtifactDataSyncDelegate(this, dataSyncUtils).clean(collector);
        if (dataSyncSettings.getScm().equalsIgnoreCase(collectorName))
            return new GithubDataSyncDelegate(this, dataSyncUtils).clean(collector);
        if (dataSyncSettings.getCodeQuality().equalsIgnoreCase(collectorName))
            return new SonarDataSyncDelegate(this, dataSyncUtils).clean(collector);
        if (dataSyncSettings.getStaticSecurity().equalsIgnoreCase(collectorName))
            return new StaticSecurityDataSyncDelegate(this, dataSyncUtils).clean(collector);
        if (dataSyncSettings.getLibraryPolicy().equalsIgnoreCase(collectorName))
            return new LibraryPolicyDataSyncDelegate(this, dataSyncUtils).clean(collector);
        if (CollectionUtils.isNotEmpty(dataSyncSettings.getTests())&& dataSyncSettings.getTests().contains(collectorName))
            return new TestDataSyncDelegate(this, dataSyncUtils).clean(collector);
        return dataSyncUtils.warn(collectorName, "Refresh Unsuccessful");

    }

    public BaseCollectorRepository<Collector> getCollectorRepository() {
        return this.collectorRepository;
    }

    public CollectorItemRepository getCollectorItemRepository() {
        return this.collectorItemRepository;
    }

    public BinaryArtifactRepository getBinaryArtifactRepository() {
        return this.binaryArtifactRepository;
    }

    public ComponentRepository getComponentRepository() {
        return this.componentRepository;
    }

    public GitRequestRepository getGitRequestRepository() {
        return this.gitRequestRepository;
    }

    public CodeQualityRepository getCodeQualityRepository() {
        return this.codeQualityRepository;
    }

    public LibraryPolicyResultsRepository getLibraryPolicyResultsRepository() {
        return this.libraryPolicyResultsRepository;
    }

    public TestResultRepository getTestResultRepository(){
        return this.testResultRepository;
    }

    public ApiSettings getSettings(){
        return this.settings;
    }

}
