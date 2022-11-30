package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.service.CollectorItemService;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.owasp.esapi.ESAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class CollectorItemController {

    private CollectorItemService collectorItemService;

    @Autowired
    public CollectorItemController(CollectorItemService collectorItemService){
        this.collectorItemService = collectorItemService;
    }

    @RequestMapping(path="/collector-items/cleanup", method = RequestMethod.DELETE)
    public ResponseEntity<String> cleanup(@RequestParam(value = "collectorType", required = true, defaultValue = "") String collectorType, @RequestParam(value = "collectorName", required = true, defaultValue = "") String collectorName) {
    	
    	if (StringUtils.isEmpty(ESAPI.encoder().encodeForHTML(collectorName)) || Objects.isNull(ESAPI.encoder().encodeForHTML(collectorType))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Collector type and name are required parameters");
        }
            return  collectorItemService.cleanup(ESAPI.encoder().encodeForHTML(collectorType), ESAPI.encoder().encodeForHTML(collectorName));
    }

}
