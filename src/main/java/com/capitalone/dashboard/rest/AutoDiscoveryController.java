
package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;
import com.capitalone.dashboard.service.AutoDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class AutoDiscoveryController {

    private final AutoDiscoveryService autoDiscoveryService;

    @Autowired
    public AutoDiscoveryController(AutoDiscoveryService autoDiscoveryService) {
        this.autoDiscoveryService = autoDiscoveryService;
    }

    @RequestMapping(value = "/autodiscovery/updateStatus", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> remoteUpdateAutoDiscoveryStatus(@Valid @RequestBody AutoDiscoveryRemoteRequest request) {
        try {
            AutoDiscovery autoDiscovery = autoDiscoveryService.save(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Successfully updated autoDiscovery: id =" + autoDiscovery.getId());
        } catch (HygieiaException he) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Failed to update autoDiscovery. Error: " + he.getMessage());
        }
    }
}
