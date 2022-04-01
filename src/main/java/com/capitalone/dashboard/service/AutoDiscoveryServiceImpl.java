package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscoveredEntry;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.FeatureFlag;
import com.capitalone.dashboard.repository.AutoDiscoveryRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.FeatureFlagRepository;
import com.capitalone.dashboard.util.FeatureFlagsEnum;
import com.capitalone.dashboard.util.HygieiaUtils;
import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AutoDiscoveryServiceImpl implements AutoDiscoveryService {
    private static final Log LOG = LogFactory.getLog(AutoDiscoveryServiceImpl.class);
    private final AutoDiscoveryRepository autoDiscoveryRepository;
    private final CollectorRepository collectorRepository;
    private final FeatureFlagRepository featureFlagRepository;

    @Autowired
    public AutoDiscoveryServiceImpl(AutoDiscoveryRepository autoDiscoveryRepository,CollectorRepository collectorRepository, FeatureFlagRepository featureFlagRepository) {
        this.autoDiscoveryRepository = autoDiscoveryRepository;
        this.collectorRepository = collectorRepository;
        this.featureFlagRepository = featureFlagRepository;
    }

    @Override
    public AutoDiscovery save(AutoDiscoveryRemoteRequest request) throws HygieiaException {
        String autoDiscoveryId = request.getAutoDiscoveryId();
        if (autoDiscoveryId==null || !ObjectId.isValid(autoDiscoveryId)) {
            throw new HygieiaException("Invalid Auto Discovery Object ID: [" + autoDiscoveryId + "] received.", HygieiaException.BAD_DATA);
        }

        ObjectId id = new ObjectId(autoDiscoveryId);
        AutoDiscovery autoDiscovery;
        FeatureFlag featureFlag = featureFlagRepository.findByName(FeatureFlagsEnum.auto_discover.toString());

        if (autoDiscoveryRepository.exists(id)) {
            // update existing AutoDiscovery record with the status from request
            autoDiscovery = autoDiscoveryRepository.findOne(id);
            updateAutoDiscovery(autoDiscovery, request, featureFlag);
            autoDiscovery.setModifiedTimestamp(System.currentTimeMillis());
        } else {
            // create new AutoDiscovery record
            autoDiscovery = requestToAutoiscovery(request, featureFlag);
            long currTime = System.currentTimeMillis();
            autoDiscovery.setCreatedTimestamp(currTime);
            autoDiscovery.setModifiedTimestamp(currTime);
        }

        autoDiscoveryRepository.save(autoDiscovery);
        return autoDiscovery;
    }

    /**
     * Creates a AutoDiscovery object from the request.
     * @param request
     * @return AutoDiscovery
     */
    private AutoDiscovery requestToAutoiscovery(AutoDiscoveryRemoteRequest request, FeatureFlag featureFlag) {

        cleanAutoDiscoveryRequestByFeatureFlag(request, featureFlag);
        removeDuplicatesNull(request);
        return new AutoDiscovery(request.getMetaData(), request.getCodeRepoEntries(), request.getBuildEntries(), request.getSecurityScanEntries(),
                request.getDeploymentEntries(), request.getLibraryScanEntries(), request.getFunctionalTestEntries(), request.getArtifactEntries(),
                request.getStaticCodeEntries(), request.getFeatureEntries(),request.getPerformanceTestEntries(), request.getInfraStructureScanEntries());
    }


    /**
     * Update the AutoDiscovery Entries' status from the request.
     * @param autoDiscovery
     * @param request
     */
    private void updateAutoDiscovery(AutoDiscovery autoDiscovery, AutoDiscoveryRemoteRequest request, FeatureFlag featureFlag) {

        updateEntryStatus(request.getCodeRepoEntries(), autoDiscovery.getCodeRepoEntries());
        updateEntryStatus(request.getBuildEntries(), autoDiscovery.getBuildEntries());
        updateEntryStatus(request.getSecurityScanEntries(), autoDiscovery.getSecurityScanEntries());
        updateEntryStatus(request.getDeploymentEntries(), autoDiscovery.getDeploymentEntries());
        updateEntryStatus(request.getLibraryScanEntries(), autoDiscovery.getLibraryScanEntries());
        updateEntryStatus(request.getFunctionalTestEntries(), autoDiscovery.getFunctionalTestEntries());
        updateEntryStatus(request.getArtifactEntries(), autoDiscovery.getArtifactEntries());
        updateEntryStatus(request.getStaticCodeEntries(), autoDiscovery.getStaticCodeEntries());
        updateEntryStatus(request.getFeatureEntries(), autoDiscovery.getFeatureEntries());
        updateEntryStatus(request.getInfraStructureScanEntries(), autoDiscovery.getInfraStructureScanEntries());
        removeDuplicatesNull(autoDiscovery);
        removeEntriesByFeatureFlag(autoDiscovery, featureFlag);
    }

    private void removeEntriesByFeatureFlag(@NotNull AutoDiscovery autoDiscovery, FeatureFlag featureFlag) {
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.SCM)) autoDiscovery.setCodeRepoEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Build)) autoDiscovery.setBuildEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.StaticSecurityScan)) autoDiscovery.setSecurityScanEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Deployment)) autoDiscovery.setDeploymentEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.LibraryPolicy)) autoDiscovery.setLibraryScanEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Test)) autoDiscovery.setFunctionalTestEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Artifact)) autoDiscovery.setArtifactEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.CodeQuality)) autoDiscovery.setStaticCodeEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.AgileTool)) autoDiscovery.setFeatureEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.InfrastructureScan)) autoDiscovery.setInfraStructureScanEntries(new ArrayList<>());
    }

    private void cleanAutoDiscoveryRequestByFeatureFlag(@NotNull AutoDiscoveryRemoteRequest request, FeatureFlag featureFlag){
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.SCM)) request.setCodeRepoEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Build)) request.setBuildEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.StaticSecurityScan)) request.setSecurityScanEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Deployment)) request.setDeploymentEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.LibraryPolicy)) request.setLibraryScanEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Test)) request.setFunctionalTestEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Artifact)) request.setArtifactEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.CodeQuality)) request.setStaticCodeEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.AgileTool)) request.setFeatureEntries(new ArrayList<>());
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.InfrastructureScan)) request.setInfraStructureScanEntries(new ArrayList<>());
    }

    /**
     * Update the AutoDiscovery Entries' status from source to target.
     * @param source
     * @param target
     */
    private void updateEntryStatus(List<AutoDiscoveredEntry> source, List<AutoDiscoveredEntry> target) {
        boolean optionsMatched = true;
        for (AutoDiscoveredEntry srcEntry : source) {
            String toolName = srcEntry.getToolName();
            Collector collector = collectorRepository.findByName(toolName);
            Map<String, Object> uniqueOptions = collector.getUniqueFields();
            for (AutoDiscoveredEntry entry : target) {
                for (String field : uniqueOptions.keySet()) {
                    try {
                        if (!((String) entry.getOptions().get(field)).equalsIgnoreCase((String) srcEntry.getOptions().get(field))) {
                            optionsMatched = false;
                        }
                    } catch (Exception e) {
                        LOG.info("Caught exception in AutoDiscoveryServiceImpl.updateEntryStatus()-- invalid options for collectorItem." + e.getMessage());
                    }
                }

                if (optionsMatched) {
                    entry.setStatus(srcEntry.getStatus());
                }
            }
        }
    }

    /**
     * Remove Nulls and Duplicates from {@link AutoDiscovery}
     *
     * @param autoDiscovery
     */
    private void removeDuplicatesNull(@NotNull AutoDiscovery autoDiscovery) {
        autoDiscovery.setCodeRepoEntries(filterDuplicatesNull(autoDiscovery.getCodeRepoEntries()));
        autoDiscovery.setBuildEntries(filterDuplicatesNull(autoDiscovery.getBuildEntries()));
        autoDiscovery.setSecurityScanEntries(filterDuplicatesNull(autoDiscovery.getSecurityScanEntries()));
        autoDiscovery.setDeploymentEntries(filterDuplicatesNull(autoDiscovery.getDeploymentEntries()));
        autoDiscovery.setLibraryScanEntries(filterDuplicatesNull(autoDiscovery.getLibraryScanEntries()));
        autoDiscovery.setFunctionalTestEntries(filterDuplicatesNull(autoDiscovery.getFunctionalTestEntries()));
        autoDiscovery.setArtifactEntries(filterDuplicatesNull(autoDiscovery.getArtifactEntries()));
        autoDiscovery.setStaticCodeEntries(filterDuplicatesNull(autoDiscovery.getStaticCodeEntries()));
        autoDiscovery.setFeatureEntries(filterDuplicatesNull(autoDiscovery.getFeatureEntries()));
        autoDiscovery.setInfraStructureScanEntries(filterDuplicatesNull(autoDiscovery.getInfraStructureScanEntries()));
    }

    /**
     * Remove nulls and Duplicates from {@link AutoDiscoveryRemoteRequest}
     *
     * @param request
     */
    private void removeDuplicatesNull(@NotNull AutoDiscoveryRemoteRequest request) {
        request.setCodeRepoEntries(filterDuplicatesNull(request.getCodeRepoEntries()));
        request.setBuildEntries(filterDuplicatesNull(request.getBuildEntries()));
        request.setSecurityScanEntries(filterDuplicatesNull(request.getSecurityScanEntries()));
        request.setDeploymentEntries(filterDuplicatesNull(request.getDeploymentEntries()));
        request.setLibraryScanEntries(filterDuplicatesNull(request.getLibraryScanEntries()));
        request.setFunctionalTestEntries(filterDuplicatesNull(request.getFunctionalTestEntries()));
        request.setArtifactEntries(filterDuplicatesNull(request.getArtifactEntries()));
        request.setStaticCodeEntries(filterDuplicatesNull(request.getStaticCodeEntries()));
        request.setFeatureEntries(filterDuplicatesNull(request.getFeatureEntries()));
        request.setInfraStructureScanEntries(filterDuplicatesNull(request.getInfraStructureScanEntries()));
    }

    /**
     * Filter nulls and duplicates from List of {@link AutoDiscoveredEntry}
     *
     * @param entry
     */
    private List<AutoDiscoveredEntry> filterDuplicatesNull( List<AutoDiscoveredEntry> entry) {
        if(CollectionUtils.isEmpty(entry)) return entry;
        Iterables.removeIf(entry, Objects::isNull);
        return entry.stream().distinct().collect(Collectors.toList());
    }

}
