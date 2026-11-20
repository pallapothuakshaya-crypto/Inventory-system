package com.ims.inventory.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private Long logId;
    private String actionType; // CREATE, UPDATE, DELETE, LOGIN
    private String entityType; // PRODUCT, CATEGORY, SUPPLIER, USER
    private String entityName;
    private String details;
    private String performedBy;
    private LocalDateTime timestamp;

    public ActivityLog() {}

    public ActivityLog(Long logId, String actionType, String entityType, String entityName, String details, String performedBy, LocalDateTime timestamp) {
        this.logId = logId;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityName = entityName;
        this.details = details;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }

    public ActivityLog(String actionType, String entityType, String entityName, String details, String performedBy) {
        this(null, actionType, entityType, entityName, details, performedBy, null);
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
