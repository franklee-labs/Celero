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
 * Demonstrates ConditionListener and RuleListener with explicit priority ordering,
 * and how listeners share state via RuleContext.setAttribute / getAttribute.
 *
 * Context attribute flow:
 *   "metrics.total" / "metrics.matched"  written by MetricsConditionListener(order=1)
 *                                         read    by DebugConditionListener(order=10)
 *   "failed.conditions"                  written by AlertConditionListener(order=20)
 *                                         read    by AuditRuleListener(order=1)
 *   "matched.rules"                      written by RewardRuleListener(order=5)
 *                                         read    by main() after evaluation
 *
 * Listeners are registered out of order intentionally — the engine sorts them by order().
 */
class SimpleRuleEngineListeners {

    private static final String ATTR_METRICS_TOTAL   = "metrics.total";
    private static final String ATTR_METRICS_MATCHED = "metrics.matched";
    private static final String ATTR_FAILED_CONDITIONS = "failed.conditions";
    private static final String ATTR_MATCHED_RULES   = "matched.rules";

    // ── Condition listeners ───────────────────────────────────────────────

    // order=1: increments running counters in context — fires first among condition listeners
    static class MetricsConditionListener implements ConditionListener {
        @Override
        public void onResult(ConditionEvent event) {
            RuleContext ctx = event.getContext();
            int total   = getInt(ctx, ATTR_METRICS_TOTAL)   + 1;
            int matched = getInt(ctx, ATTR_METRICS_MATCHED) + (event.isMatched() ? 1 : 0);
            ctx.setAttribute(ATTR_METRICS_TOTAL,   total);
            ctx.setAttribute(ATTR_METRICS_MATCHED, matched);
            System.out.printf("  [CONDITION][order=1 ][METRICS] setAttribute: total=%d  matched=%d%n", total, matched);
        }

        @Override
        public int order() { return 1; }
    }

    // order=10: reads the counters written by MetricsConditionListener — fires second
    static class DebugConditionListener implements ConditionListener {
        @Override
        public void onResult(ConditionEvent event) {
            RuleContext ctx = event.getContext();
            int total   = getInt(ctx, ATTR_METRICS_TOTAL);
            int matched = getInt(ctx, ATTR_METRICS_MATCHED);
            System.out.printf("  [CONDITION][order=10][DEBUG] getAttribute: total=%d matched=%d  |  rule=%s  condition=%s  result=%b%n",
                    total, matched, event.getRuleName(), event.getConditionName(), event.isMatched());
        }

        @Override
        public int order() { return 10; }
    }

    // order=20: appends failed condition names to context list — fires last among condition listeners
    static class AlertConditionListener implements ConditionListener {
        @Override
        @SuppressWarnings("unchecked")
        public void onResult(ConditionEvent event) {
            if (!event.isMatched()) {
                RuleContext ctx = event.getContext();
                List<String> failed = (List<String>) ctx.getAttribute(ATTR_FAILED_CONDITIONS);
                if (failed == null) {
                    failed = new ArrayList<>();
                }
                failed.add(event.getConditionName());
                ctx.setAttribute(ATTR_FAILED_CONDITIONS, failed);
                System.out.printf("  [CONDITION][order=20][ALERT] setAttribute: failed.conditions=%s%n", failed);
            }
        }

        @Override
        public int order() { return 20; }
    }

    // ── Rule listeners ────────────────────────────────────────────────────

    // order=1: reads and clears "failed.conditions" accumulated by AlertConditionListener — fires first among rule listeners
    static class AuditRuleListener implements RuleListener {
        @Override
        @SuppressWarnings("unchecked")
        public void onRuleResult(RuleEvent event) {
            RuleContext ctx = event.getContext();
            List<String> failed = (List<String>) ctx.getAttribute(ATTR_FAILED_CONDITIONS);
            System.out.printf("  [RULE][order=1 ][AUDIT] getAttribute: failed.conditions=%s  |  rule=%s  result=%s%n",
                    failed == null ? "[]" : failed, event.getRuleName(), event.isMatched() ? "PASS" : "FAIL");
            ctx.setAttribute(ATTR_FAILED_CONDITIONS, new ArrayList<String>());
        }

        @Override
        public int order() { return 1; }
    }

    // order=5: writes matched rule IDs to context for post-evaluation summary — fires second among rule listeners
    static class RewardRuleListener implements RuleListener {
        @Override
        @SuppressWarnings("unchecked")
        public void onRuleResult(RuleEvent event) {
            if (event.isMatched()) {
                RuleContext ctx = event.getContext();
                List<String> matchedRules = (List<String>) ctx.getAttribute(ATTR_MATCHED_RULES);
                if (matchedRules == null) {
                    matchedRules = new ArrayList<>();
                }
                matchedRules.add(event.getRuleId());
                ctx.setAttribute(ATTR_MATCHED_RULES, matchedRules);
                System.out.printf("  [RULE][order=5 ][REWARD] setAttribute: matched.rules=%s%n", matchedRules);
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

    @SuppressWarnings("unchecked")
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

        // Read final summary written by RewardRuleListener
        System.out.println("─".repeat(60));
        List<String> matchedRules = (List<String>) ctx.getAttribute(ATTR_MATCHED_RULES);
        System.out.println("[SUMMARY] getAttribute: matched.rules=" + matchedRules);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static int getInt(RuleContext ctx, String key) {
        Object val = ctx.getAttribute(key);
        return val instanceof Integer i ? i : 0;
    }

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
