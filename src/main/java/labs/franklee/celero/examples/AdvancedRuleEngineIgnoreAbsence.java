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
 * Demonstrates how ignoreAbsence interacts with INDETERMINATE inside the same rule.
 *
 * Rules used (coupon-rules-ignore-absence.json) mix per-condition ignoreAbsence settings:
 *
 *   high-value-user:
 *     c-spend  ignoreAbsence=true  — missing totalSpend → FALSE  (collapse)
 *     c-level  ignoreAbsence=false — missing memberLevel → INDETERMINATE
 *
 *   vip-access:
 *     c-age      ignoreAbsence=true  — missing age      → FALSE  (collapse)
 *     c-verified ignoreAbsence=false — missing verified → INDETERMINATE
 *
 * Decision path inside Condition.execute() when a field is absent:
 *   ignoreAbsence=true  → FALSE          (field treated as not matching, result is certain)
 *   ignoreAbsence=false → INDETERMINATE  (only when AdvancedCeleroEngine / enableMissing=true)
 *
 * Test cases:
 *   Frank1 — missing totalSpend  (ignoreAbsence=true)  → high-value-user = FALSE
 *   Frank2 — missing memberLevel (ignoreAbsence=false) → high-value-user = INDETERMINATE
 *   Grace  — missing age + verified
 *               age      ignoreAbsence=true  → c-age=FALSE  (OR first branch fails)
 *               verified ignoreAbsence=false → c-verified=INDETERMINATE (OR second branch)
 *               OR(FALSE, INDETERMINATE)     → INDETERMINATE
 *               vip-access = INDETERMINATE
 */
class AdvancedRuleEngineIgnoreAbsence {

    private static final List<CeleroRule> rules;
    private static final List<Map<String, Object>> users;

    static {
        try {
            rules = loadRulesFromClasspath("examples/coupon-rules-ignore-absence.json");
            System.out.println("Loaded rules: " + rules.size());
            System.out.println("  high-value-user: c-spend(ignoreAbsence=true)  c-level(ignoreAbsence=false)");
            System.out.println("  vip-access:      c-age(ignoreAbsence=true)    c-verified(ignoreAbsence=false)");
            System.out.println();

            users = List.of(
                    // all fields present — same result as AdvancedRuleEngine
                    Map.of("name", "Alice", "totalSpend", 800L, "memberLevel", "gold",
                            "status", "active", "age", 25L, "verified", true, "banned", false),
                    // missing totalSpend: c-spend ignoreAbsence=true → FALSE (short-circuits AND)
                    // expected: high-value-user = FALSE
                    Map.of("name", "Frank1 (missing totalSpend)", "memberLevel", "gold",
                            "status", "active", "age", 28L, "verified", true, "banned", false),
                    // missing memberLevel: c-level ignoreAbsence=false → INDETERMINATE
                    // expected: high-value-user = INDETERMINATE
                    Map.of("name", "Frank2 (missing memberLevel)", "totalSpend", 800L,
                            "status", "active", "age", 28L, "verified", true, "banned", false),
                    // missing age + verified:
                    //   c-age ignoreAbsence=true → FALSE, c-verified ignoreAbsence=false → INDETERMINATE
                    //   OR(FALSE, INDETERMINATE) → INDETERMINATE
                    // expected: vip-access = INDETERMINATE
                    Map.of("name", "Grace (missing age + verified)",
                            "totalSpend", 100L, "memberLevel", "normal",
                            "status", "active", "banned", false)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

        for (Map<String, Object> user : users) {
            String userName = (String) user.get("name");
            System.out.println("┌── User: " + userName);

            RuleContext ruleContext = RuleContext.of(user).setEnableReports(true);

            for (CeleroRule rule : rules) {
                EvalResult result = engine.evaluate(rule, ruleContext);
                Report report = ruleContext.getReports().get(rule);
                System.out.printf("│  %s [%s]%n", label(result), rule.getId());
                if (result.isTrue()) {
                    printConditions("matched      ", report);
                } else if (result.isFalse()) {
                    printConditions("unmatched    ", report);
                } else {
                    printConditions("indeterminate", report);
                }
            }

            System.out.println("└──────────────────────────────────────");
            System.out.println();
        }
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
                case "matched"       -> route.getMatched();
                case "unmatched"     -> route.getUnmatched();
                case "indeterminate" -> route.getAbsent();
                default              -> Set.of();
            };
            items.stream()
                    .filter(item -> item.getConditionId() != null)
                    .forEach(item -> System.out.printf("│      %s: %s (%s)%n", tag, item.getConditionName(), item.getConditionId()));
        }
    }

    private static List<CeleroRule> loadRulesFromClasspath(String path) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = AdvancedRuleEngineIgnoreAbsence.class.getClassLoader().getResourceAsStream(path);
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
