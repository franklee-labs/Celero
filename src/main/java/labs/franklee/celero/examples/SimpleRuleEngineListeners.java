package labs.franklee.celero.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.engine.DefaultCeleroEngine;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.listener.ConditionEvent;
import labs.franklee.celero.listener.ConditionListener;
import labs.franklee.celero.listener.RuleEvent;
import labs.franklee.celero.listener.RuleListener;
import labs.franklee.celero.rules.RuleBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates ConditionListener and RuleListener with explicit priority ordering.
 *
 * Listeners are called in ascending order() value — lower number fires first.
 * This example registers listeners out of order to show the engine sorts them correctly.
 *
 * Rule file: src/main/resources/examples/coupon-rules.json
 */
class SimpleRuleEngineListeners {

    // ── Condition listeners ───────────────────────────────────────────────

    // order=10: detailed debug log — fires second
    static class DebugConditionListener implements ConditionListener {
        @Override
        public void onResult(ConditionEvent event) {
            System.out.printf("  [CONDITION][order=10][DEBUG] rule=%s  condition=%s  matched=%b%n",
                    event.getRuleName(), event.getConditionName(), event.isMatched());
        }

        @Override
        public int order() { return 10; }
    }

    // order=1: metrics counter — fires first
    static class MetricsConditionListener implements ConditionListener {
        private int total = 0;
        private int matched = 0;

        @Override
        public void onResult(ConditionEvent event) {
            total++;
            if (event.isMatched()) matched++;
            System.out.printf("  [CONDITION][order=1 ][METRICS] conditions evaluated: %d  matched: %d%n", total, matched);
        }

        @Override
        public int order() { return 1; }
    }

    // order=20: alert on failed condition — fires last
    static class AlertConditionListener implements ConditionListener {
        @Override
        public void onResult(ConditionEvent event) {
            if (!event.isMatched()) {
                System.out.printf("  [CONDITION][order=20][ALERT] condition NOT met — rule=%s  condition=%s%n",
                        event.getRuleName(), event.getConditionName());
            }
        }

        @Override
        public int order() { return 20; }
    }

    // ── Rule listeners ────────────────────────────────────────────────────

    // order=1: audit log — fires first
    static class AuditRuleListener implements RuleListener {
        @Override
        public void onRuleResult(RuleEvent event) {
            System.out.printf("  [RULE][order=1 ][AUDIT] rule=%s  result=%s%n",
                    event.getRuleName(), event.isMatched() ? "PASS" : "FAIL");
        }

        @Override
        public int order() { return 1; }
    }

    // order=5: business action on match — fires second
    static class RewardRuleListener implements RuleListener {
        @Override
        public void onRuleResult(RuleEvent event) {
            if (event.isMatched()) {
                System.out.printf("  [RULE][order=5 ][REWARD] issuing coupon for rule [%s]%n", event.getRuleName());
            }
        }

        @Override
        public int order() { return 5; }
    }

    // ── Setup & run ───────────────────────────────────────────────────────

    private static final List<CeleroRule> rules;
    static {
        try {
            rules = loadRulesFromClasspath("examples/coupon-rules.json");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();

        // Register listeners out of order intentionally — engine will sort by order()
        engine.addConditionListener(new AlertConditionListener());   // order=20
        engine.addConditionListener(new DebugConditionListener());   // order=10
        engine.addConditionListener(new MetricsConditionListener()); // order=1

        engine.addRuleListener(new RewardRuleListener());            // order=5
        engine.addRuleListener(new AuditRuleListener());             // order=1

        Map<String, Object> user = Map.of(
                "name", "Alice",
                "totalSpend", 800L, "memberLevel", "gold",
                "registeredDays", 365L, "orderCount", 20L,
                "status", "active", "age", 25L, "verified", true, "banned", false
        );

        System.out.println("Evaluating user: " + user.get("name"));
        System.out.println("─".repeat(60));

        RuleContext ctx = RuleContext.of(user);
        engine.evaluate(rules, ctx);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static List<CeleroRule> loadRulesFromClasspath(String path) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SimpleRuleEngineListeners.class.getClassLoader().getResourceAsStream(path);
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
