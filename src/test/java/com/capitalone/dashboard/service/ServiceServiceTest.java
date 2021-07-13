package com.capitalone.dashboard.service;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.capitalone.dashboard.model.*;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.ServiceRepository;
import com.capitalone.dashboard.util.URLConnectionFactory;


@RunWith(MockitoJUnitRunner.class)
public class ServiceServiceTest {

    @Mock DashboardRepository dashboardRepository;
    @Mock ServiceRepository serviceRepository;
    @Mock URLConnectionFactory urlConnectionFactory;
    @InjectMocks ServiceServiceImpl serviceService;

    @Test
    public void all() {
        serviceService.all();
        verify(serviceRepository).findAll();
    }

    @Test
    public void dashboardServices() {
        ObjectId id = ObjectId.get();
        serviceService.dashboardServices(id);
        verify(serviceRepository).findByDashboardId(id);
    }

    @Test
    public void dashboardDependentServices() {
        ObjectId id = ObjectId.get();
        serviceService.dashboardDependentServices(id);
        verify(serviceRepository).findByDependedBy(id);
    }

    @Test
    public void get() {
        ObjectId id = ObjectId.get();
        serviceService.get(id);
        verify(serviceRepository).findById(id).get();
    }

    @Test
    //@Ignore
    public void create() {
        final ObjectId id = ObjectId.get();
        final String name = "service";
        final String url = "https://abc123456.com";
        List<String> activeWidgets = new ArrayList<>();
        List<Owner> owners = new ArrayList<>();
        owners.add(new Owner("amit", AuthType.STANDARD));
        final Dashboard dashboard = new Dashboard("template", "title", new Application("app"), owners, DashboardType.Team, "ASVTEST","BAPTEST",activeWidgets, false, ScoreDisplayType.HEADER);
        when(dashboardRepository.findById(id).get()).thenReturn(dashboard);

        Service service=serviceService.create(id, name,url);

        verify(serviceRepository).save(argThat(new ArgumentMatcher<Service>() {
//            @Override
//            public boolean matches(Object o) {
//
//                Service service = (Service) o;
//               //return true;
//                return service.getName().equals(name) &&
//                        service.getDashboardId().equals(id) &&
//                        service.getStatus().equals(ServiceStatus.Warning) &&
//                        service.getApplicationName().equals(dashboard.getApplication().getName());
//            }
        	@Override
			public boolean matches(Service argument) {
        		return argument.getName().equals(name) &&
        		argument.getDashboardId().equals(id) &&
        		argument.getStatus().equals(ServiceStatus.Warning) &&
        		argument.getApplicationName().equals(dashboard.getApplication().getName());
			}
        	
        }));
    }

    @Test
    public void update() throws IOException {
        ObjectId dashId = ObjectId.get();
        Service service = new Service();
        service.setDashboardId(dashId);
        service.setLastUpdated(0l);
        String url = "http://some.url";
        service.setUrl(url);

        MockURLConnection spy = Mockito.spy(new MockURLConnection(new URL(url)));
        when(urlConnectionFactory.get(any(URL.class))).thenReturn(spy);
        
        serviceService.update(dashId, service);

        verify(urlConnectionFactory).get(any(URL.class));
        verify(spy).connect();
        verify(spy).getResponseCode();
        
        verify(serviceRepository).save(argThat(new ArgumentMatcher<Service>() {

            @Override
			public boolean matches(Service argument) {
                return argument.getLastUpdated() > 0;
            }
        }));
    }

    @Test
    public void delete() {
        ObjectId dashId = ObjectId.get();
        ObjectId serviceId = ObjectId.get();
        Service service = new Service();
        service.setDashboardId(dashId);
        when(serviceRepository.findById(serviceId).get()).thenReturn(service);

        serviceService.delete(dashId, serviceId);

        verify(serviceRepository).delete(service);
    }

    @Test
    public void addDependentService() {
        final ObjectId dashId = ObjectId.get();
        ObjectId serviceId = ObjectId.get();
        Service service = new Service();
        service.setDashboardId(ObjectId.get());
        when(serviceRepository.findById(serviceId).get()).thenReturn(service);

        serviceService.addDependentService(dashId, serviceId);

        verify(serviceRepository).save(argThat(new ArgumentMatcher<Service>() {
        	@Override
			public boolean matches(Service argument) {
                return argument.getDependedBy().contains(dashId);
            }
        }));
    }

    @Test
    public void deleteDependentService() {
        final ObjectId dashId = ObjectId.get();
        ObjectId serviceId = ObjectId.get();
        Service service = new Service();
        service.setDashboardId(dashId);
        when(serviceRepository.findById(serviceId).get()).thenReturn(service);

        serviceService.deleteDependentService(dashId, serviceId);

        verify(serviceRepository).save(argThat(new ArgumentMatcher<Service>() {
//            @Override
//            public boolean matches(Object o) {
//                Service service = (Service) o;
//                return !service.getDependedBy().contains(dashId);
//            }

			@Override
			public boolean matches(Service argument) {
				return !argument.getDependedBy().contains(dashId);
			}
        }));
    }

    class MockURLConnection extends HttpURLConnection {

		protected MockURLConnection(URL u) {
			super(u);
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean usingProxy() {
			return false;
		}

		@Override
		public void connect() throws IOException {
		}
    	
    }
    
}
