package labs.franklee.celero.examples;

import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.engine.DefaultCeleroEngine;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.listener.ConditionEvent;
import labs.franklee.celero.listener.ConditionListener;
import labs.franklee.celero.rules.RuleBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demonstrates condition result caching in a rule with the shape A AND (B OR C).
 *
 * The engine expands A AND (B OR C) into two paths:
 *   path 1:  A AND B
 *   path 2:  A AND C
 *
 * When A is cacheable and the rule has caching enabled:
 *   - path 1 evaluates A (result cached), then B (false) → path fails
 *   - path 2 reads A from cache (not re-executed), evaluates C (true) → path succeeds
 *
 * Condition A uses REGEXP (an intentionally expensive match) to illustrate why
 * caching is valuable.  B and C are simple EQ checks.
 *
 * Two runs compare behavior:
 *   runWithCache()    → A executed once,  B once, C once  (cache hit on path 2)
 *   runWithoutCache() → A executed twice, B once, C once  (no cache, A re-evaluated)
 */
public class SimpleRuleEngineCachedConditionExample {

    // A AND (B OR C)
    // A: email matches a pattern        — REGEXP, cacheable=true
    // B: role == "admin"                — EQ,     cacheable=true
    // C: level == "high"                — EQ,     cacheable=true
    private static final String RULE_JSON = """
            {
              "type": "relation",
              "sign": "AND",
              "children": [
                {
                  "id": "cond-a",
                  "name": "A: email format",
                  "type": "condition",
                  "sign": "REGEXP",
                  "cacheable": true,
                  "properties": {
                    "field": "email",
                    "value": "^[\\\\w.+-]+@[\\\\w-]+\\\\.[a-z]{2,}$"
                  }
                },
                {
                  "type": "relation",
                  "sign": "OR",
                  "children": [
                    {
                      "id": "cond-b",
                      "name": "B: role check",
                      "type": "condition",
                      "sign": "EQ",
                      "cacheable": true,
                      "properties": {
                        "field": "role",
                        "value": "admin",
                        "valueType": "String"
                      }
                    },
                    {
                      "id": "cond-c",
                      "name": "C: level check",
                      "type": "condition",
                      "sign": "EQ",
                      "cacheable": true,
                      "properties": {
                        "field": "level",
                        "value": "high",
                        "valueType": "String"
                      }
                    }
                  ]
                }
              ]
            }
            """;

    // A=true (email valid), B=false (not admin), C=true (level=high)
    // Expected evaluation order:
    //   path 1 (A&B): A executes → true (cached),  B executes → false (path fails)
    //   path 2 (A&C): A from cache → true (skipped), C executes → true (path succeeds)
    private static final Map<String, Object> USER = Map.of(
            "email", "alice@example.com",
            "role",  "user",
            "level", "high"
    );

    // ── execution counter listener ────────────────────────────────────────

    static class ExecutionTracker implements ConditionListener {
        // LinkedHashMap preserves insertion order for predictable printing
        final Map<String, Integer> executionCount = new LinkedHashMap<>();

        @Override
        public void onResult(ConditionEvent event) {
            String key = event.getConditionId() + " (" + event.getConditionName() + ")";
            executionCount.merge(key, 1, Integer::sum);
        }

        void printSummary() {
            System.out.println("  Condition execution counts:");
            executionCount.forEach((id, count) ->
                    System.out.printf("    %-40s executed %d time(s)%n", id, count));
        }
    }

    // ── runs ──────────────────────────────────────────────────────────────

    private static void runWithCache() throws Throwable {
        System.out.println("── With caching (rule.cacheable=true) ──────────────────");

        CeleroRule rule = RuleBuilder.fromJson("rule-cached", RULE_JSON)
                .name("cached rule")
                .cacheable(true)   // enables condition result cache for this rule
                .build();

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        ExecutionTracker tracker = new ExecutionTracker();
        engine.addConditionListener(tracker);

        boolean result = engine.evaluate(rule, RuleContext.of(USER));
        System.out.println("  Rule matched: " + result);
        tracker.printSummary();
        System.out.println("  → A evaluated once: path 2 read the cached result instead of re-executing.");
        System.out.println();
    }

    private static void runWithoutCache() throws Throwable {
        System.out.println("── Without caching (rule.cacheable=false) ──────────────");

        CeleroRule rule = RuleBuilder.fromJson("rule-uncached", RULE_JSON)
                .name("uncached rule")
                .cacheable(false)  // condition results are never cached
                .build();

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        ExecutionTracker tracker = new ExecutionTracker();
        engine.addConditionListener(tracker);

        boolean result = engine.evaluate(rule, RuleContext.of(USER));
        System.out.println("  Rule matched: " + result);
        tracker.printSummary();
        System.out.println("  → A evaluated twice: no cache, so path 2 re-executed A from scratch.");
        System.out.println();
    }

    static void main(String[] args) throws Throwable {
        System.out.println("Rule shape:  A AND (B OR C)");
        System.out.println("Paths:       [A AND B]  [A AND C]");
        System.out.println("Input:       A=true (email valid)  B=false (not admin)  C=true (level=high)");
        System.out.println();

        runWithCache();
        runWithoutCache();
    }
}
