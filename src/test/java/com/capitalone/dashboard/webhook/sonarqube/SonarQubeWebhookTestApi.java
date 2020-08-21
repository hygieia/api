package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.SonarProjectRepository;
import com.capitalone.dashboard.settings.ApiSettings;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;

import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class SonarQubeWebhookTestApi {

    @Mock private CodeQualityRepository codeQualityRepository;
    @Mock private SonarProjectRepository sonarProjectRepository;
    @Mock private CollectorRepository collectorRepository;
    @Mock private ComponentRepository componentRepository;

    @Mock
    private ApiSettings apiSettings;

    private SonarQubeHookService sonarQubeHookService;
    @Mock
    private RestClient restClient;


    @Before
    public void init() {
        sonarQubeHookService = new SonarQubeHookServiceImpl(codeQualityRepository,sonarProjectRepository,collectorRepository, componentRepository,apiSettings,restClient);
    }

    @Test
    public void sonar6Webhook() throws Exception {
            Collector collector = new Collector();
            String collectorId = createGuid("0123456789abcdef");
            collector.setId(new ObjectId(collectorId));
            JSONObject test = getData("sonar6call.json");
            String checkResponse = sonarQubeHookService.createFromSonarQubeV1(test);
            assert(checkResponse.contains("Processing Complete for "));
            when(collectorRepository.findByName("Sonar")).thenReturn(collector);
    }

    @Test
    public void sonar7Webhook() throws Exception {
        Collector collector = new Collector();
        String collectorId = createGuid("0123456789abcdef");
        collector.setId(new ObjectId(collectorId));
        JSONObject test = getData("sonar7call.json");
        String checkResponse = sonarQubeHookService.createFromSonarQubeV1(test);
        assert(checkResponse.contains("Processing Complete for "));
        when(collectorRepository.findByName("Sonar")).thenReturn(collector);
    }

    private JSONObject getData(String filename) throws Exception {
        String data = IOUtils.toString(Resources.getResource(filename));
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(data);
        return json;
    }

    private static String createGuid(String hex) {
        byte[]  bytes = new byte[12];
        new Random().nextBytes(bytes);

        char[] hexArray = hex.toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
