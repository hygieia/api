package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.editors.CaseInsensitiveBuildStatusEditor;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.BuildSearchRequest;
import com.capitalone.dashboard.response.BuildDataCreateResponse;
import com.capitalone.dashboard.service.BuildCommonService;
import com.capitalone.dashboard.service.BuildService;
import com.capitalone.dashboard.util.CommonConstants;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class BuildController {

    private final HttpServletRequest httpServletRequest;
    private final BuildService buildService;
    private final BuildCommonService buildCommonService;

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildController.class);

    @Autowired
    public BuildController(HttpServletRequest httpServletRequest, BuildService buildService,
                           BuildCommonService buildCommonService) {
        this.httpServletRequest = httpServletRequest;
        this.buildService = buildService;
        this.buildCommonService = buildCommonService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BuildStatus.class, new CaseInsensitiveBuildStatusEditor());
    }

    @RequestMapping(value = "/build-details/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    public Build build(@PathVariable ObjectId id) {
        return buildCommonService.get(id);
    }

    @RequestMapping(value = "/build", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<Build>> builds(@Valid BuildSearchRequest request) {
        return buildService.search(request);
    }

    @RequestMapping(value = "/build", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createBuild(@Valid @RequestBody BuildDataCreateRequest request) throws HygieiaException {
        String response = buildService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/v2/build", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createBuildv2(@Valid @RequestBody BuildDataCreateRequest request) throws HygieiaException {
        String response = buildService.createV2(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/v3/build", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<BuildDataCreateResponse> createBuildv3(@Valid @RequestBody BuildDataCreateRequest request) throws HygieiaException {
        request.setClientReference(httpServletRequest.getHeader(CommonConstants.HEADER_CLIENT_CORRELATION_ID));
        String requester = httpServletRequest.getHeader(CommonConstants.HEADER_API_USER);
        BuildDataCreateResponse response = buildService.createV3(request);
        String response_message = "Successfully created/updated build : "+ response.getId();
        LOGGER.info("correlation_id="+response.getClientReference() +", application=hygieia, service=api, uri=" + httpServletRequest.getRequestURI()
                + ", requester=" + requester + ", response_status=success, response_code=" + HttpStatus.CREATED.value()
                + ", response_status_message=" + response_message + ", build_url=" + request.getBuildUrl()
                + ", build_status=" + BuildStatus.fromString(request.getBuildStatus()));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(CommonConstants.HEADER_CLIENT_CORRELATION_ID,response.getClientReference())
                .body(response);
    }
}
