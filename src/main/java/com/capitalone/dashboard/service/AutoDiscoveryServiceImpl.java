package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscoveredEntry;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.AutoDiscoveryRepository;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;
import com.capitalone.dashboard.repository.CollectorRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AutoDiscoveryServiceImpl implements AutoDiscoveryService {
    private static final Log LOG = LogFactory.getLog(AutoDiscoveryServiceImpl.class);
    private final AutoDiscoveryRepository autoDiscoveryRepository;
    private final CollectorRepository collectorRepository;

    @Autowired
    public AutoDiscoveryServiceImpl(AutoDiscoveryRepository autoDiscoveryRepository,CollectorRepository collectorRepository) {
        this.autoDiscoveryRepository = autoDiscoveryRepository;
        this.collectorRepository = collectorRepository;
    }

    @Override
    public AutoDiscovery save(AutoDiscoveryRemoteRequest request) throws HygieiaException {
        String autoDiscoveryId = request.getAutoDiscoveryId();
        if (autoDiscoveryId==null || !ObjectId.isValid(autoDiscoveryId)) {
            throw new HygieiaException("Invalid Auto Discovery Object ID: [" + autoDiscoveryId + "] received.", HygieiaException.BAD_DATA);
        }

        ObjectId id = new ObjectId(autoDiscoveryId);
        AutoDiscovery autoDiscovery = null;

        if (autoDiscoveryRepository.exists(id)) {
            // update existing AutoDiscovery record with the status from request
            autoDiscovery = autoDiscoveryRepository.findOne(id);
            updateAutoiscovery(autoDiscovery, request);
            autoDiscovery.setModifiedTimestamp(System.currentTimeMillis());
        } else {
            // create new AutoDiscovery record
            autoDiscovery = requestToAutoiscovery(request);
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
    private AutoDiscovery requestToAutoiscovery(AutoDiscoveryRemoteRequest request) {
        return new AutoDiscovery(request.getMetaData(), request.getCodeRepoEntries(), request.getBuildEntries(), request.getSecurityScanEntries(),
                request.getDeploymentEntries(), request.getLibraryScanEntries(), request.getFunctionalTestEntries(), request.getArtifactEntries(),
                request.getStaticCodeEntries(), request.getFeatureEntries());
    }


    /**
     * Update the AutoDiscovery Entries' status from the request.
     * @param autoDiscovery
     * @param request
     */
    private void updateAutoiscovery(AutoDiscovery autoDiscovery, AutoDiscoveryRemoteRequest request) {
        updateEntryStatus(request.getCodeRepoEntries(), autoDiscovery.getCodeRepoEntries());
        updateEntryStatus(request.getBuildEntries(), autoDiscovery.getBuildEntries());
        updateEntryStatus(request.getSecurityScanEntries(), autoDiscovery.getSecurityScanEntries());
        updateEntryStatus(request.getDeploymentEntries(), autoDiscovery.getDeploymentEntries());
        updateEntryStatus(request.getLibraryScanEntries(), autoDiscovery.getLibraryScanEntries());
        updateEntryStatus(request.getFunctionalTestEntries(), autoDiscovery.getFunctionalTestEntries());
        updateEntryStatus(request.getArtifactEntries(), autoDiscovery.getArtifactEntries());
        updateEntryStatus(request.getStaticCodeEntries(), autoDiscovery.getStaticCodeEntries());
        updateEntryStatus(request.getFeatureEntries(), autoDiscovery.getFeatureEntries());
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

}
