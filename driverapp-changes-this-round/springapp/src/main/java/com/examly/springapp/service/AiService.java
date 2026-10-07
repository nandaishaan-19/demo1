package com.examly.springapp.service;

import com.examly.springapp.dto.DriverDTO;
import com.examly.springapp.model.Driver;
import com.examly.springapp.model.Feedback;
import com.examly.springapp.repository.DriverRepo;
import com.examly.springapp.repository.FeedbackRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI features: semantic driver search, feedback sentiment / tagging and per-driver review summary.
 * Gemini is used when an API key is configured; otherwise (or when a call fails) a local
 * keyword based fallback keeps every feature - and the existing test cases - working.
 */
@Service
public class AiService {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "and", "the", "for", "to", "of", "in", "on", "at", "with", "my", "me", "i", "is", "are", "need",
            "want", "looking", "find", "who", "that", "can", "please", "driver", "drivers", "trip", "good", "best"));

    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "good", "great", "excellent", "amazing", "awesome", "polite", "friendly", "safe", "smooth", "comfortable",
            "clean", "punctual", "professional", "helpful", "perfect", "best", "happy", "recommend", "careful", "prompt"));

    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "bad", "poor", "terrible", "rude", "late", "dirty", "unsafe", "rash", "worst", "horrible", "delay",
            "delayed", "unprofessional", "slow", "angry", "uncomfortable", "overcharged", "cancelled", "reckless"));

    private static final Map<String, String[]> TAG_KEYWORDS = new LinkedHashMap<>();
    static {
        TAG_KEYWORDS.put("punctuality", new String[]{"punctual", "on time", "late", "delay", "prompt", "waiting"});
        TAG_KEYWORDS.put("safety", new String[]{"safe", "unsafe", "rash", "careful", "reckless", "speed", "accident"});
        TAG_KEYWORDS.put("cleanliness", new String[]{"clean", "dirty", "hygiene", "smell"});
        TAG_KEYWORDS.put("behaviour", new String[]{"polite", "rude", "friendly", "behaviour", "behavior", "courteous", "attitude"});
        TAG_KEYWORDS.put("driving skill", new String[]{"smooth", "driving", "route", "navigation", "skilled", "experienced"});
        TAG_KEYWORDS.put("pricing", new String[]{"price", "charge", "overcharged", "fare", "cost", "cheap", "expensive"});
        TAG_KEYWORDS.put("vehicle condition", new String[]{"vehicle", "car", "comfortable", "condition", "seat"});
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private DriverRepo driverRepo;

    @Autowired
    private FeedbackRepo feedbackRepo;

    @Autowired
    private ModelMapper modelMapper;

    // ------------------------------------------------------------------ semantic driver search

    /** Returns the matching drivers as DTOs (entities never leave the service layer). */
    public List<DriverDTO> searchDrivers(String query) {
        List<Driver> drivers = driverRepo.findAll();
        if (query == null || query.isBlank() || drivers.isEmpty()) {
            return toDtos(drivers);
        }
        List<Driver> semantic = semanticSearch(query, drivers);
        return toDtos(semantic != null ? semantic : keywordSearch(query, drivers));
    }

    private List<DriverDTO> toDtos(List<Driver> drivers) {
        return drivers.stream().map(driver -> modelMapper.map(driver, DriverDTO.class)).collect(Collectors.toList());
    }

    /** Embedding + cosine similarity ranking; null when Gemini is unavailable. */
    private List<Driver> semanticSearch(String query, List<Driver> drivers) {
        if (!geminiService.isEnabled()) {
            return null;
        }
        float[] queryVector = geminiService.embed(query);
        if (queryVector == null) {
            return null;
        }
        Map<Driver, Double> scores = new LinkedHashMap<>();
        for (Driver driver : drivers) {
            float[] vector = driverVector(driver);
            if (vector == null) {
                return null;
            }
            scores.put(driver, cosine(queryVector, vector));
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Driver, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private float[] driverVector(Driver driver) {
        String profile = driverProfile(driver);
        float[] cached = embeddingCache.get(profile);
        if (cached != null) {
            return cached;
        }
        float[] vector = geminiService.embed(profile);
        if (vector != null) {
            embeddingCache.put(profile, vector);
        }
        return vector;
    }

    private String driverProfile(Driver driver) {
        return "Driver " + driver.getDriverName() + ", vehicle: " + driver.getVehicleType()
                + ", experience: " + driver.getExperienceYears() + " years"
                + ", hourly rate: " + driver.getHourlyRate()
                + ", status: " + driver.getAvailabilityStatus()
                + ", address: " + driver.getAddress();
    }

    static double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** Local fallback: scores each driver by the query terms found in its profile. */
    private List<Driver> keywordSearch(String query, List<Driver> drivers) {
        List<String> tokens = tokenize(query).stream().filter(t -> !STOP_WORDS.contains(t)).collect(Collectors.toList());
        boolean wantsExperience = tokens.stream().anyMatch(t -> t.startsWith("experienc") || t.equals("senior") || t.equals("expert"));
        boolean wantsCheap = tokens.stream().anyMatch(t -> t.equals("cheap") || t.equals("affordable") || t.equals("budget"));

        Map<Driver, Double> scores = new LinkedHashMap<>();
        for (Driver driver : drivers) {
            String text = driverProfile(driver).toLowerCase();
            double score = 0;
            for (String token : tokens) {
                if (text.contains(token)) {
                    score += 2;
                }
            }
            if (score > 0 || tokens.isEmpty()) {
                if (wantsExperience && driver.getExperienceYears() != null) {
                    score += Math.min(driver.getExperienceYears(), 10) / 5.0;
                }
                if (wantsCheap && driver.getHourlyRate() != null) {
                    score += 1.0 / (1 + driver.getHourlyRate() / 100.0);
                }
                if ("Active".equalsIgnoreCase(driver.getAvailabilityStatus())) {
                    score += 0.5;
                }
                scores.put(driver, score);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Driver, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+")).filter(t -> t.length() > 1).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ sentiment + tags

    /** Returns sentiment (Positive/Neutral/Negative), sentimentScore (0..1) and aiTags (comma separated). */
    public Map<String, Object> analyzeFeedback(String text, String category, Integer rating) {
        Map<String, Object> result = analyzeWithGemini(text, category, rating);
        return result != null ? result : analyzeLocally(text, rating);
    }

    private Map<String, Object> analyzeWithGemini(String text, String category, Integer rating) {
        if (!geminiService.isEnabled() || text == null || text.isBlank()) {
            return null;
        }
        String prompt = "Classify this customer feedback about a hired driver. Respond ONLY with JSON of the form "
                + "{\"sentiment\":\"Positive|Neutral|Negative\",\"sentimentScore\":0.0-1.0,\"tags\":[\"short theme\",...]} "
                + "(sentimentScore 0 = very negative, 1 = very positive, at most 4 tags).\n"
                + "Category: " + category + "\nRating (1-5): " + rating + "\nFeedback: " + text;
        String raw = geminiService.generateJson(prompt);
        if (raw == null) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(stripFences(raw));
            String sentiment = normalizeSentiment(node.path("sentiment").asText(""));
            if (sentiment == null) {
                return null;
            }
            List<String> tags = new ArrayList<>();
            node.path("tags").forEach(t -> tags.add(t.asText().trim().toLowerCase()));
            return result(sentiment, clamp(node.path("sentimentScore").asDouble(0.5)), String.join(",", tags));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> analyzeLocally(String text, Integer rating) {
        String lower = text == null ? "" : text.toLowerCase();
        List<String> words = tokenize(lower);
        long positive = words.stream().filter(POSITIVE_WORDS::contains).count();
        long negative = words.stream().filter(NEGATIVE_WORDS::contains).count();
        double textScore = clamp(0.5 + 0.15 * (positive - negative));
        double score = rating != null && rating > 0 ? (textScore + (rating - 1) / 4.0) / 2.0 : textScore;
        String sentiment = score >= 0.6 ? "Positive" : score <= 0.4 ? "Negative" : "Neutral";

        List<String> tags = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : TAG_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    tags.add(entry.getKey());
                    break;
                }
            }
        }
        return result(sentiment, Math.round(score * 100.0) / 100.0, String.join(",", tags));
    }

    private Map<String, Object> result(String sentiment, double score, String tags) {
        Map<String, Object> result = new HashMap<>();
        result.put("sentiment", sentiment);
        result.put("sentimentScore", score);
        result.put("aiTags", tags);
        return result;
    }

    /** Back-fills sentiment and tags for feedback created before the AI feature existed. */
    public int analyzeExistingFeedback() {
        int updated = 0;
        for (Feedback feedback : feedbackRepo.findAll()) {
            if (feedback.getSentiment() == null || feedback.getSentiment().isBlank()) {
                applyAnalysis(feedback);
                feedbackRepo.save(feedback);
                updated++;
            }
        }
        return updated;
    }

    public void applyAnalysis(Feedback feedback) {
        Map<String, Object> analysis = analyzeFeedback(feedback.getFeedbackText(), feedback.getCategory(), feedback.getRating());
        feedback.setSentiment((String) analysis.get("sentiment"));
        feedback.setSentimentScore((Double) analysis.get("sentimentScore"));
        feedback.setAiTags((String) analysis.get("aiTags"));
    }

    // ------------------------------------------------------------------ per-driver summary

    public Map<String, Object> summarizeDriverFeedback(Long driverId) {
        List<Feedback> feedbacks = feedbackRepo.findByDriverDriverId(driverId);
        int count = feedbacks.size();
        double average = feedbacks.stream().filter(f -> f.getRating() != null).mapToInt(Feedback::getRating).average().orElse(0);
        average = Math.round(average * 10.0) / 10.0;
        long positive = feedbacks.stream().filter(f -> "Positive".equals(f.getSentiment())).count();
        int aiScore = count == 0 ? 0 : (int) Math.round(average / 5.0 * 70 + (double) positive / count * 30);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "");
        result.put("strengths", new ArrayList<String>());
        result.put("improvements", new ArrayList<String>());
        result.put("aiScore", aiScore);
        result.put("averageRating", average);
        result.put("feedbackCount", count);

        if (count == 0) {
            result.put("summary", "No feedback has been posted for this driver yet.");
            return result;
        }
        if (!summarizeWithGemini(feedbacks, result)) {
            summarizeLocally(feedbacks, average, positive, result);
        }
        return result;
    }

    private boolean summarizeWithGemini(List<Feedback> feedbacks, Map<String, Object> result) {
        if (!geminiService.isEnabled()) {
            return false;
        }
        String reviews = feedbacks.stream()
                .limit(40)
                .map(f -> "- (" + f.getRating() + "/5, " + f.getCategory() + ") " + f.getFeedbackText())
                .collect(Collectors.joining("\n"));
        String raw = geminiService.generateJson("Summarise these customer reviews of one driver. Respond ONLY with JSON "
                + "{\"summary\":\"2-3 sentences\",\"strengths\":[\"...\"],\"improvements\":[\"...\"]} "
                + "(at most 4 short items per list).\nReviews:\n" + reviews);
        if (raw == null) {
            return false;
        }
        try {
            JsonNode node = mapper.readTree(stripFences(raw));
            String summary = node.path("summary").asText("");
            if (summary.isBlank()) {
                return false;
            }
            result.put("summary", summary);
            result.put("strengths", toList(node.path("strengths")));
            result.put("improvements", toList(node.path("improvements")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void summarizeLocally(List<Feedback> feedbacks, double average, long positive, Map<String, Object> result) {
        Map<String, Integer> good = new HashMap<>();
        Map<String, Integer> bad = new HashMap<>();
        for (Feedback feedback : feedbacks) {
            boolean isGood = "Positive".equals(feedback.getSentiment()) || (feedback.getRating() != null && feedback.getRating() >= 4);
            boolean isBad = "Negative".equals(feedback.getSentiment()) || (feedback.getRating() != null && feedback.getRating() <= 2);
            String tags = feedback.getAiTags() == null ? "" : feedback.getAiTags();
            for (String tag : tags.split(",")) {
                if (!tag.isBlank()) {
                    if (isGood) good.merge(tag.trim(), 1, Integer::sum);
                    if (isBad) bad.merge(tag.trim(), 1, Integer::sum);
                }
            }
        }
        List<String> strengths = topKeys(good).stream().map(t -> "Customers praise the " + t).collect(Collectors.toList());
        List<String> improvements = topKeys(bad).stream().map(t -> "Customers raised concerns about " + t).collect(Collectors.toList());
        if (strengths.isEmpty() && average >= 4) {
            strengths.add("Consistently high ratings from customers");
        }
        if (improvements.isEmpty()) {
            improvements.add("No recurring issues reported");
        }
        result.put("summary", "Based on " + feedbacks.size() + " review(s) with an average rating of " + average + "/5, "
                + positive + " of them were positive.");
        result.put("strengths", strengths);
        result.put("improvements", improvements);
    }

    // ------------------------------------------------------------------ helpers

    private List<String> topKeys(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private List<String> toList(JsonNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(item -> {
            if (!item.asText().isBlank()) list.add(item.asText().trim());
        });
        return list;
    }

    private String normalizeSentiment(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.startsWith("pos")) return "Positive";
        if (v.startsWith("neg")) return "Negative";
        if (v.startsWith("neu")) return "Neutral";
        return null;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String stripFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*", "");
            s = s.replaceFirst("```\\s*$", "");
        }
        return s.trim();
    }
}
