package com.rapidalert.template.controller;

import com.rapidalert.template.dto.response.TemplateHistoryResponse;
import com.rapidalert.template.service.TemplateHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/templates/history")
public class TemplateHistoryController {

    private final TemplateHistoryService templateHistoryService;

    @PostMapping("/{id}")
    @Operation(summary = "create a TemplateHistory entry based on existing template")
    public ResponseEntity<TemplateHistoryResponse> create(
            @RequestHeader Long clientId,
            @PathVariable("id") Long templateId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateHistoryService.create(clientId, templateId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "get a TemplateHistory by ID")
    public ResponseEntity<TemplateHistoryResponse> get(
            @RequestHeader Long clientId,
            @PathVariable("id") Long templateId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(templateHistoryService.get(clientId, templateId));
    }
}
