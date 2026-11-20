package com.ims.inventory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ims.inventory.dao.HistoryDAO;
import com.ims.inventory.model.ActivityLog;

@Service
public class HistoryService {

    private final HistoryDAO historyDAO;

    public HistoryService(HistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    public void log(String actionType, String entityType, String entityName, String details, String performedBy) {
        historyDAO.logActivity(new ActivityLog(actionType, entityType, entityName, details, performedBy));
    }

    public List<ActivityLog> getAllLogs() {
        return historyDAO.getAllLogs();
    }

    public List<ActivityLog> getLogsByUser(String username) {
        return historyDAO.getLogsByUser(username);
    }

    public List<ActivityLog> searchLogs(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllLogs();
        }
        return historyDAO.searchLogs(keyword.trim());
    }
}
