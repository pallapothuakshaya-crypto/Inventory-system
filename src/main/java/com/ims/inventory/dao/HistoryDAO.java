package com.ims.inventory.dao;

import java.util.List;
import com.ims.inventory.model.ActivityLog;

public interface HistoryDAO {
    void logActivity(ActivityLog log);
    List<ActivityLog> getAllLogs();
    List<ActivityLog> getLogsByUser(String username);
    List<ActivityLog> searchLogs(String keyword);
}
