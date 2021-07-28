
package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.request.DashboardRemoteRequest;
import com.capitalone.dashboard.service.DashboardRemoteService;
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
public class DashboardRemoteController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardRemoteController.class);

    private final HttpServletRequest httpServletRequest;
    private final DashboardRemoteService dashboardRemoteService;


    @Autowired
    public DashboardRemoteController(HttpServletRequest httpServletRequest, DashboardRemoteService dashboardRemoteService) {
        this.httpServletRequest = httpServletRequest;
        this.dashboardRemoteService = dashboardRemoteService;
    }

    @RequestMapping(value = "/dashboard/remoteCreate", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> remoteCreateDashboard(@Valid @RequestBody DashboardRemoteRequest request) {
        String correlation_id = httpServletRequest.getHeader(CommonConstants.HEADER_CLIENT_CORRELATION_ID);
        String requester = httpServletRequest.getHeader(CommonConstants.HEADER_API_USER);
        request.setClientReference(correlation_id);
        try {

            Dashboard dashboard = dashboardRemoteService.remoteCreate(request, false);
            final String response_message = "Successfully created dashboard: id =" + dashboard.getId();
            LOGGER.info("correlation_id="+correlation_id +", application=hygieia, ba="+dashboard.getConfigurationItemBusServName()+
                    ", component="+dashboard.getConfigurationItemBusAppName()+", service=api, uri=" + httpServletRequest.getRequestURI()+", requester="+requester+
                    ", response_status=success, response_code=" +HttpStatus.CREATED.value()+", response_status_message="+response_message);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response_message);
        } catch (Exception he) {
            final String response_message = "Failed to create dashboard. Error: " + he.getMessage();
            LOGGER.info("correlation_id="+correlation_id +", application=hygieia, ba="+request.getMetaData().getBusinessService() +
                    ", component="+request.getMetaData().getBusinessApplication()+", service=api, uri=" + httpServletRequest.getRequestURI() + ", requester="+requester+
                    ", response_status=failed, response_code=" +HttpStatus.BAD_REQUEST.value()+", response_status_message="+response_message);
            LOGGER.error("RemoteCreate receives exception", he);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response_message);
        }
    }

    @RequestMapping(value = "/dashboard/remoteUpdate", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> remoteUpdateDashboard(@Valid @RequestBody DashboardRemoteRequest request) {
        try {
            Dashboard dashboard = dashboardRemoteService.remoteCreate(request, true);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Successfully updated dashboard: id =" + dashboard.getId());
        } catch (Exception he) {
            LOGGER.error("RemoteUpdate receives exception", he);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Failed to update dashboard. Error: " + he.getMessage());
        }
    }
}
