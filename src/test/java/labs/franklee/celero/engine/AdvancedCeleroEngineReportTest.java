package labs.franklee.celero.engine;

import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.rules.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedCeleroEngineReportTest {

    // ---- helpers ----

    private static ConditionNode condNode(String id, String name, String key, String value) {
        ConditionNode c = new ConditionNode();
        c.setType("condition").setSign("EQ").setId(id).setName(name);
        c.setProperties(Map.of("key", key, "value", value, "valueType", "String"));
        return c;
    }

    private static ConditionNode condNode(String id, String name, String key, String value, boolean cacheable) {
        ConditionNode c = condNode(id, name, key, value);
        c.setCacheable(cacheable);
        return c;
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

    private static Rule singleRule(String ruleId, String key, String value) throws Throwable {
        return RuleBuilder.create()
                .id(ruleId).name(ruleId)
                .root(condNode("cond-1", "cond-1-name", key, value))
                .build();
    }

    // (B OR C) AND A, paths [B,A] and [C,A], A cacheable — triggers cached-FALSE branch
    private static Rule buildOrAndRule(String ruleId) throws Throwable {
        ConditionNode condA = condNode("cond-a", "cond-a-name", "role", "admin", true);
        ConditionNode condB = condNode("cond-b", "cond-b-name", "status", "active", false);
        ConditionNode condC = condNode("cond-c", "cond-c-name", "level", "high", false);

        RelationNode orNode = new RelationNode();
        orNode.setSign("OR");
        orNode.setChildren(List.of(condB, condC));

        RelationNode andNode = new RelationNode();
        andNode.setSign("AND");
        andNode.setChildren(List.of(orNode, condA));

        return RuleBuilder.create().id(ruleId).name(ruleId).cacheable(true).root(andNode).build();
    }

    // AND(A, OR(B,C)), paths [A,B] and [A,C], A cacheable — triggers cached-TRUE and cached-INDETERMINATE branches
    private static Rule buildAndOrRule(String ruleId) throws Throwable {
        ConditionNode condA = condNode("cond-a", "cond-a-name", "role", "admin", true);
        ConditionNode condB = condNode("cond-b", "cond-b-name", "status", "active", false);
        ConditionNode condC = condNode("cond-c", "cond-c-name", "level", "high", false);

        RelationNode orNode = new RelationNode();
        orNode.setSign("OR");
        orNode.setChildren(List.of(condB, condC));

        RelationNode andNode = new RelationNode();
        andNode.setSign("AND");
        andNode.setChildren(List.of(condA, orNode));

        return RuleBuilder.create().id(ruleId).name(ruleId).cacheable(true).root(andNode).build();
    }

    private static Route.Item item(String id, String name) {
        return new Route.Item(id, name);
    }

    // ---- Report disabled ----

    @Test
    void report_disabled_condTrue_noReportsGenerated() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "active"));
        engine.evaluate(rule, ctx);
        assertTrue(ctx.getReports().isEmpty());
    }

    @Test
    void report_disabled_condFalse_noReportsGenerated() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive"));
        engine.evaluate(rule, ctx);
        assertTrue(ctx.getReports().isEmpty());
    }

    @Test
    void report_disabled_cachedCondFalse_noReportsGenerated() throws Throwable {
        // (B OR C) AND A: A cached as FALSE after path1 — report disabled, cached-FALSE branch must NOT append
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = buildOrAndRule("r1");
        RuleContext ctx = RuleContext.of(Map.of("role", "user", "status", "active", "level", "high"));
        engine.evaluate(rule, ctx);
        assertTrue(ctx.getReports().isEmpty());
    }

    // ---- Single condition ----

    @Test
    void report_singleCond_true_matchedContainsCond() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "active")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().contains(item("cond-1", "cond-1-name")));
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getAbsent().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_singleCond_false_unmatchedContainsCond() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getUnmatched().contains(item("cond-1", "cond-1-name")));
        assertTrue(r.getAbsent().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_singleCond_missing_absentContainsCond() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of()).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getAbsent().contains(item("cond-1", "cond-1-name")));
        assertTrue(r.getSkipped().isEmpty());
    }

    // ---- AND rule ----

    @Test
    void report_andRule_allTrue_allMatched() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active", "role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertEquals(2, r.getMatched().size());
        assertTrue(r.getMatched().contains(item("c1", "c1-name")));
        assertTrue(r.getMatched().contains(item("c2", "c2-name")));
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getAbsent().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_andRule_firstFalse_unmatched_secondSkipped() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive", "role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getUnmatched().contains(item("c1", "c1-name")));
        assertTrue(r.getSkipped().contains(item("c2", "c2-name")));
        assertTrue(r.getAbsent().isEmpty());
    }

    @Test
    void report_andRule_firstTrue_secondFalse_noSkipped() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active", "role", "user")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().contains(item("c1", "c1-name")));
        assertTrue(r.getUnmatched().contains(item("c2", "c2-name")));
        assertTrue(r.getSkipped().isEmpty());
        assertTrue(r.getAbsent().isEmpty());
    }

    @Test
    void report_andRule_missFirst_doesNotShortCircuit_absentAndMatched() throws Throwable {
        // AND(MISS, TRUE): MISS must not short-circuit
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "missing-key", "x"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("role", "admin")).setEnableReports(true);
        EvalResult result = engine.evaluate(rule, ctx);

        assertTrue(result.isIndeterminate());
        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getAbsent().contains(item("c1", "c1-name")));
        assertTrue(r.getMatched().contains(item("c2", "c2-name")));
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_andRule_missFirst_thenFalse_absentAndUnmatched() throws Throwable {
        // AND(MISS, FALSE): MISS doesn't short-circuit, FALSE does
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "missing-key", "x"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("role", "user")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getAbsent().contains(item("c1", "c1-name")));
        assertTrue(r.getUnmatched().contains(item("c2", "c2-name")));
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_andRule_firstTrue_secondMiss_matchedAndAbsent() throws Throwable {
        // AND(TRUE, MISS)
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "missing-key", "x"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().contains(item("c1", "c1-name")));
        assertTrue(r.getAbsent().contains(item("c2", "c2-name")));
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_andRule_allMiss_allAbsent() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = andRule("r1",
                condNode("c1", "c1-name", "a", "x"),
                condNode("c2", "c2-name", "b", "y"));
        RuleContext ctx = RuleContext.of(Map.of()).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertEquals(2, r.getAbsent().size());
        assertTrue(r.getAbsent().contains(item("c1", "c1-name")));
        assertTrue(r.getAbsent().contains(item("c2", "c2-name")));
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    // ---- OR rule (multi-path) ----

    @Test
    void report_orRule_firstTrue_onlyOneRoute() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(1, routes.size());
        assertTrue(routes.get(0).getMatched().contains(item("c1", "c1-name")));
    }

    @Test
    void report_orRule_allFalse_twoRoutes() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive", "role", "user")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.get(0).getUnmatched().contains(item("c1", "c1-name")));
        assertTrue(routes.get(1).getUnmatched().contains(item("c2", "c2-name")));
    }

    @Test
    void report_orRule_firstFalse_secondMiss_twoRoutes() throws Throwable {
        // OR(FALSE, MISS): both paths evaluated, result = INDETERMINATE
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "missing-key", "x"));
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.get(0).getUnmatched().contains(item("c1", "c1-name")));
        assertTrue(routes.get(1).getAbsent().contains(item("c2", "c2-name")));
    }

    @Test
    void report_orRule_firstMiss_secondTrue_twoRoutes() throws Throwable {
        // OR(MISS, TRUE): both paths evaluated, result = TRUE
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = orRule("r1",
                condNode("c1", "c1-name", "missing-key", "x"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.get(0).getAbsent().contains(item("c1", "c1-name")));
        assertTrue(routes.get(1).getMatched().contains(item("c2", "c2-name")));
    }

    // ---- Route.Item details ----

    @Test
    void report_routeItem_conditionIdAndName_correct() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        ConditionNode cond = condNode("my-cond-id", "my-cond-name", "x", "y");
        Rule rule = RuleBuilder.create().id("r1").name("r1").root(cond).build();
        RuleContext ctx = RuleContext.of(Map.of("x", "y")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route.Item matched = ctx.getReports().get(rule).getRoutes().get(0).getMatched().iterator().next();
        assertEquals("my-cond-id", matched.getConditionId());
        assertEquals("my-cond-name", matched.getConditionName());
    }

    // ---- Multiple rules ----

    @Test
    void report_multipleRules_separateReportsPerRule() throws Throwable {
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule r1 = singleRule("r1", "status", "active");
        Rule r2 = singleRule("r2", "role", "admin");
        RuleContext ctx = RuleContext.of(Map.of("status", "active", "role", "user")).setEnableReports(true);
        engine.evaluate(List.of(r1, r2), ctx);

        assertEquals(2, ctx.getReports().size());
        assertTrue(ctx.getReports().get(r1).getRoutes().get(0).getMatched().contains(item("cond-1", "cond-1-name")));
        assertTrue(ctx.getReports().get(r2).getRoutes().get(0).getUnmatched().contains(item("cond-1", "cond-1-name")));
    }

    // ---- Cache + Report ----

    @Test
    void report_cachedCondFalse_withReportEnabled_appendsRoute() throws Throwable {
        // (B OR C) AND A, paths [B,A] and [C,A], A cacheable=true
        // Params: role=user (A=FALSE), status=active (B=TRUE), level=high (C=TRUE)
        // path1: B=TRUE, A=FALSE (cached) → route1={matched=[B], unmatched=[A]}
        // path2: C=TRUE, A=cache-hit FALSE → cached-FALSE branch → route2={matched=[C], unmatched=[A]}
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = buildOrAndRule("r1");
        RuleContext ctx = RuleContext.of(Map.of("role", "user", "status", "active", "level", "high")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        Route route1 = routes.get(0);
        assertTrue(route1.getMatched().contains(item("cond-b", "cond-b-name")));
        assertTrue(route1.getUnmatched().contains(item("cond-a", "cond-a-name")));
        Route route2 = routes.get(1);
        assertTrue(route2.getMatched().contains(item("cond-c", "cond-c-name")));
        assertTrue(route2.getUnmatched().contains(item("cond-a", "cond-a-name")));
    }

    @Test
    void report_cachedCondTrue_withReportEnabled_marksAsMatched() throws Throwable {
        // AND(A, OR(B,C)), paths [A,B] and [A,C], A cacheable=true
        // Params: role=admin (A=TRUE), no status (B=INDETERMINATE), no level (C=INDETERMINATE)
        // path1: A=TRUE (cached), B=INDETERMINATE → route1={matched=[A], absent=[B]}
        // path2: A=cache-hit TRUE → cached-TRUE branch (matchedIdx.add), C=INDETERMINATE → route2={matched=[A], absent=[C]}
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = buildAndOrRule("r1");
        RuleContext ctx = RuleContext.of(Map.of("role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        Route route1 = routes.get(0);
        assertTrue(route1.getMatched().contains(item("cond-a", "cond-a-name")));
        assertEquals(1, route1.getAbsent().size());
        assertTrue(route1.getAbsent().contains(item("cond-b", "cond-b-name")));
        Route route2 = routes.get(1);
        assertTrue(route2.getMatched().contains(item("cond-a", "cond-a-name")));
        assertEquals(1, route2.getAbsent().size());
        assertTrue(route2.getAbsent().contains(item("cond-c", "cond-c-name")));
    }

    @Test
    void report_cachedCondMiss_withReportEnabled_marksAsAbsent() throws Throwable {
        // AND(A, OR(B,C)), paths [A,B] and [A,C], A cacheable=true
        // Params: no role (A=INDETERMINATE, cached), status=inactive (B=FALSE), level=low (C=FALSE)
        // path1: A=INDETERMINATE (cached), B=FALSE → route1={absent=[A], unmatched=[B]}
        // path2: A=cache-hit INDETERMINATE → cached-INDETERMINATE branch (absentIdx.add), C=FALSE → route2={absent=[A], unmatched=[C]}
        AdvancedCeleroEngine engine = new AdvancedCeleroEngine();
        Rule rule = buildAndOrRule("r1");
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive", "level", "low")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        Route route1 = routes.get(0);
        assertTrue(route1.getAbsent().contains(item("cond-a", "cond-a-name")));
        assertTrue(route1.getUnmatched().contains(item("cond-b", "cond-b-name")));
        Route route2 = routes.get(1);
        assertTrue(route2.getAbsent().contains(item("cond-a", "cond-a-name")));
        assertTrue(route2.getUnmatched().contains(item("cond-c", "cond-c-name")));
    }
}
