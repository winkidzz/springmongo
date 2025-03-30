package com.example.demo.controller;

import com.example.demo.service.DataLoadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class DataLoadController {

    @Autowired
    private DataLoadService dataLoadService;

    @GetMapping("/load")
    public ResponseEntity<String> loadData() {
        return ResponseEntity.ok(dataLoadService.loadLargeDataSet());
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getStats() {
        return ResponseEntity.ok(dataLoadService.getDataStats());
    }
}