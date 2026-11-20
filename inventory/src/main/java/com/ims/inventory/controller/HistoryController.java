package com.ims.inventory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.ims.inventory.model.ActivityLog;
import com.ims.inventory.service.HistoryService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<ActivityLog> getAll(HttpServletRequest request) {
        return historyService.getAllLogs();
    }

    @GetMapping("/search")
    public List<ActivityLog> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        return historyService.searchLogs(keyword);
    }
}
