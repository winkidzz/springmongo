package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve web-based dashboard interfaces
 */
@Controller
public class DashboardController {

    /**
     * Serve the performance dashboard HTML page
     * 
     * @return the name of the view to render
     */
    @GetMapping("/dashboard")
    public String performanceDashboard() {
        return "redirect:/performance-dashboard.html";
    }
}