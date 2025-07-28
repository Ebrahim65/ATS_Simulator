package com.ebrahim65.ats.match_simulator.util;

/**
 *
 * @author Ebrahim
 */

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class KeywordExtractor {
    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "and", "for", "with", "this", "that", "are", "you", 
            "your", "will", "have", "has", "had", "can", "could", "should",
            "we're", "were", "required", "looking"
    );

    public List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String cleanedText = text.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase();
        String[] words = cleanedText.split("\\s+");

        Map<String, Integer> wordCounts = new HashMap<>();
        for (String word : words) {
            if (word.length() > 3 && !COMMON_WORDS.contains(word)) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }

        return wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
