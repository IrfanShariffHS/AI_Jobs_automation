package com.automation.controller;

import com.automation.dto.AutomationRequest;
import com.automation.dto.AutomationStatus;
import com.automation.service.AutomationOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/automation")
public class AutomationController {

    @Autowired
    private AutomationOrchestrator automationOrchestrator;

    @PostMapping("/start")
    public ResponseEntity<?> startAutomation(@RequestBody AutomationRequest request) {
        try {
            AutomationStatus status = automationOrchestrator.startAutomation(request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "automationId", status.getAutomationId(),
                "status", status.getStatus(),
                "message", "Automation started"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopAutomation(@RequestBody Map<String, String> request) {
        try {
            String automationId = request.get("automationId");
            automationOrchestrator.stopAutomation(automationId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "status", "STOPPED"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/status/{automationId}")
    public ResponseEntity<AutomationStatus> getAutomationStatus(@PathVariable String automationId) {
        AutomationStatus status = automationOrchestrator.getAutomationStatus(automationId);
        return ResponseEntity.ok(status);
    }
}
