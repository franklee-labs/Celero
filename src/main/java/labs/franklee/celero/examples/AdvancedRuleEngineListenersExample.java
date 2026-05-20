package labs.franklee.celero.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import labs.franklee.celero.engine.AdvancedCeleroEngine;
import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.listener.AdvancedConditionEvent;
import labs.franklee.celero.listener.AdvancedConditionListener;
import labs.franklee.celero.listener.AdvancedRuleEvent;
import labs.franklee.celero.listener.AdvancedRuleListener;
import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.rules.RuleBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates AdvancedConditionListener and AdvancedRuleListener with priority ordering,
 * and how listeners share state via RuleContext.setAttribute / getAttribute.
 *
 * Key difference from SimpleRuleEngineListeners: condition events carry EvalResult
 * (TRUE / FALSE / INDETERMINATE) instead of a plain boolean, allowing listeners to
 * react specifically to missing-field cases.
 *
 * Context attribute flow:
 *   "metrics.total" / "metrics.true" / "metrics.false" / "metrics.indeterminate"
 *                                    written by MetricsConditionListener(order=1)
 *                                    read    by DebugConditionListener(order=10)
 *   "indeterminate.conditions"       written by IndeterminateConditionListener(order=20)
 *                                    read    by AuditRuleListener(order=1)
 *   "matched.rules"                  written by RewardRuleListener(order=5)
 *                                    read    by main() after evaluation
 *
 * Listeners are registered out of order intentionally — engine sorts by order().
 */
class AdvancedRuleEngineListenersExample {

    private static final String ATTR_METRICS_TOTAL           = "metrics.total";
    private static final String ATTR_METRICS_TRUE            = "metrics.true";
    private static final String ATTR_METRICS_FALSE           = "metrics.false";
    private static final String ATTR_METRICS_INDETERMINATE   = "metrics.indeterminate";
    private static final String ATTR_INDETERMINATE_CONDITIONS = "indeterminate.conditions";
    private static final String ATTR_MATCHED_RULES           = "matched.rules";

    // ── Condition listeners ───────────────────────────────────────────────

    // order=1: increments per-state counters in context — fires first
    static class MetricsConditionListener implements AdvancedConditionListener {
        @Override
        public void onResult(AdvancedConditionEvent event) {
            RuleContext ctx = event.getContext();
            EvalResult result = event.getResult();
            int total  = getInt(ctx, ATTR_METRICS_TOTAL)          + 1;
            int trueC  = getInt(ctx, ATTR_METRICS_TRUE)           + (result.isTrue()          ? 1 : 0);
            int falseC = getInt(ctx, ATTR_METRICS_FALSE)          + (result.isFalse()         ? 1 : 0);
            int indetC = getInt(ctx, ATTR_METRICS_INDETERMINATE)  + (result.isIndeterminate() ? 1 : 0);
            ctx.setAttribute(ATTR_METRICS_TOTAL,         total);
            ctx.setAttribute(ATTR_METRICS_TRUE,          trueC);
            ctx.setAttribute(ATTR_METRICS_FALSE,         falseC);
            ctx.setAttribute(ATTR_METRICS_INDETERMINATE, indetC);
            System.out.printf("  [CONDITION][order=1 ][METRICS] setAttribute: total=%d  true=%d  false=%d  indeterminate=%d%n",
                    total, trueC, falseC, indetC);
        }

        @Override
        public int order() { return 1; }
    }

    // order=10: reads counters written by MetricsConditionListener — fires second
    static class DebugConditionListener implements AdvancedConditionListener {
        @Override
        public void onResult(AdvancedConditionEvent event) {
            RuleContext ctx = event.getContext();
            System.out.printf("  [CONDITION][order=10][DEBUG] getAttribute: true=%d false=%d indeterminate=%d  |  rule=%s  condition=%s  result=%s%n",
                    getInt(ctx, ATTR_METRICS_TRUE),
                    getInt(ctx, ATTR_METRICS_FALSE),
                    getInt(ctx, ATTR_METRICS_INDETERMINATE),
                    event.getRuleName(), event.getConditionName(),
                    label(event.getResult()));
        }

