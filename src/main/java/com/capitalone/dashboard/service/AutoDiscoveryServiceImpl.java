package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.repository.AutoDiscoveryRepository;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


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
        final String METHOD_NAME = "AutoDiscoveryServiceImpl.save";
        String autoDiscoveryId = request.getAutoDiscoveryId();
        if (! ObjectId.isValid(autoDiscoveryId) ) {
            throw new HygieiaException("Invalid Auto Discovery Object ID: " + autoDiscoveryId + " received.", HygieiaException.BAD_DATA);
        }

        AutoDiscovery autoDiscovery = requestToAutoiscovery( request );
        ObjectId id = new ObjectId(autoDiscoveryId);

        if (autoDiscoveryRepository.exists(id)) {
            autoDiscovery.setId(id);
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
}
