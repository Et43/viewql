package com.mwrcybersec.viewql.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.mwrcybersec.viewql.service.DatabaseService;

@Controller
@RequestMapping("/databases")
public class DatabaseController {
    private final DatabaseService databaseService;

    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @GetMapping
    public String listDatabases(Model model) {
        model.addAttribute("databases", databaseService.listDatabases());
        return "databases";
    }

    @PostMapping("/{id}/scan")
    @ResponseBody
    public String runScan(@PathVariable String id) {
        databaseService.runSecurityScan(id);
        return "Scan completed successfully";
    }

    @GetMapping("/{id}/results")
    @ResponseBody
    public String getScanResults(@PathVariable String id) {
        return databaseService.getScanResults(id);
    }
}