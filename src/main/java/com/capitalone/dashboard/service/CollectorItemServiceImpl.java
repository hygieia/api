package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.settings.ApiSettings;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.capitalone.dashboard.model.CollectorType.*;
import static com.capitalone.dashboard.model.CollectorType.LibraryPolicy;

@Service
public class CollectorItemServiceImpl implements CollectorItemService {

    private ApiSettings apiSettings;
    private ComponentRepository componentRepository;
    private CollectorRepository collectorRepository;
    private CollectorItemRepository collectorItemRepository;
    private final Logger LOG = LoggerFactory.getLogger(CollectorServiceImpl.class);
    private final Long DAY_IN_MILLIS = 86400000l;

    @Autowired
    public CollectorItemServiceImpl(ComponentRepository componentRepository, CollectorRepository collectorRepository,
                                    CollectorItemRepository collectorItemRepository, ApiSettings apiSettings){
        this.componentRepository = componentRepository;
        this.collectorRepository = collectorRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.apiSettings = apiSettings;
    }

    public ResponseEntity<String> cleanup(String collectorTypeString, String collectorName) {

        // to time duration, get max age of items, count of items deleted (vars respectively)
        long startTime = System.currentTimeMillis();
        long endDate = startTime - (apiSettings.getCollectorItemGracePeriod() * DAY_IN_MILLIS);
        int count = 0;

        CollectorType collectorType;
        try{
            collectorType = CollectorType.fromString(collectorTypeString);
        }
        catch (IllegalArgumentException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error :: No such collector type " + collectorTypeString);
        }

        // prevent cleaning the wrong collector (returns content only if there is an error)
        ResponseEntity<String> isCleanable = isCleanableCollector(collectorType, collectorName);
        if(Objects.nonNull(isCleanable)){return isCleanable;}

        Optional<Collector> collector = collectorRepository.findByCollectorTypeAndName(collectorType, collectorName).stream().findFirst();
        if (!collector.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error :: Could not find collector: " + collectorName);
        }

        // get collectorItems that have not been updated since the endDate given
        List<CollectorItem> collectorItems = collectorItemRepository.findByCollectorIdAndLastUpdatedBefore(collector.get().getId(), endDate);
        LOG.info(String.format("deleteDisconnectedItems :: Found %d collectorItems to verify", collectorItems.size()));

        // iterate through enabled items and check if they are connected to a dashboard
        for (CollectorItem collectorItem : collectorItems) {
            if (!hasComponent(collectorType, collectorItem.getId())) {

                String loggingPrefix = String.format("findOldCollectorItems :: Removing (#%d of %d):: could not find a dashboard for the collectorItem with the following options:"
                        ,collectorItems.indexOf(collectorItem)+1, collectorItems.size());

                logDeletedCollectorItem(collectorType, collectorItem, loggingPrefix);
//                collectorItemRepository.delete(collectorItem.getId());
                count++;
            }
        }

        LOG.info(String.format("deleteDisconnectedItems :: Finished (duration=%s) :: collectorType=%s :: Found %d items with no corresponding dashboard.", System.currentTimeMillis() - startTime, collectorType.toString(), count));
        return ResponseEntity.status(HttpStatus.OK).body(String.format("Successfully removed %d  %s type collectorItems out of %d", count, collectorType.toString(), collectorItems.size()));
    }


    /**
     * ******************************************************************************
     * Bottom three methods are helper functions for the deleteDisconnectedItems()
     * ******************************************************************************
     */
    private ResponseEntity<String> isCleanableCollector(CollectorType collectorType, String collectorName){
        if (!(collectorType.equals(LibraryPolicy) || collectorType.equals(StaticSecurityScan) || collectorType.equals(SCM))){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Warning :: collectorItems of type " + collectorType.toString() + " are not cleanable");
        }
        if (!(collectorName.equalsIgnoreCase(apiSettings.getLibraryPolicyCollectorName()) || collectorName.equalsIgnoreCase(apiSettings.getSecurityScanCollectorName())
        || collectorName.equalsIgnoreCase(apiSettings.getDataSyncSettings().getScm()))){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error :: Could not find collector: " + collectorName);
        }
        return null;
    }


    private Boolean hasComponent(CollectorType collectorType, ObjectId collectorItemId){
        switch (collectorType){
            case LibraryPolicy:
                return componentRepository.findByLibraryPolicyCollectorItems(collectorItemId).stream().findFirst().isPresent();
            case SCM:
                return componentRepository.findBySCMCollectorItemId(collectorItemId).stream().findFirst().isPresent();
            case StaticSecurityScan:
                return componentRepository.findByStaticSecurityScanCollectorItems(collectorItemId).stream().findFirst().isPresent();
            // don't expect this case to be hit, but will skip deletion of item
            default:
                return true;
        }
    }

    private void logDeletedCollectorItem(CollectorType collectorType, CollectorItem collectorItem, String logPrefix){
        switch (collectorType){
            case LibraryPolicy:
                LOG.info(String.format("%s {projectToken: %s, projectName: %s}", logPrefix, collectorItem.getOptions().get("projectToken"), collectorItem.getOptions().get("projectName")));
                break;
            case SCM:
                LOG.info(String.format("%s {url: %s, branch: %s}", logPrefix, collectorItem.getOptions().get("url"), collectorItem.getOptions().get("branch")));
                break;
            case StaticSecurityScan:
                LOG.info(String.format("%s {team: %s, project: %s}", logPrefix, collectorItem.getOptions().get("team"), collectorItem.getOptions().get("project")));
                break;
        }
    }
}
