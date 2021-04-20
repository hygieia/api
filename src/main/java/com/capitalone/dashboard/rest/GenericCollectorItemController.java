package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.GenericCollectorItemCreateRequest;
import com.capitalone.dashboard.service.GenericCollectorItemService;
import com.capitalone.dashboard.util.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class GenericCollectorItemController {

    private final HttpServletRequest httpServletRequest;
    private final GenericCollectorItemService genericCollectorItemService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericCollectorItemController.class);

    @Autowired
    public GenericCollectorItemController(HttpServletRequest httpServletRequest, GenericCollectorItemService genericCollectorItemService) {
        this.httpServletRequest = httpServletRequest;
        this.genericCollectorItemService = genericCollectorItemService;
    }


    @RequestMapping(value = "/generic-item", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createGenericItem (@Valid @RequestBody GenericCollectorItemCreateRequest request) throws HygieiaException {
        request.setClientReference(httpServletRequest.getHeader(CommonConstants.HEADER_CLIENT_CORRELATION_ID));
        String requester = httpServletRequest.getHeader(CommonConstants.HEADER_API_USER);
        String response = genericCollectorItemService.create(request);
        StringBuilder log = new StringBuilder();
        log.append("correlation_id=" + request.getClientReference() + ", application=hygieia, service=api, uri=" + httpServletRequest.getRequestURI());
        log.append(", requester=" + requester + ", response_status=success, response_code=" +HttpStatus.CREATED.value());
        log.append(", response_status_message=" + response + ", build_url=" + request.getBuildUrl());
        log.append(", gce_request_pattern=" + request.getPattern() + ", gce_tool=" + request.getToolName());
        log.append(", gce_capture_pattern="+request.getPattern() + ", gce_source=" + request.getSource());
        log.append(", gce_related_coll_item=" + request.getRelatedCollectorItemId() + ", gce_raw_data=" + request.getRawData());
        LOGGER.info(log.toString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/generic-binary-artifact", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createGenericBinaryArtifact (@Valid @RequestBody GenericCollectorItemCreateRequest request) throws HygieiaException {
        String response = genericCollectorItemService.createGenericBinaryArtifactData(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
