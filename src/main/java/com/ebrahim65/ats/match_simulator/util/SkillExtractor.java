package com.ebrahim65.ats.match_simulator.util;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.ebrahim65.ats.match_simulator.service.IndustryDetectionService;

@Component
public class SkillExtractor {
    private final IndustryDetectionService industryDetector;

    public SkillExtractor(IndustryDetectionService industryDetector) {
        this.industryDetector = industryDetector;
    }

    public Map<String, Object> extractSkills(String jobDescription, String resumeText) {
        System.out.println("\n===== Skill Extraction Started =====");
        
        String industry = industryDetector.detectIndustry(jobDescription);
        System.out.println("Detected Industry: " + industry);
        
        List<String> jdSkills = extractSkillsFromText(jobDescription);
        List<String> cvSkills = extractSkillsFromText(resumeText);
        
        System.out.println("JD Skills: " + jdSkills);
        System.out.println("CV Skills: " + cvSkills);

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        
        List<String> cvSkillsLower = cvSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        for (String skill : jdSkills) {
            if (cvSkillsLower.contains(skill.toLowerCase())) {
                matchedSkills.add(skill);
            } else {
                missingSkills.add(skill);
            }
        }

        System.out.println("Matched Skills: " + matchedSkills);
        System.out.println("Missing Skills: " + missingSkills);
        System.out.println("===== Skill Extraction Completed =====\n");

        Map<String, Object> result = new HashMap<>();
        result.put("industry", industry);
        result.put("matchedSkills", matchedSkills);
        result.put("missingSkills", missingSkills);
        return result;
    }

    public List<String> extractSkillsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();
        String industry = industryDetector.detectIndustry(text);
        List<String> industrySkills = industryDetector.getSkillsForIndustry(industry);

        return industrySkills.stream()
            .filter(skill -> {
                
                String skillPattern = "\\b" + 
                    skill.toLowerCase()
                        .replace("+", "\\+")
                        .replace(" ", "\\s+")
                        .replace("-", "[-\\s]?") + "\\b";
                
                Pattern pattern = Pattern.compile(skillPattern, Pattern.CASE_INSENSITIVE);
                boolean found = pattern.matcher(lowerText).find();
                
                System.out.println(String.format(
                    "Checking skill '%s' with pattern '%s': %s", 
                    skill, skillPattern, found
                ));
                
                return found;
            })
            .distinct()
            .collect(Collectors.toList());
    }
}