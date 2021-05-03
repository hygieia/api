package com.capitalone.dashboard.rest;


import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.request.CodeQualityRequest;
import com.capitalone.dashboard.request.LibraryPolicyRequest;
import com.capitalone.dashboard.service.CodeQualityService;
import com.capitalone.dashboard.service.LibraryPolicyService;
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

    @Autowired
    public WidgetController(CodeQualityService codeQualityService, LibraryPolicyService libraryPolicyService) {
        this.codeQualityService = codeQualityService;
        this.libraryPolicyService = libraryPolicyService;
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


}
