package com.mwrcybersec.viewql.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(Model model) {
        return "dashboard";
    }

    @GetMapping("/diagnostics")
    public String diagnosticsPage(Model model) {
        try {
            // Add any initialization logic here
            return "diagnostics";
        } catch (Exception e) {
            model.addAttribute("message", "Error loading diagnostics page: " + e.getMessage());
            e.printStackTrace(); // For server-side logging
            return "error";
        }
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }
}