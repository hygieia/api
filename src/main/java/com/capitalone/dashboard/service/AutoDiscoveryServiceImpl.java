package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscoveredEntry;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.repository.AutoDiscoveryRepository;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutoDiscoveryServiceImpl implements AutoDiscoveryService {
    private static final Log LOG = LogFactory.getLog(AutoDiscoveryServiceImpl.class);
    private final AutoDiscoveryRepository autoDiscoveryRepository;

    @Autowired
    public AutoDiscoveryServiceImpl(AutoDiscoveryRepository autoDiscoveryRepository) {
        this.autoDiscoveryRepository = autoDiscoveryRepository;
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
        } else {
            // create new AutoDiscovery record
            autoDiscovery = requestToAutoiscovery(request);
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
        for (AutoDiscoveredEntry srcEntry : source) {
            target.stream().forEach(entry -> {
                if(entry.getOptions().equals(srcEntry.getOptions())){
                    entry.setStatus(srcEntry.getStatus());
                }
            });
        }
    }

}
