package com.ebrahim65.ats.match_simulator.service;

import com.ebrahim65.ats.match_simulator.dto.AnalysisResult;
import com.ebrahim65.ats.match_simulator.dto.AnalysisRequest;
import com.ebrahim65.ats.match_simulator.util.KeywordExtractor;
import com.ebrahim65.ats.match_simulator.util.SkillExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AnalysisService {
    private final KeywordExtractor keywordExtractor;
    private final SkillExtractor skillExtractor;
    private final FileProcessingService fileProcessingService;

    public AnalysisService(KeywordExtractor keywordExtractor,
                         SkillExtractor skillExtractor,
                         FileProcessingService fileProcessingService) {
        this.keywordExtractor = keywordExtractor;
        this.skillExtractor = skillExtractor;
        this.fileProcessingService = fileProcessingService;
    }

    public AnalysisResult analyze(AnalysisRequest request) throws IOException {
        // Validate inputs
        if (request.getJobDescription() == null || request.getJobDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Job description cannot be empty");
        }

        // Process CV content
        String cvContent = processCvContent(request);

        // Extract keywords
        List<String> jdKeywords = keywordExtractor.extractKeywords(request.getJobDescription());
        List<String> cvKeywords = keywordExtractor.extractKeywords(cvContent);

        //skill analysis results
        Map<String, Object> skillResults = skillExtractor.extractSkills(
            request.getJobDescription(), 
            cvContent
        );
        
        String detectedIndustry = (String) skillResults.get("industry");
        List<String> matchedSkills = getSkillList(skillResults, "matchedSkills");
        List<String> missingSkills = getSkillList(skillResults, "missingSkills");

        //fallback
        if (matchedSkills.isEmpty() && missingSkills.isEmpty()) {
            System.out.println("Using enhanced skill extraction fallback");
            List<String> enhancedCVSkills = enhancedSkillExtraction(cvContent);
            List<String> enhancedJDSkills = enhancedSkillExtraction(request.getJobDescription());
            
            matchedSkills = new ArrayList<>(enhancedCVSkills);
            matchedSkills.retainAll(enhancedJDSkills);
            
            missingSkills = new ArrayList<>(enhancedJDSkills);
            missingSkills.removeAll(enhancedCVSkills);
        }

        //matches and scores
        List<String> matchedKeywords = findMatches(jdKeywords, cvKeywords);
        List<String> missingKeywords = findMissing(jdKeywords, cvKeywords);

        double keywordScore = calculateMatchScore(jdKeywords.size(), matchedKeywords.size());
        double skillScore = calculateSkillScore(matchedSkills, missingSkills);
        double overallScore = (keywordScore * 0.6) + (skillScore * 0.4);

        
        List<String> tips = generateOptimizationTips(
            matchedKeywords,
            missingKeywords,
            matchedSkills,
            missingSkills,
            overallScore,
            detectedIndustry
        );

        return new AnalysisResult(
            Math.round(overallScore * 10) / 10.0,
            matchedKeywords,
            missingKeywords,
            matchedSkills,
            missingSkills,
            detectedIndustry,
            tips
        );
    }

    private List<String> enhancedSkillExtraction(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        //tech skills patterns
        List<String> skillPatterns = Arrays.asList(
            "java", "python", "javascript", "typescript",
            "sql", "mysql", "postgresql",
            "spring\\s*boot", "spring\\s*framework",
            "html", "css", "bootstrap",
            "react", "angular", "vue",
            "docker", "kubernetes",
            "aws", "azure", "cloud\\s*computing",
            "rest\\s*api", "microservices",
            "git", "github", "version\\s*control",
            "communication", "teamwork", "leadership",
            "problem\\s*solving", "time\\s*management"
        );

        List<String> foundSkills = new ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String pattern : skillPatterns) {
            Pattern compiledPattern = Pattern.compile("\\b" + pattern + "\\b", Pattern.CASE_INSENSITIVE);
            if (compiledPattern.matcher(lowerText).find()) {
                foundSkills.add(pattern.replace("\\s*", " "));
            }
        }

        return foundSkills.stream().distinct().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> getSkillList(Map<String, Object> skillResults, String key) {
        Object skills = skillResults.get(key);
        if (skills instanceof List) {
            return (List<String>) skills;
        }
        return Collections.emptyList();
    }

    private List<String> extractSkillsFromText(String text) {
        return skillExtractor.extractSkillsFromText(text);
    }

    private String processCvContent(AnalysisRequest request) throws IOException {
        if (request.getCvFile() != null && !request.getCvFile().isEmpty()) {
            return fileProcessingService.extractTextFromFile(request.getCvFile());
        } else if (request.getCvContent() != null && !request.getCvContent().trim().isEmpty()) {
            return request.getCvContent();
        }
        throw new IllegalArgumentException("CV content cannot be empty");
    }

    private List<String> findMatches(List<String> source, List<String> target) {
        return source.stream()
            .filter(target::contains)
            .collect(Collectors.toList());
    }

    private List<String> findMissing(List<String> source, List<String> target) {
        return source.stream()
            .filter(item -> !target.contains(item))
            .collect(Collectors.toList());
    }

    private double calculateMatchScore(int total, int matched) {
        return total == 0 ? 0 : ((double) matched / total) * 100;
    }

    private double calculateSkillScore(List<String> matchedSkills, List<String> missingSkills) {
        int totalSkills = matchedSkills.size() + missingSkills.size();
        return totalSkills == 0 ? 100 : ((double) matchedSkills.size() / totalSkills) * 100;
    }

    private List<String> generateOptimizationTips(
            List<String> matchedKeywords,
            List<String> missingKeywords,
            List<String> matchedSkills,
            List<String> missingSkills,
            double overallScore,
            String industry
    ) {
        List<String> tips = new ArrayList<>();
        
        // Industry context
        tips.add(String.format("Analysis for %s position:", 
            industry.substring(0, 1).toUpperCase() + industry.substring(1)));

        // Keywords
        if (!missingKeywords.isEmpty()) {
            tips.add("Important keywords to include: " + 
                String.join(", ", getTopItems(missingKeywords, 5)));
        }

        // Skills
        if (!matchedSkills.isEmpty()) {
            tips.add("Your strong skills: " + 
                String.join(", ", getTopItems(matchedSkills, 5)));
        }

        if (!missingSkills.isEmpty()) {
            tips.add("Skills to highlight: " + 
                String.join(", ", getTopItems(missingSkills, 3)));
        }

        
        if (overallScore < 40) {
            tips.add("Significant improvements needed in keyword and skill alignment.");
        } else if (overallScore < 70) {
            tips.add("Good potential match - focus on missing elements.");
        } else {
            tips.add("Excellent match with the job requirements!");
        }

        return tips;
    }

    private List<String> getTopItems(List<String> items, int max) {
        return items.stream()
            .limit(max)
            .collect(Collectors.toList());
    }
}
