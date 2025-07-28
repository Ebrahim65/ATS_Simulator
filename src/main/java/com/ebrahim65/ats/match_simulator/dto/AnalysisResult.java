package com.ebrahim65.ats.match_simulator.dto;

/**
 *
 * @author Ebrahim
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private double matchPercentage;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String detectedIndustry;
    private List<String> optimizationTips;
}