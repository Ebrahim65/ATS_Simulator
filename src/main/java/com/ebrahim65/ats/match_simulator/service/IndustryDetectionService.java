package com.ebrahim65.ats.match_simulator.service;

/**
 *
 * @author Ebrahim
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndustryDetectionService {
    private final Map<String, IndustryProfile> industryProfiles;
    private final List<String> universalSkills;

    public IndustryDetectionService(@Value("classpath:skills.json") Resource skillsResource) 
        throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(skillsResource.getInputStream());
        
        // Load industry profiles
        this.industryProfiles = new HashMap<>();
        JsonNode industries = root.path("industries");
        industries.fields().forEachRemaining(entry -> {
            IndustryProfile profile = new IndustryProfile(
                mapper.convertValue(entry.getValue().path("keywords"), 
                new TypeReference<List<String>>(){}),
                mapper.convertValue(entry.getValue().path("skills"), 
                new TypeReference<List<String>>(){})
            );
            industryProfiles.put(entry.getKey(), profile);
        });
        
        // Load universal skills
        this.universalSkills = mapper.convertValue(
            root.path("universal_skills"),
            new TypeReference<List<String>>(){}
        );
    }

    public String detectIndustry(String jobDescription) {
        String lowerDesc = jobDescription.toLowerCase();
        Map<String, Integer> industryScores = new HashMap<>();

        industryProfiles.forEach((industry, profile) -> {
            int score = profile.getKeywords().stream()
                .mapToInt(keyword -> StringUtils.countMatches(lowerDesc, keyword))
                .sum();
            industryScores.put(industry, score);
        });

        return industryScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("general");
    }

    public List<String> getSkillsForIndustry(String industry) {
        List<String> skills = new ArrayList<>();
        
        if (industryProfiles.containsKey(industry)) {
            skills.addAll(industryProfiles.get(industry).getSkills());
        }
        
        skills.addAll(universalSkills);
        return skills;
    }

    @Data
    @AllArgsConstructor
    private static class IndustryProfile {
        private List<String> keywords;
        private List<String> skills;
    }
}