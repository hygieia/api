package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.editors.CaseInsensitiveCodeQualityTypeEditor;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.request.CodeQualityCreateRequest;
import com.capitalone.dashboard.request.CodeQualityRequest;
import com.capitalone.dashboard.service.CodeQualityService;
import com.capitalone.dashboard.util.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class CodeQualityController {

    private final HttpServletRequest httpServletRequest;
    private final CodeQualityService codeQualityService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeQualityController.class);

    @Autowired
    public CodeQualityController(HttpServletRequest httpServletRequest, CodeQualityService codeQualityService) {
        this.httpServletRequest = httpServletRequest;
        this.codeQualityService = codeQualityService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(CodeQualityType.class, new CaseInsensitiveCodeQualityTypeEditor());
    }

    @RequestMapping(value = "/quality", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<CodeQuality>> qualityData(@Valid CodeQualityRequest request) {
        return codeQualityService.search(request);
    }

    @RequestMapping(value = "/quality/static-analysis", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<CodeQuality>> qualityStaticAnalysis(@Valid CodeQualityRequest request) {
        request.setType(CodeQualityType.StaticAnalysis);
        return codeQualityService.search(request);
    }

    @RequestMapping(value = "/quality/static-analysis", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createStaticAnanlysis(@Valid @RequestBody CodeQualityCreateRequest request) throws HygieiaException {
        String response = codeQualityService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/v2/quality/static-analysis", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createStaticAnalysisV2(@Valid @RequestBody CodeQualityCreateRequest request) throws HygieiaException {
        request.setClientReference(httpServletRequest.getHeader(CommonConstants.HEADER_CLIENT_CORRELATION_ID));
        String requester = httpServletRequest.getHeader(CommonConstants.HEADER_API_USER);
        String response = codeQualityService.createV2(request);
        StringBuilder log = new StringBuilder();
        log.append("correlation_id=" + request.getClientReference() + ", application=hygieia, service=api, uri=" + httpServletRequest.getRequestURI());
        log.append( ", requester=" + requester + ", response_status=success, response_code=" +HttpStatus.CREATED.value());
        log.append( ", response_status_message=" + response + ", build_url=" + request.getBuildUrl());
        log.append( ", code_quality_type="+request.getType().toString() + ", code_quality_project_id=" + request.getProjectId());
        log.append( ", code_quality_project_name="+request.getProjectName() + ", code_quality_project_version=" + request.getProjectVersion());
        log.append( ", code_quality_nicename=" + request.getNiceName() + ", code_quality_tool=" + request.getToolName());
        log.append( ", code_quality_project_url=" + request.getProjectUrl() + ", code_quality_server=" + request.getServerUrl());
        LOGGER.info(log.toString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/quality/security-analysis", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<CodeQuality>> qualitySecurityAnalysis(@Valid CodeQualityRequest request) {
        request.setType(CodeQualityType.SecurityAnalysis);
        return codeQualityService.search(request);
    }


}
