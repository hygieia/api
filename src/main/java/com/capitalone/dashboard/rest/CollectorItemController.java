package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.editors.CaseInsensitiveCollectorTypeEditor;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.service.CollectorItemService;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import com.capitalone.dashboard.settings.ApiSettings;

@RestController
public class CollectorItemController {

    private ApiSettings apiSettings;
    private CollectorItemService collectorItemService;

    @Autowired
    public CollectorItemController(CollectorItemService collectorItemService){
        this.collectorItemService = collectorItemService;
    }

    @RequestMapping(path="/collectorItem/deleteDisconnectedItems", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDisconnectedItems(@RequestParam(value = "collectorType", required = true, defaultValue = "") String collectorType, @RequestParam(value = "collectorName", required = true, defaultValue = "") String collectorName) {
        if (StringUtils.isEmpty(collectorName) || Objects.isNull(collectorType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Collector type and name are required parameters");
        }
            return  collectorItemService.deleteDisconnectedItems(collectorType, collectorName);
    }

}
