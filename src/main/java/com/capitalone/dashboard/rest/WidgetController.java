package com.capitalone.dashboard.rest;


import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.request.*;
import com.capitalone.dashboard.service.CodeQualityService;
import com.capitalone.dashboard.service.GitRequestService;
import com.capitalone.dashboard.service.LibraryPolicyService;
import com.capitalone.dashboard.service.InfraStructureService;
import com.capitalone.dashboard.service.CommitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import com.capitalone.dashboard.model.Commit;



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
    private final GitRequestService gitRequestService;
    private final CommitService commitService;

    @Autowired
    public WidgetController(CodeQualityService codeQualityService, LibraryPolicyService libraryPolicyService,
                            InfraStructureService infraStructureService, GitRequestService gitRequestService, CommitService commitService) {
        this.codeQualityService = codeQualityService;
        this.libraryPolicyService = libraryPolicyService;
        this.infraStructureService = infraStructureService;
        this.gitRequestService = gitRequestService;
        this.commitService = commitService;
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

    // New route for "gitRequest" search that takes both component and collector Item ids
    @RequestMapping(value="/ui-widget/gitrequests/type/{type}", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<GitRequest>> widgetGitRequest(@Valid GitRequestRequest request, @PathVariable String type){
        return gitRequestService.getGitRequestForWidget(request, type);
    }

    // New route for "commits" that take both component and collector Item ids
    @RequestMapping(value="/ui-widget/commit", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<Commit>> widgetCommitRequest(@Valid CommitRequest request) {
        return commitService.getCommitsForWidget(request);
    }
}
