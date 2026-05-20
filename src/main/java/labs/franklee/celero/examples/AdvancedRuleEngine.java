package labs.franklee.celero.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import labs.franklee.celero.engine.AdvancedCeleroEngine;
import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.engine.Report;
import labs.franklee.celero.engine.Route;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.rules.RuleBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Demonstrates AdvancedCeleroEngine which evaluates rules to a three-state result:
 *   TRUE          — all conditions matched
 *   FALSE         — at least one condition did not match
 *   INDETERMINATE — no condition was false, but at least one required field was absent
 *
 * Compared to DefaultCeleroEngine (boolean), the advanced engine does not treat a
 * missing field as an automatic FALSE — it defers the decision to the caller.
 *
 * Rule file: src/main/resources/examples/coupon-rules.json
 */
class AdvancedRuleEngine {

    private static final List<CeleroRule> rules;
    private static final List<Map<String, Object>> users;

    static {
        try {
            rules = loadRulesFromClasspath("examples/coupon-rules.json");
            System.out.println("Loaded rules: " + rules.size());
            rules.forEach(r -> System.out.printf("  [%s] %s — %s%n", r.getId(), r.getName(), r.getDescription()));
            System.out.println();

            users = List.of(
                    // expected: TRUE  for high-value-user + vip-access
                    Map.of("name", "Alice", "totalSpend", 800L, "memberLevel", "gold",
                            "registeredDays", 365L, "orderCount", 20L,
                            "status", "active", "age", 25L, "verified", true, "banned", false),
                    // expected: TRUE  for new-user-welcome + vip-access
                    Map.of("name", "Bob", "totalSpend", 0L, "memberLevel", "normal",
                            "registeredDays", 7L, "orderCount", 0L,
                            "status", "active", "age", 20L, "verified", false, "banned", false),
                    // expected: INDETERMINATE for high-value-user (memberLevel absent)
                    //           TRUE          for vip-access
                    Map.of("name", "Frank", "totalSpend", 800L,
                            "registeredDays", 180L, "orderCount", 10L,
                            "status", "active", "age", 28L, "verified", true, "banned", false),
                    // expected: FALSE         for high-value-user (spend too low)
                    //           INDETERMINATE for vip-access (age + verified absent)
                    Map.of("name", "Grace", "totalSpend", 100L, "memberLevel", "normal",
                            "registeredDays", 60L, "orderCount", 3L,
                            "status", "active", "banned", false),
                    // expected: FALSE for all rules
                    Map.of("name", "Henry", "totalSpend", 200L, "memberLevel", "bronze",
                            "registeredDays", 90L, "orderCount", 8L,
                            "status", "inactive", "age", 35L, "verified", false, "banned", true)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void runSingleRule() {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

        System.out.println("=== Single rule evaluation ===");
        CeleroRule vipRule = rules.stream().filter(r -> r.getId().equals("vip-access")).findFirst().orElseThrow();

        // Missing "age" and "verified" — OR branch cannot be resolved
        Map<String, Object> incompleteUser = Map.of("status", "active", "banned", false);
        EvalResult result = engine.evaluate(vipRule, RuleContext.of(incompleteUser));
        System.out.printf("Rule [%s]  result: %s%n%n", vipRule.getName(), label(result));
    }

    private static void runMultiRules() {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

        for (Map<String, Object> user : users) {
            String userName = (String) user.get("name");
            System.out.println("┌── User: " + userName);

            RuleContext ruleContext = RuleContext.of(user).setEnableReports(true);

            for (CeleroRule rule : rules) {
                EvalResult result = engine.evaluate(rule, ruleContext);
                Report report = ruleContext.getReports().get(rule);
                System.out.printf("│  %s [%s] %s%n", label(result), rule.getId(), rule.getName());
                if (result.isTrue()) {
                    printConditions("matched  ", report);
                } else if (result.isFalse()) {
                    printConditions("unmatched", report);
                } else {
                    printConditions("absent   ", report);
                }
            }

            System.out.println("└──────────────────────────────────────");
            System.out.println();
        }
    }

    static void main(String[] args) {
        runSingleRule();
        runMultiRules();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static String label(EvalResult result) {
        if (result.isTrue())          return "TRUE         ";
        if (result.isFalse())         return "FALSE        ";
        return                               "INDETERMINATE";
    }

    private static void printConditions(String tag, Report report) {
        if (report == null) return;
        for (Route route : report.getRoutes()) {
            Set<Route.Item> items = switch (tag.trim()) {
                case "matched"   -> route.getMatched();
                case "unmatched" -> route.getUnmatched();
                case "absent"    -> route.getAbsent();
                default          -> Set.of();
            };
            items.stream()
                    .filter(item -> item.getConditionId() != null)
                    .forEach(item -> System.out.printf("│      %s: %s (%s)%n", tag, item.getConditionName(), item.getConditionId()));
        }
    }

    private static List<CeleroRule> loadRulesFromClasspath(String path) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = AdvancedRuleEngine.class.getClassLoader().getResourceAsStream(path);
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
}
