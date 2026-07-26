package com.radion.service.pipeline.placement.timeline;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class CompanyMatcher {

    private static final List<String> SUFFIXES_TO_REMOVE = List.of(
            "ltd", "limited", "pvt", "private", "technologies", "technology",
            "solutions", "services", "inc", "corp", "corporation", "llc",
            "co", "company", "india", "global", "software", "systems", "group"
    );

    private static final Pattern PUNCTUATION = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private static final List<String> LEGAL_SUFFIXES = List.of(
            "ltd", "limited", "pvt", "private", "inc", "corp", "corporation", 
            "llc", "co", "company", "india", "global"
    );

    /**
     * Normalizes a company name by stripping punctuation, extra spaces, and legal/business suffixes.
     * Example: "Tata Consultancy Services Pvt. Ltd." -> "tata consultancy"
     *
     * @param rawName Original company name.
     * @return Normalized company string used strictly for similarity matching.
     */
    public String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }
        String clean = PUNCTUATION.matcher(rawName.toLowerCase()).replaceAll(" ");
        String[] words = MULTI_SPACE.split(clean.trim());
        
        StringBuilder normalized = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && !SUFFIXES_TO_REMOVE.contains(word)) {
                if (normalized.length() > 0) {
                    normalized.append(" ");
                }
                normalized.append(word);
            }
        }
        String result = normalized.toString().trim();
        return result.isEmpty() ? clean.trim() : result;
    }

    /**
     * Generates an acronym from the company name (ignoring pure legal suffixes like Ltd/Pvt).
     * Example: "Tata Consultancy Services Ltd." -> "tcs"
     */
    public String getAcronym(String rawName) {
        if (rawName == null || rawName.isBlank()) return "";
        String clean = PUNCTUATION.matcher(rawName.toLowerCase()).replaceAll(" ");
        String[] words = MULTI_SPACE.split(clean.trim());
        StringBuilder ac = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && !LEGAL_SUFFIXES.contains(word)) {
                ac.append(word.charAt(0));
            }
        }
        return ac.toString();
    }

    /**
     * Computes similarity between two company names using normalized Levenshtein ratio, containment, and acronym matching.
     *
     * @param s1 First company name.
     * @param s2 Second company name.
     * @return Similarity score between 0.0 and 1.0.
     */
    public double calculateSimilarity(String s1, String s2) {
        String n1 = normalize(s1);
        String n2 = normalize(s2);

        if (n1.isEmpty() || n2.isEmpty()) {
            return 0.0;
        }
        if (n1.equals(n2)) {
            return 1.0;
        }
        if ((n1.length() >= 3 && n2.contains(n1)) || (n2.length() >= 3 && n1.contains(n2))) {
            return 1.0;
        }

        String a1 = getAcronym(s1);
        String a2 = getAcronym(s2);
        if ((!a1.isEmpty() && (a1.equals(n2) || a1.equals(a2))) || 
            (!a2.isEmpty() && (a2.equals(n1) || a2.equals(a1)))) {
            return 1.0;
        }

        int dist = levenshteinDistance(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) dist / maxLen);
    }

    /**
     * Determines if two company names match based on a configurable similarity threshold.
     *
     * @param s1 First company name.
     * @param s2 Second company name.
     * @param threshold Configurable similarity threshold (e.g. 0.75).
     * @return true if similarity >= threshold.
     */
    public boolean isMatch(String s1, String s2, double threshold) {
        return calculateSimilarity(s1, s2) >= threshold;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }
}
