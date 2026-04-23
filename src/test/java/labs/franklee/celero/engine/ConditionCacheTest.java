package labs.franklee.celero.engine;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.listener.AdvancedConditionEvent;
import labs.franklee.celero.listener.ConditionEvent;
import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.logic.base.ValueType;
import labs.franklee.celero.logic.impl.EqualCondition;
import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.RelationNode;
import labs.franklee.celero.rules.Rule;
import labs.franklee.celero.rules.RuleBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConditionCacheTest {

    // ---- helpers ----

    private static Context cacheCtx(Object... kvs) {
        var m = new java.util.HashMap<String, Object>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return Context.Builder.createBuilder(RuleContext.of(m))
                .enableConditionResultCache()
                .build();
    }

    private static Context noCacheCtx(Object... kvs) {
        var m = new java.util.HashMap<String, Object>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return Context.Builder.createBuilder(RuleContext.of(m)).build();
    }

    private static EqualCondition eq(String id, String field, String value) {
        EqualCondition c = new EqualCondition(field, value, ValueType.String, 0);
        c.setId(id);
        return c;
    }

    private static ConditionNode condNode(String id, String field, String value, boolean cacheable) {
        ConditionNode c = new ConditionNode();
        c.setType("condition").setSign("EQ").setId(id).setName(id + "-name");
        c.setProperties(Map.of("field", field, "value", value, "valueType", "String"));
        c.setCacheable(cacheable);
        return c;
    }

    // ---- Context cache API ----

    @Test
    void context_setAndGet_returnsStoredResult() {
        Context ctx = cacheCtx();
        ctx.setConditionEvalResult("key-1", EvalResult.TRUE);
        assertEquals(EvalResult.TRUE, ctx.getConditionEvalResult("key-1"));
    }

    @Test
    void context_getUnknownKey_returnsNull() {
        assertNull(cacheCtx().getConditionEvalResult("unknown"));
    }

    @Test
    void context_put_secondWriteOverrides() {
        Context ctx = cacheCtx();
        ctx.setConditionEvalResult("key-1", EvalResult.TRUE);
        ctx.setConditionEvalResult("key-1", EvalResult.FALSE);
        assertEquals(EvalResult.FALSE, ctx.getConditionEvalResult("key-1"));
    }

    @Test
    void context_multipleKeys_storedIndependently() {
        Context ctx = cacheCtx();
        ctx.setConditionEvalResult("a", EvalResult.TRUE);
        ctx.setConditionEvalResult("b", EvalResult.FALSE);
        assertEquals(EvalResult.TRUE, ctx.getConditionEvalResult("a"));
        assertEquals(EvalResult.FALSE, ctx.getConditionEvalResult("b"));
    }

    @Test
    void context_cacheEnabled_flagIsTrue() {
        assertTrue(cacheCtx().isEnableConditionResultCache());
    }

    @Test
    void context_cacheDisabled_flagIsFalse() {
        assertFalse(noCacheCtx().isEnableConditionResultCache());
    }

    // ---- Condition.execute() cache write ----

    @Test
    void execute_cacheEnabledAndCacheable_storesResult() throws Exception {
        EqualCondition cond = eq("cond-x", "status", "active");
        cond.setCacheable(true);
        cond.compile();

        Context ctx = cacheCtx("status", "active");
        cond.execute(ctx);

        assertNotNull(ctx.getConditionEvalResult(cond.getInternalUniqueId()));
    }

    @Test
    void execute_cacheEnabled_resultTrue_storedAsTrue() throws Exception {
        EqualCondition cond = eq("cond-t", "flag", "on");
        cond.setCacheable(true);
        cond.compile();

        Context ctx = cacheCtx("flag", "on");
        EvalResult result = cond.execute(ctx);

        assertTrue(result.isTrue());
        assertEquals(EvalResult.TRUE, ctx.getConditionEvalResult(cond.getInternalUniqueId()));
    }

    @Test
    void execute_cacheEnabled_resultFalse_storedAsFalse() throws Exception {
        EqualCondition cond = eq("cond-f", "flag", "on");
        cond.setCacheable(true);
        cond.compile();

        Context ctx = cacheCtx("flag", "off");
        EvalResult result = cond.execute(ctx);

        assertTrue(result.isFalse());
        assertEquals(EvalResult.FALSE, ctx.getConditionEvalResult(cond.getInternalUniqueId()));
    }

    @Test
    void execute_cacheDisabled_nothingStored() throws Exception {
        EqualCondition cond = eq("cond-d", "status", "active");
        cond.setCacheable(true);
        cond.compile();

        Context ctx = noCacheCtx("status", "active");
        cond.execute(ctx);

        assertNull(ctx.getConditionEvalResult(cond.getInternalUniqueId()));
    }

    @Test
    void execute_conditionNotCacheable_nothingStored() throws Exception {
        EqualCondition cond = eq("cond-nc", "status", "active");
        cond.setCacheable(false);
        cond.compile();

        Context ctx = cacheCtx("status", "active");
        cond.execute(ctx);

        assertNull(ctx.getConditionEvalResult(cond.getInternalUniqueId()));
    }

    // ---- Rule-level cacheable ----

    @Test
    void rule_cacheable_true_isCacheableReturnsTrue() throws Throwable {
        Rule rule = RuleBuilder.create().id("r1").name("rule1").cacheable(true)
                .root(condNode("c1", "status", "active", true)).build();
        assertTrue(rule.isCacheable());
    }

    @Test
    void rule_cacheable_false_isCacheableReturnsFalse() throws Throwable {
        Rule rule = RuleBuilder.create().id("r1").name("rule1").cacheable(false)
                .root(condNode("c1", "status", "active", true)).build();
        assertFalse(rule.isCacheable());
    }

    // ---- Engine: condition A shared across two paths via AND(A, OR(B, C)) ----
    //
    // Rule: A AND (B OR C) → resolves to path1=[A,B] and path2=[A,C]
    // A is the same ConditionNode instance, so both paths share the same internalUniqueId.
    //
    // Params: A=TRUE (role=admin), B=FALSE (no status), C=FALSE (no level)
    //
    // Cache enabled + A cacheable:
    //   path1: eval A (TRUE, cached), eval B (FALSE) → fails
    //   path2: A cache hit → skip, eval C (FALSE) → fails
    //   events = [A, B, C] → size 3, A fires once
    //
    // Cache disabled (or A not cacheable):
    //   path1: eval A, eval B → fails
    //   path2: eval A again, eval C → fails
    //   events = [A, B, A, C] → size 4, A fires twice

    private static Rule buildAndOrRule(String ruleId, boolean ruleCacheable,
                                       boolean condACacheable) throws Throwable {
        ConditionNode condA = condNode("cond-a", "role", "admin", condACacheable);
        ConditionNode condB = condNode("cond-b", "status", "active", false);
        ConditionNode condC = condNode("cond-c", "level", "high", false);

        RelationNode orNode = new RelationNode();
        orNode.setSign("OR");
        orNode.setChildren(List.of(condB, condC));

        RelationNode andNode = new RelationNode();
        andNode.setSign("AND");
        andNode.setChildren(List.of(condA, orNode));

        return RuleBuilder.create().id(ruleId).name(ruleId)
                .cacheable(ruleCacheable).root(andNode).build();
    }

    private static Rule buildAndOrRule2(String ruleId, boolean ruleCacheable,
                                       boolean condACacheable) throws Throwable {
        // (B OR C) AND A
        ConditionNode condA = condNode("cond-a", "role", "admin", condACacheable);
        ConditionNode condB = condNode("cond-b", "status", "active", false);
        ConditionNode condC = condNode("cond-c", "level", "high", false);

        RelationNode orNode = new RelationNode();
        orNode.setSign("OR");
        orNode.setChildren(List.of(condB, condC));

        RelationNode andNode = new RelationNode();
        andNode.setSign("AND");
        andNode.setChildren(List.of(orNode, condA));


        return RuleBuilder.create().id(ruleId).name(ruleId)
                .cacheable(ruleCacheable).root(andNode).build();
    }

    private static Rule buildAndOrRule3(String ruleId, boolean ruleCacheable,
                                        boolean condACacheable) throws Throwable {
        // A AND (B OR C)
        ConditionNode condA = condNode("cond-a", "role", "admin", condACacheable);
        ConditionNode condB = condNode("cond-b", "status", "active", false);
        ConditionNode condC = condNode("cond-c", "level", "high", false);

        RelationNode orNode = new RelationNode();
        orNode.setSign("OR");
        orNode.setChildren(List.of(condB, condC));

        RelationNode andNode = new RelationNode();
        andNode.setSign("AND");
        andNode.setChildren(List.of(condA, orNode));


        return RuleBuilder.create().id(ruleId).name(ruleId)
                .cacheable(ruleCacheable).root(andNode).build();
    }



    @Test
    void engine_cacheEnabled_condACacheable_firesThreeTimes_AOnlyOnce() throws Throwable {
        Rule rule = buildAndOrRule("r-cache-on", true, true);

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        boolean result = engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));

        assertFalse(result);
        assertEquals(3, events.size(), "expected [A, B, C] — A cached after path1, skipped on path2");
        assertEquals(1, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire exactly once");
    }

    @Test
    void engine_cacheEnabled_condACacheable_falseValue_firesThreeTimes_AOnlyOnce() throws Throwable {
        Rule rule = buildAndOrRule2("r-cache-on", true, true);

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        boolean result = engine.evaluate(rule, RuleContext.of(Map.of("role", "user", "status", "active", "level", "high")));

        assertFalse(result);
        assertEquals(3, events.size(), "expected [A, B, C] — A cached after path1, skipped on path2");
        assertEquals(1, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire exactly once");
    }

    @Test
    void engine_cacheDisabled_firesFourTimes_ATwice() throws Throwable {
        Rule rule = buildAndOrRule("r-cache-off", false, true);

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        boolean result = engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));

        assertFalse(result);
        assertEquals(4, events.size(), "expected [A, B, A, C] — no cache, A re-evaluated on path2");
        assertEquals(2, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire twice");
    }

    @Test
    void engine_cacheEnabled_condANotCacheable_firesFourTimes_ATwice() throws Throwable {
        Rule rule = buildAndOrRule("r-cache-on-nc", true, false);

        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        List<ConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        boolean result = engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));

        assertFalse(result);
        assertEquals(4, events.size(), "A not cacheable → no cache write → re-evaluated on path2");
        assertEquals(2, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire twice — condition-level cache disabled");
    }

    @Test
    void advancedEngine_cacheEnabled_condACacheable_firesThreeTimes_AOnlyOnce() throws Throwable {
        Rule rule = buildAndOrRule("r-cache-on", true, true);

        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        EvalResult result = engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));

        assertTrue(result.isIndeterminate());
        assertEquals(3, events.size(), "expected [A, B, C] — A cached after path1, skipped on path2");
        assertEquals(1, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire exactly once");
    }

    @Test
    void advancedEngine_cacheEnabled_condACacheable_falseValue_firesThreeTimes_AOnlyOnce() throws Throwable {
        Rule rule = buildAndOrRule2("r-cache-on", true, true);

        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        EvalResult result = engine.evaluate(rule, RuleContext.of(Map.of("role", "user", "status", "active", "level", "high")));

        assertTrue(result.isFalse());
        assertEquals(3, events.size(), "expected [A, B, C] — A cached after path1, skipped on path2");
        assertEquals(1, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire exactly once");
    }

    @Test
    void advancedEngine_cacheDisabled_firesFourTimes_ATwice() throws Throwable {
        Rule rule = buildAndOrRule("r-cache-off", false, true);

        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        EvalResult result = engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));

        assertTrue(result.isIndeterminate());
        assertEquals(4, events.size(), "expected [A, B, A, C] — no cache, A re-evaluated on path2");
        assertEquals(2, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire twice");
    }

    @Test
    void advancedEngine_cacheEnabled_condANotCacheable_firesFourTimes_ATwice() throws Throwable {
        Rule rule = buildAndOrRule("r-cache-on-nc", true, false);

        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        EvalResult result = engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));

        assertTrue(result.isIndeterminate());
        assertEquals(4, events.size(), "A not cacheable → no cache write → re-evaluated on path2");
        assertEquals(2, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire twice — condition-level cache disabled");
    }

    @Test
    void advancedEngine_cacheEnabled_condACacheable_absent_firesThreeTimes_AOnce() throws Throwable {
        Rule rule = buildAndOrRule3("r-cache-on-nc", true, true);

        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        List<AdvancedConditionEvent> events = new ArrayList<>();
        engine.addConditionListener(events::add);

        EvalResult result = engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive", "level", "medium")));

        assertTrue(result.isFalse());
        assertEquals(3, events.size(), "A not cacheable → no cache write → re-evaluated on path2");
        assertEquals(1, events.stream().filter(e -> "cond-a".equals(e.getConditionId())).count(),
                "A must fire twice — condition-level cache disabled");
    }
}
