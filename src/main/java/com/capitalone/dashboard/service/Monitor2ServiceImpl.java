package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.Monitor2;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.repository.Monitor2Repository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.request.Monitor2DataCreateRequest;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Hits the repositories for saving the data to mongo and retrieving it.
@Service
public class Monitor2ServiceImpl implements Monitor2Service {
    private final Monitor2Repository monitor2Repository;
    private final DashboardRepository dashboardRepository;

    @Autowired
    public Monitor2ServiceImpl(Monitor2Repository monitor2Repository, DashboardRepository dashboardRepository) {
        this.monitor2Repository = monitor2Repository;
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public Iterable<Monitor2> all() {
        return monitor2Repository.findAll();
    }

    @Override
    public List<Monitor2> dashboardMonitor2es(ObjectId dashboardId) {
        return monitor2Repository.findByDashboardId(dashboardId);
    }

    @Override
    public Monitor2 get(ObjectId monitor2Id) {
        return monitor2Repository.findById(monitor2Id).orElse(null);
    }

    @Override
    public Monitor2 create(ObjectId dashboardId, Monitor2DataCreateRequest monitor2DataCreateRequest) {
        Monitor2 monitor2 = new Monitor2();
        monitor2.setName(monitor2DataCreateRequest.getName());
        monitor2.setUrl(monitor2DataCreateRequest.getUrl());
        monitor2.setStatus(3);
        monitor2.setDashboardId(dashboardId);
        monitor2.setLastUpdated(System.currentTimeMillis());

        Optional<Dashboard> dashboard = dashboardRepository.findById(dashboardId);
        if (dashboard.isEmpty()) return null;
        monitor2.setApplicationName(dashboard.get().getApplication().getName());

        return monitor2Repository.save(monitor2);
    }

    @Override
    public Monitor2 update(ObjectId dashboardId, Monitor2 monitor2) {
        if (!monitor2.getDashboardId().equals(dashboardId)) {
            throw new IllegalStateException("Not allowed to update this service from this dashboard!");
        }
        monitor2.setLastUpdated(System.currentTimeMillis());
        return monitor2Repository.save(monitor2);
    }

    @Override
    public void delete(ObjectId dashboardId, ObjectId monitor2Id) {
        Monitor2 service = get(monitor2Id);
        if (!service.getDashboardId().equals(dashboardId)) {
            throw new IllegalStateException("Not allowed to delete this service from this dashboard!");
        }
        monitor2Repository.delete(service);
    }
}
