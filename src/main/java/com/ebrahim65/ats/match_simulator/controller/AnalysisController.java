package com.ebrahim65.ats.match_simulator.controller;

/**
 *
 * @author Ebrahim
 */

import com.ebrahim65.ats.match_simulator.dto.AnalysisRequest;
import com.ebrahim65.ats.match_simulator.dto.AnalysisResult;
import com.ebrahim65.ats.match_simulator.service.AnalysisService;
import com.ebrahim65.ats.match_simulator.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "http://localhost:8081")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<AnalysisResult> analyze(
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam(value = "cvContent", required = false) String cvContent,
            @RequestParam(value = "cvFile", required = false) MultipartFile cvFile) {
        
        try {
            validateInputs(jobDescription, cvContent, cvFile);
            AnalysisResult result = analysisService.analyze(
                new AnalysisRequest(jobDescription, cvContent, cvFile)
            );
            
            // Temporary logging to verify skills are being returned
            //System.out.println("Matched Skills: " + result.getMatchedSkills());
            //System.out.println("Missing Skills: " + result.getMissingSkills());
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                createErrorResult(e.getMessage())
            );
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                createErrorResult(Constants.FILE_PROCESSING_ERROR + e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                createErrorResult(Constants.UNEXPECTED_ERROR + e.getMessage())
            );
        }
    }

    @GetMapping("/test")
    public String testEndpoint() {
        return "ATS Match Simulator Backend is running!";
    }

    private void validateInputs(String jobDescription, String cvContent, MultipartFile cvFile) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException(Constants.JOB_DESCRIPTION_REQUIRED);
        }
        if ((cvContent == null || cvContent.trim().isEmpty()) && 
            (cvFile == null || cvFile.isEmpty())) {
            throw new IllegalArgumentException(Constants.CV_CONTENT_REQUIRED);
        }
    }

    private AnalysisResult createErrorResult(String message) {
        return new AnalysisResult(
            0, 
            List.of(), 
            List.of(), 
            List.of(), 
            List.of(), 
            Constants.DEFAULT_INDUSTRY, 
            List.of(message)
        );
    }

    // endpoint to get supported industries
    @GetMapping("/industries")
    public ResponseEntity<List<String>> getSupportedIndustries() {
        return ResponseEntity.ok(
            List.of("technology", "healthcare", "finance", "education", "general")
        );
    }
}
