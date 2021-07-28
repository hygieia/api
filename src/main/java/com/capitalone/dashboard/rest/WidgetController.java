package com.capitalone.dashboard.rest;


import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.request.CodeQualityRequest;
import com.capitalone.dashboard.request.LibraryPolicyRequest;
import com.capitalone.dashboard.service.CodeQualityService;
import com.capitalone.dashboard.service.LibraryPolicyService;
import com.capitalone.dashboard.model.InfrastructureScan;
import com.capitalone.dashboard.request.InfraStructureRequest;
import com.capitalone.dashboard.service.InfraStructureService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class WidgetController {

    private final CodeQualityService codeQualityService;
    private final LibraryPolicyService libraryPolicyService;
    private final InfraStructureService infraStructureService;

    @Autowired
    public WidgetController(CodeQualityService codeQualityService, LibraryPolicyService libraryPolicyService,
                            InfraStructureService infraStructureService) {
        this.codeQualityService = codeQualityService;
        this.libraryPolicyService = libraryPolicyService;
        this.infraStructureService = infraStructureService;
    }

    @RequestMapping(value = "/ui-widget/code-quality", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<CodeQuality>> widgetCodeQuality(@Valid CodeQualityRequest request) {
        request.setType(CodeQualityType.SecurityAnalysis);
        return codeQualityService.getCodeQualityForWidget(request);
    }

    @RequestMapping(value = "/ui-widget/library-policy", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<LibraryPolicyResult>> widgetLibraryPolicy(@Valid LibraryPolicyRequest request) {
        return libraryPolicyService.getLibraryPolicyForWidget(request);
    }

    @RequestMapping(value = "/ui-widget/infra-scan", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<InfrastructureScan>> widgetInfraScan(@Valid InfraStructureRequest request) {
        return infraStructureService.getInfraScanForWidget(request);
    }
}