        @Override
        public int order() { return 10; }
    }

    // order=20: appends INDETERMINATE condition names to context list — fires last
    static class IndeterminateConditionListener implements AdvancedConditionListener {
        @Override
        @SuppressWarnings("unchecked")
        public void onResult(AdvancedConditionEvent event) {
            if (event.getResult().isIndeterminate() && event.getConditionId() != null) {
                RuleContext ctx = event.getContext();
                List<String> list = (List<String>) ctx.getAttribute(ATTR_INDETERMINATE_CONDITIONS);
                if (list == null) list = new ArrayList<>();
                list.add(event.getConditionName());
                ctx.setAttribute(ATTR_INDETERMINATE_CONDITIONS, list);
                System.out.printf("  [CONDITION][order=20][INDET ] setAttribute: indeterminate.conditions=%s%n", list);
            }
        }

        @Override
        public int order() { return 20; }
    }

    // ── Rule listeners ────────────────────────────────────────────────────

    // order=1: reads and clears "indeterminate.conditions" accumulated this rule cycle — fires first
    static class AuditRuleListener implements AdvancedRuleListener {
        @Override
        @SuppressWarnings("unchecked")
        public void onRuleResult(AdvancedRuleEvent event) {
            RuleContext ctx = event.getContext();
            List<String> indet = (List<String>) ctx.getAttribute(ATTR_INDETERMINATE_CONDITIONS);
            System.out.printf("  [RULE][order=1 ][AUDIT] getAttribute: indeterminate.conditions=%s  |  rule=%s  result=%s%n",
                    indet == null ? "[]" : indet, event.getRuleName(), label(event.isMatched()));
            ctx.setAttribute(ATTR_INDETERMINATE_CONDITIONS, new ArrayList<String>());
        }

        @Override
        public int order() { return 1; }
    }

    // order=5: writes matched rule IDs to context for post-evaluation summary — fires second
    static class RewardRuleListener implements AdvancedRuleListener {
        @Override
        @SuppressWarnings("unchecked")
        public void onRuleResult(AdvancedRuleEvent event) {
            if (event.isMatched().isTrue()) {
                RuleContext ctx = event.getContext();
                List<String> matched = (List<String>) ctx.getAttribute(ATTR_MATCHED_RULES);
                if (matched == null) matched = new ArrayList<>();
                matched.add(event.getRuleId());
                ctx.setAttribute(ATTR_MATCHED_RULES, matched);
                System.out.printf("  [RULE][order=5 ][REWARD] setAttribute: matched.rules=%s%n", matched);
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
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

        // Register listeners out of order intentionally — engine will sort by order()
        engine.addConditionListener(new IndeterminateConditionListener()); // order=20
        engine.addConditionListener(new DebugConditionListener());         // order=10
        engine.addConditionListener(new MetricsConditionListener());       // order=1

        engine.addRuleListener(new RewardRuleListener());                  // order=5
        engine.addRuleListener(new AuditRuleListener());                   // order=1

        // Grace: missing "age" and "verified" — vip-access will be INDETERMINATE
        Map<String, Object> user = Map.of(
                "name", "Grace",
                "totalSpend", 100L, "memberLevel", "normal",
                "registeredDays", 60L, "orderCount", 3L,
                "status", "active", "banned", false
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

    private static String label(EvalResult r) {
        if (r.isTrue())          return "TRUE";
        if (r.isFalse())         return "FALSE";
        return                          "INDETERMINATE";
    }

    private static int getInt(RuleContext ctx, String key) {
        Object val = ctx.getAttribute(key);
        return val instanceof Integer i ? i : 0;
    }

    private static List<CeleroRule> loadRulesFromClasspath(String path) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = AdvancedRuleEngineListenersExample.class.getClassLoader().getResourceAsStream(path);
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
