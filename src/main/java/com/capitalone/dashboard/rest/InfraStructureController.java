package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.service.InfraStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfraStructureController {

    @Autowired
    public InfraStructureController(InfraStructureService infraStructureService) {
    }
}
