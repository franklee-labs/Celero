package labs.franklee.celero.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.engine.DefaultCeleroEngine;
import labs.franklee.celero.engine.Report;
import labs.franklee.celero.engine.Route;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.rules.RuleBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates loading rules from a JSON file, creating an engine, and evaluating results.
 *
 * Rule file: src/main/resources/examples/coupon-rules.json
 * Scenario: determine which coupon each user qualifies for.
 */
class SimpleRuleEngine {
    private final static List<CeleroRule> rules;
    private final static List<Map<String, Object>> users;
    static {
        // Load rules from JSON file
        try {
            rules = loadRulesFromClasspath("examples/coupon-rules.json");
            System.out.println("Loaded rules: " + rules.size());
            rules.forEach(r -> System.out.printf("  [%s] %s — %s%n", r.getId(), r.getName(), r.getDescription()));
            System.out.println();

            // Define test users
            users = List.of(
                    // expected: high-value-user
                    Map.of("name", "Alice", "totalSpend", 800L, "memberLevel", "gold",
                            "registeredDays", 365L, "orderCount", 20L,
                            "status", "active", "age", 25L, "verified", true, "banned", false),
                    // expected: new-user-welcome
                    Map.of("name", "Bob", "totalSpend", 0L, "memberLevel", "normal",
                            "registeredDays", 7L, "orderCount", 0L,
                            "status", "active", "age", 20L, "verified", false, "banned", false),
                    // expected: vip-access (adult, not banned)
                    Map.of("name", "Carol", "totalSpend", 200L, "memberLevel", "silver",
                            "registeredDays", 90L, "orderCount", 5L,
                            "status", "active", "age", 22L, "verified", false, "banned", false),
                    // expected: high-value-user only (vip-access rejected — account inactive)
                    Map.of("name", "Dave", "totalSpend", 1000L, "memberLevel", "platinum",
                            "registeredDays", 200L, "orderCount", 50L,
                            "status", "inactive", "age", 30L, "verified", true, "banned", false),
                    // expected: vip-access (minor but verified)
                    Map.of("name", "Eve", "totalSpend", 50L, "memberLevel", "normal",
                            "registeredDays", 15L, "orderCount", 0L,
                            "status", "active", "age", 16L, "verified", true, "banned", false)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void runSingleRule() {
        // Create engine
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        // Evaluate a single rule
        System.out.println("=== Single rule evaluation ===");
        CeleroRule vipRule = rules.stream().filter(r -> r.getId().equals("vip-access")).findFirst().orElseThrow();
        Map<String, Object> testUser = Map.of("status", "active", "age", 17L, "verified", true, "banned", false);
        boolean result = engine.evaluate(vipRule, RuleContext.of(testUser));
        System.out.printf("Rule [%s] matched: %b%n%n", vipRule.getName(), result);
    }

    private static void runMultiRules() {
        // Create engine
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        // Evaluate rules and print results
        for (Map<String, Object> user : users) {
            String userName = (String) user.get("name");
            System.out.println("┌── User: " + userName);

            RuleContext ruleContext = RuleContext.of(user).setEnableReports(true);
            engine.evaluate(rules, ruleContext);

            for (CeleroRule rule : rules) {
                Report report = ruleContext.getReports().get(rule);
                boolean matched = isMatched(report);
                if (matched) {
                    System.out.printf("│  ✓ matched: [%s] %s%n", rule.getId(), rule.getName());
                    printMatchedConditions(report);
                } else {
                    System.out.printf("│  ✗ [%s] unmatched conditions:%n", rule.getId());
                    printUnmatchedConditions(report);
                }
            }
            System.out.println("└──────────────────────────────────────");
            System.out.println();
        }
    }

    static void main(String[] args) throws Throwable {
        runSingleRule();
        System.out.println();
        runMultiRules();
    }

    // helpers
    private static List<CeleroRule> loadRulesFromClasspath(String path) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SimpleRuleEngine.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("rule file not found: " + path);
        }
        JsonNode array = mapper.readTree(is);
        List<CeleroRule> rules = new ArrayList<>();
        for (JsonNode node : array) {
            String id = node.get("id").asText();
            String name = node.get("name").asText();
            String description = node.get("description").asText();
            String ruleJson = mapper.writeValueAsString(node.get("rule"));
            CeleroRule rule = RuleBuilder.fromJson(id, ruleJson)
                    .name(name)
                    .description(description)
                    .build();
            rules.add(rule);
        }
        return rules;
    }

    private static boolean isMatched(Report report) {
        if (report == null) return false;
        for (Route route : report.getRoutes()) {
            if (route.getUnmatched().isEmpty() && route.getSkipped().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void printMatchedConditions(Report report) {
        if (report == null) return;
        for (Route route : report.getRoutes()) {
            if (route.getUnmatched().isEmpty() && route.getSkipped().isEmpty()) {
                route.getMatched().stream()
                        .filter(item -> item.getConditionId() != null)
                        .forEach(item ->
                                System.out.printf("│      condition met: %s (%s)%n", item.getConditionName(), item.getConditionId()));
            }
        }
    }

    private static void printUnmatchedConditions(Report report) {
        if (report == null) return;
        for (Route route : report.getRoutes()) {
            route.getUnmatched().forEach(item ->
                    System.out.printf("│      not satisfied: %s (%s)%n", item.getConditionName(), item.getConditionId()));
        }
    }
}
