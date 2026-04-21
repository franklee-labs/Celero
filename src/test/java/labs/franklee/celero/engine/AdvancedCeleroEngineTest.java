package labs.franklee.celero.engine;

import labs.franklee.celero.listener.AdvancedConditionEvent;
import labs.franklee.celero.listener.AdvancedConditionListener;
import labs.franklee.celero.listener.AdvancedRuleEvent;
import labs.franklee.celero.listener.AdvancedRuleListener;
import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.RelationNode;
import labs.franklee.celero.rules.Rule;
import labs.franklee.celero.rules.RuleBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedCeleroEngineTest {

    // ---- helpers ----

    private static ConditionNode condNode(String id, String name, String key, String value) {
        ConditionNode c = new ConditionNode();
        c.setType("condition").setSign("EQ").setId(id).setName(name);
        c.setProperties(Map.of("key", key, "value", value, "valueType", "String"));
        return c;
    }

    private static ConditionNode condNode(String id, String key, String value) {
        return condNode(id, id + "-name", key, value);
    }

    private static Rule singleCondRule(String ruleId, String ruleName, String key, String value) throws Throwable {
        return RuleBuilder.create()
                .id(ruleId).name(ruleName)
                .root(condNode("cond-1", "cond-1-name", key, value))
                .build();
    }

    private static Rule andRule(String ruleId, ConditionNode... conds) throws Throwable {
        RelationNode root = new RelationNode();
        root.setSign("AND");
        root.setChildren(List.of(conds));
        return RuleBuilder.create().id(ruleId).name(ruleId).root(root).build();
    }

    private static Rule orRule(String ruleId, ConditionNode... conds) throws Throwable {
        RelationNode root = new RelationNode();
        root.setSign("OR");
        root.setChildren(List.of(conds));
        return RuleBuilder.create().id(ruleId).name(ruleId).root(root).build();
    }

    // ---- evaluate(Rule) — basic TRUE / FALSE ----

    @Test
    void evaluate_allConditionsMatch_returnsTrue() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "active"))).isTrue());
    }

    @Test
    void evaluate_conditionFails_returnsFalse() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive"))).isFalse());
    }

    // ---- evaluate(Rule) — MISS propagation ----

    @Test
    void evaluate_missingParam_singlePath_returnsMiss() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of())).isIndeterminate());
    }

    @Test
    void evaluate_conditionFalse_overridesMiss_returnsFalse() throws Throwable {
        // AND(MISS, FALSE) → path = FALSE (FALSE takes priority over MISS)
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "missing-key", "x"),   // MISS
                condNode("c2", "role", "admin")         // FALSE (role=user)
        );
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("role", "user"))).isFalse());
    }

    @Test
    void evaluate_conditionMissOnly_pathReturnsMiss() throws Throwable {
        // AND(MISS, MISS) → path = MISS
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "a", "x"),
                condNode("c2", "b", "y")
        );
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of())).isIndeterminate());
    }

    @Test
    void evaluate_missDoesNotShortCircuit_remainingConditionsEvaluated() throws Throwable {
        // AND(MISS, FALSE) — MISS must not short-circuit; FALSE must still be evaluated
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "missing-key", "x"),
                condNode("c2", "role", "admin")
        );

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("role", "user")));

        assertEquals(2, events.size());
        assertTrue(events.get(0).getResult().isIndeterminate());
        assertTrue(events.get(1).getResult().isFalse());
    }

    @Test
    void evaluate_falseShortCircuits_remainingConditionsNotEvaluated() throws Throwable {
        // AND(FALSE, ...) — FALSE must short-circuit
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        );

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive", "role", "admin")));

        assertEquals(1, events.size());
        assertEquals("c1", events.get(0).getConditionId());
    }

    // ---- multi-path (OR) — MISS propagation across paths ----

    @Test
    void evaluate_orRule_oneMiss_oneTrue_returnsTrue() throws Throwable {
        // OR(MISS, TRUE) → TRUE
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "missing-key", "x"),
                condNode("c2", "role", "admin")
        );
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("role", "admin"))).isTrue());
    }

    @Test
    void evaluate_orRule_oneFalse_oneMiss_returnsMiss() throws Throwable {
        // OR(FALSE, MISS) → MISS
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "status", "active"),
                condNode("c2", "missing-key", "x")
        );
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive"))).isIndeterminate());
    }

    @Test
    void evaluate_orRule_allFalse_returnsFalse() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        );
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive", "role", "user"))).isFalse());
    }

    // ---- evaluate(List<Rule>) — AdvancedRuleListener ----

    @Test
    void evaluate_list_ruleListener_firedForEachRule() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule r1 = singleCondRule("r1", "rule1", "status", "active");
        Rule r2 = singleCondRule("r2", "rule2", "role", "admin");

        List<AdvancedRuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        engine.evaluate(List.of(r1, r2), RuleContext.of(Map.of("status", "active", "role", "admin")));

        assertEquals(2, events.size());
        assertEquals("r1", events.get(0).getRuleId());
        assertEquals("r2", events.get(1).getRuleId());
    }

    @Test
    void evaluate_list_ruleEvent_evalResultCorrect() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule r1 = singleCondRule("r1", "rule1", "status", "active");
        Rule r2 = singleCondRule("r2", "rule2", "role", "admin");
        Rule r3 = singleCondRule("r3", "rule3", "missing-key", "x");

        List<AdvancedRuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        engine.evaluate(List.of(r1, r2, r3), RuleContext.of(Map.of("status", "active", "role", "user")));

        assertTrue(events.get(0).isMatched().isTrue());    // r1: match
        assertTrue(events.get(1).isMatched().isFalse());   // r2: no match
        assertTrue(events.get(2).isMatched().isIndeterminate()); // r3: missing param
    }

    @Test
    void evaluate_list_ruleEvent_ruleNameAndContextCorrect() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "my-rule", "status", "active");

        List<AdvancedRuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        RuleContext ctx = RuleContext.of(Map.of("status", "active"));
        engine.evaluate(List.of(rule), ctx);

        assertEquals("my-rule", events.get(0).getRuleName());
        assertSame(ctx, events.get(0).getContext());
    }

    // ---- AdvancedConditionListener ----

    @Test
    void conditionListener_firedOnEachCondition() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        );

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active", "role", "admin")));

        assertEquals(2, events.size());
    }

    @Test
    void conditionListener_event_hasCorrectIds() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        AdvancedConditionEvent e = events.get(0);
        assertEquals("r1", e.getRuleId());
        assertEquals("rule1", e.getRuleName());
        assertEquals("cond-1", e.getConditionId());
        assertEquals("cond-1-name", e.getConditionName());
    }

    @Test
    void conditionListener_event_evalResultTrue_whenMatch() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        assertTrue(events.get(0).getResult().isTrue());
    }

    @Test
    void conditionListener_event_evalResultFalse_whenNoMatch() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive")));

        assertTrue(events.get(0).getResult().isFalse());
    }

    @Test
    void conditionListener_event_evalResultMiss_whenParamMissing() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of()));

        assertTrue(events.get(0).getResult().isIndeterminate());
    }

    @Test
    void conditionListener_event_containsRuleContext() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        RuleContext ctx = RuleContext.of(Map.of("status", "active"));
        engine.evaluate(rule, ctx);

        assertSame(ctx, events.get(0).getContext());
    }

    // ---- listener ordering ----

    @Test
    void conditionListener_calledInAscendingOrder() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<Integer> callOrder = new ArrayList<>();
        engine.addConditionListener(new AdvancedConditionListener() {
            public void onResult(AdvancedConditionEvent e) { callOrder.add(order()); }
            public int order() { return 10; }
        });
        engine.addConditionListener(new AdvancedConditionListener() {
            public void onResult(AdvancedConditionEvent e) { callOrder.add(order()); }
            public int order() { return 1; }
        });

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        assertEquals(List.of(1, 10), callOrder);
    }

    @Test
    void ruleListener_calledInAscendingOrder() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        List<Integer> callOrder = new ArrayList<>();
        engine.addRuleListener(new AdvancedRuleListener() {
            public void onRuleResult(AdvancedRuleEvent e) { callOrder.add(order()); }
            public int order() { return 10; }
        });
        engine.addRuleListener(new AdvancedRuleListener() {
            public void onRuleResult(AdvancedRuleEvent e) { callOrder.add(order()); }
            public int order() { return 1; }
        });

        engine.evaluate(List.of(rule), RuleContext.of(Map.of("status", "active")));

        assertEquals(List.of(1, 10), callOrder);
    }

    // ---- exception swallowing ----

    @Test
    void conditionListener_exceptionSwallowed_evaluationContinues() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        engine.addConditionListener(e -> { throw new RuntimeException("boom"); });

        assertDoesNotThrow(() ->
                assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "active"))).isTrue())
        );
    }

    @Test
    void ruleListener_exceptionSwallowed_subsequentListenerStillCalled() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleCondRule("r1", "rule1", "status", "active");

        engine.addRuleListener(new AdvancedRuleListener() {
            public void onRuleResult(AdvancedRuleEvent e) { throw new RuntimeException("boom"); }
        });

        List<AdvancedRuleEvent> received = new ArrayList<>();
        engine.addRuleListener(new AdvancedRuleListener() {
            public void onRuleResult(AdvancedRuleEvent e) { received.add(e); }
            public int order() { return 2; }
        });

        assertDoesNotThrow(() ->
                engine.evaluate(List.of(rule), RuleContext.of(Map.of("status", "active")))
        );
        assertEquals(1, received.size());
    }
}
