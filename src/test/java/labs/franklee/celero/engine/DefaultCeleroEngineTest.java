package labs.franklee.celero.engine;

import labs.franklee.celero.listener.ConditionEvent;
import labs.franklee.celero.listener.ConditionListener;
import labs.franklee.celero.listener.RuleEvent;
import labs.franklee.celero.listener.RuleListener;
import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.RelationNode;
import labs.franklee.celero.rules.RuleBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCeleroEngineTest {

    // ---- helpers ----

    private static ConditionNode condNode(String id, String name, String field, String value) {
        ConditionNode c = new ConditionNode();
        c.setType("condition").setSign("EQ").setId(id).setName(name);
        c.setProperties(Map.of("field", field, "value", value, "valueType", "String"));
        return c;
    }

    private static ConditionNode condNode(String id, String field, String value) {
        return condNode(id, id + "-name", field, value);
    }

    private static CeleroRule singleCondRule(String ruleId, String ruleName, String field, String value) throws Throwable {
        return RuleBuilder.create()
                .id(ruleId).name(ruleName)
                .root(condNode("cond-1", "cond-1-name", field, value))
                .build();
    }

    // ---- evaluate(Rule, RuleContext) ----

    @Test
    void evaluate_matchingParams_returnsTrue() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");
        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "active"))));
    }

    @Test
    void evaluate_nonMatchingParams_returnsFalse() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");
        assertFalse(engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive"))));
    }

    @Test
    void evaluate_missingParam_returnsFalse() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");
        assertFalse(engine.evaluate(rule, RuleContext.of(Map.of())));
    }

    // ---- multi-condition AND (early exit) ----

    @Test
    void evaluate_andRule_allMatch_returnsTrue() throws Throwable {
        RelationNode root = new RelationNode();
        root.setSign("AND");
        root.setChildren(List.of(
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        ));
        CeleroRule rule = RuleBuilder.create().id("r1").name("rule1").root(root).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();

        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "active", "role", "admin"))));
    }

    @Test
    void evaluate_andRule_firstCondFails_shortCircuits() throws Throwable {
        RelationNode root = new RelationNode();
        root.setSign("AND");
        root.setChildren(List.of(
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        ));
        CeleroRule rule = RuleBuilder.create().id("r1").name("rule1").root(root).build();

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive", "role", "admin")));

        assertEquals(1, events.size());
        assertEquals("c1", events.get(0).getConditionId());
    }

    // ---- multi-path OR ----

    @Test
    void evaluate_orRule_firstPathFails_secondMatches_returnsTrue() throws Throwable {
        RelationNode or = new RelationNode();
        or.setSign("OR");
        or.setChildren(List.of(
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        ));
        CeleroRule rule = RuleBuilder.create().id("r1").name("rule1").root(or).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();

        assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("role", "admin"))));
    }

    @Test
    void evaluate_orRule_allPathsFail_returnsFalse() throws Throwable {
        RelationNode or = new RelationNode();
        or.setSign("OR");
        or.setChildren(List.of(
                condNode("c1", "status", "active"),
                condNode("c2", "role", "admin")
        ));
        CeleroRule rule = RuleBuilder.create().id("r1").name("rule1").root(or).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();

        assertFalse(engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive", "role", "user"))));
    }

    // ---- evaluate(List<Rule>, RuleContext) — RuleListener ----

    @Test
    void evaluate_list_ruleListener_firedForEachRule() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule r1 = singleCondRule("r1", "rule1", "status", "active");
        CeleroRule r2 = singleCondRule("r2", "rule2", "role", "admin");

        List<RuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        engine.evaluate(List.of(r1, r2), RuleContext.of(Map.of("status", "active", "role", "admin")));

        assertEquals(2, events.size());
        assertEquals("r1", events.get(0).getRuleId());
        assertEquals("r2", events.get(1).getRuleId());
    }

    @Test
    void evaluate_list_ruleEvent_matchedFlagCorrect() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule r1 = singleCondRule("r1", "rule1", "status", "active");
        CeleroRule r2 = singleCondRule("r2", "rule2", "role", "admin");

        List<RuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        engine.evaluate(List.of(r1, r2), RuleContext.of(Map.of("status", "active")));

        assertTrue(events.get(0).isMatched());
        assertFalse(events.get(1).isMatched());
    }

    @Test
    void evaluate_list_ruleEvent_ruleNameCorrect() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "my-rule", "status", "active");

        List<RuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        engine.evaluate(List.of(rule), RuleContext.of(Map.of("status", "active")));

        assertEquals("my-rule", events.get(0).getRuleName());
    }

    @Test
    void evaluate_list_ruleEvent_containsRuleContext() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<RuleEvent> events = new ArrayList<>();
        engine.addRuleListener(events::add);

        RuleContext ctx = RuleContext.of(Map.of("status", "active"));
        engine.evaluate(List.of(rule), ctx);

        assertSame(ctx, events.get(0).getContext());
    }

    // ---- ConditionListener ----

    @Test
    void conditionListener_firedOnConditionExecution() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        assertEquals(1, events.size());
    }

    @Test
    void conditionListener_event_hasCorrectRuleAndConditionIds() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        ConditionEvent e = events.get(0);
        assertEquals("r1", e.getRuleId());
        assertEquals("rule1", e.getRuleName());
        assertEquals("cond-1", e.getConditionId());
        assertEquals("cond-1-name", e.getConditionName());
    }

    @Test
    void conditionListener_event_matchedTrue_whenConditionPasses() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        assertTrue(events.get(0).isMatched());
    }

    @Test
    void conditionListener_event_matchedFalse_whenConditionFails() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive")));

        assertFalse(events.get(0).isMatched());
    }

    @Test
    void conditionListener_event_containsRuleContext() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        RuleContext ctx = RuleContext.of(Map.of("status", "active"));
        engine.evaluate(rule, ctx);

        assertSame(ctx, events.get(0).getContext());
    }

    // ---- listener ordering ----

    @Test
    void conditionListener_calledInAscendingOrder() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<Integer> callOrder = new ArrayList<>();
        engine.addConditionListener(new ConditionListener() {
            public void onResult(ConditionEvent e) { callOrder.add(order()); }
            public int order() { return 10; }
        });
        engine.addConditionListener(new ConditionListener() {
            public void onResult(ConditionEvent e) { callOrder.add(order()); }
            public int order() { return 1; }
        });

        engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));

        assertEquals(List.of(1, 10), callOrder);
    }

    @Test
    void ruleListener_calledInAscendingOrder() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        List<Integer> callOrder = new ArrayList<>();
        engine.addRuleListener(new RuleListener() {
            public void onRuleResult(RuleEvent e) { callOrder.add(order()); }
            public int order() { return 10; }
        });
        engine.addRuleListener(new RuleListener() {
            public void onRuleResult(RuleEvent e) { callOrder.add(order()); }
            public int order() { return 1; }
        });

        engine.evaluate(List.of(rule), RuleContext.of(Map.of("status", "active")));

        assertEquals(List.of(1, 10), callOrder);
    }

    // ---- exception swallowing ----

    @Test
    void conditionListener_exceptionSwallowed_evaluationContinues() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        engine.addConditionListener(e -> { throw new RuntimeException("boom"); });

        assertDoesNotThrow(() ->
                assertTrue(engine.evaluate(rule, RuleContext.of(Map.of("status", "active"))))
        );
    }

    @Test
    void ruleListener_exceptionSwallowed_subsequentListenerStillCalled() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleCondRule("r1", "rule1", "status", "active");

        engine.addRuleListener(new RuleListener() {
            public void onRuleResult(RuleEvent e) { throw new RuntimeException("boom"); }
            public int order() { return 1; }
        });

        List<RuleEvent> received = new ArrayList<>();
        engine.addRuleListener(new RuleListener() {
            public void onRuleResult(RuleEvent e) { received.add(e); }
            public int order() { return 2; }
        });

        assertDoesNotThrow(() ->
                engine.evaluate(List.of(rule), RuleContext.of(Map.of("status", "active")))
        );
        assertEquals(1, received.size());
    }
}
