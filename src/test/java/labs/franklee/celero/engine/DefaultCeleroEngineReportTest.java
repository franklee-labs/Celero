package labs.franklee.celero.engine;

import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.RelationNode;
import labs.franklee.celero.rules.RuleBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultCeleroEngineReportTest {

    // ---- helpers ----

    private static ConditionNode condNode(String id, String name, String field, String value) {
        ConditionNode c = new ConditionNode();
        c.setType("condition").setSign("EQ").setId(id).setName(name);
        c.setProperties(Map.of("field", field, "value", value, "valueType", "String"));
        return c;
    }

    private static ConditionNode condNode(String id, String name, String field, String value, boolean cacheable) {
        ConditionNode c = condNode(id, name, field, value);
        c.setCacheable(cacheable);
        return c;
    }

    private static CeleroRule andRule(String ruleId, ConditionNode... conds) throws Throwable {
        RelationNode root = new RelationNode();
        root.setSign("AND");
        root.setChildren(List.of(conds));
        return RuleBuilder.create().id(ruleId).name(ruleId).root(root).build();
    }

    private static CeleroRule orRule(String ruleId, ConditionNode... conds) throws Throwable {
        RelationNode root = new RelationNode();
        root.setSign("OR");
        root.setChildren(List.of(conds));
        return RuleBuilder.create().id(ruleId).name(ruleId).root(root).build();
    }

    private static CeleroRule singleRule(String ruleId, String key, String value) throws Throwable {
        return RuleBuilder.create()
                .id(ruleId).name(ruleId)
                .root(condNode("cond-1", "cond-1-name", key, value))
                .build();
    }

    // (B OR C) AND A, paths [B,A] and [C,A], A cacheable — used to trigger cached-FALSE branch
    private static CeleroRule buildOrAndRule(String ruleId) throws Throwable {
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

    // AND(A, OR(B,C)), paths [A,B] and [A,C], A cacheable — used to trigger cached-TRUE branch
    private static CeleroRule buildAndOrRule(String ruleId) throws Throwable {
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
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "active"));
        engine.evaluate(rule, ctx);
        assertTrue(ctx.getReports().isEmpty());
    }

    @Test
    void report_disabled_condFalse_noReportsGenerated() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive"));
        engine.evaluate(rule, ctx);
        assertTrue(ctx.getReports().isEmpty());
    }

    @Test
    void report_disabled_cachedCondFalse_noReportsGenerated() throws Throwable {
        // (B OR C) AND A: path1=[B,A], path2=[C,A], A cached as FALSE after path1
        // Report disabled — cached-false branch must NOT append any route
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = buildOrAndRule("r1");
        RuleContext ctx = RuleContext.of(Map.of("role", "user", "status", "active", "level", "high"));
        engine.evaluate(rule, ctx);
        assertTrue(ctx.getReports().isEmpty());
    }

    // ---- Single condition ----

    @Test
    void report_singleCond_true_matchedContainsCond() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "active")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(1, routes.size());
        Route r = routes.get(0);
        assertTrue(r.getMatched().contains(item("cond-1", "cond-1-name")));
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
        assertTrue(r.getAbsent().isEmpty());
    }

    @Test
    void report_singleCond_false_unmatchedContainsCond() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getUnmatched().contains(item("cond-1", "cond-1-name")));
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_singleCond_missingParam_treatedAsFalse() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = singleRule("r1", "status", "active");
        RuleContext ctx = RuleContext.of(Map.of()).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertEquals(1, r.getUnmatched().size());
        assertTrue(r.getSkipped().isEmpty());
    }

    // ---- AND rule ----

    @Test
    void report_andRule_allTrue_allMatched() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active", "role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertEquals(2, r.getMatched().size());
        assertTrue(r.getMatched().contains(item("c1", "c1-name")));
        assertTrue(r.getMatched().contains(item("c2", "c2-name")));
        assertTrue(r.getUnmatched().isEmpty());
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_andRule_firstFalse_unmatched_secondSkipped() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive", "role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertTrue(r.getUnmatched().contains(item("c1", "c1-name")));
        assertTrue(r.getSkipped().contains(item("c2", "c2-name")));
    }

    @Test
    void report_andRule_firstTrue_secondFalse_noSkipped() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = andRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active", "role", "user")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().contains(item("c1", "c1-name")));
        assertTrue(r.getUnmatched().contains(item("c2", "c2-name")));
        assertTrue(r.getSkipped().isEmpty());
    }

    @Test
    void report_andRule_3conds_firstFalse_twoSkipped() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = andRule("r1",
                condNode("c1", "c1-name", "a", "x"),
                condNode("c2", "c2-name", "b", "y"),
                condNode("c3", "c3-name", "c", "z"));
        RuleContext ctx = RuleContext.of(Map.of("a", "wrong")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route r = ctx.getReports().get(rule).getRoutes().get(0);
        assertTrue(r.getMatched().isEmpty());
        assertEquals(1, r.getUnmatched().size());
        assertEquals(2, r.getSkipped().size());
        assertTrue(r.getUnmatched().contains(item("c1", "c1-name")));
        assertTrue(r.getSkipped().contains(item("c2", "c2-name")));
        assertTrue(r.getSkipped().contains(item("c3", "c3-name")));
    }

    // ---- OR rule (multi-path) ----

    @Test
    void report_orRule_firstTrue_onlyOneRoute() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = orRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "active")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(1, routes.size());
        assertTrue(routes.get(0).getMatched().contains(item("c1", "c1-name")));
    }

    @Test
    void report_orRule_firstFalse_secondTrue_twoRoutes() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = orRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive", "role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.get(0).getUnmatched().contains(item("c1", "c1-name")));
        assertTrue(routes.get(1).getMatched().contains(item("c2", "c2-name")));
    }

    @Test
    void report_orRule_allFalse_twoRoutes_eachWithOneUnmatched() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = orRule("r1",
                condNode("c1", "c1-name", "status", "active"),
                condNode("c2", "c2-name", "role", "admin"));
        RuleContext ctx = RuleContext.of(Map.of("status", "inactive", "role", "user")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        routes.forEach(r -> {
            assertEquals(1, r.getUnmatched().size());
            assertTrue(r.getMatched().isEmpty());
            assertTrue(r.getSkipped().isEmpty());
        });
    }

    // ---- Route.Item details ----

    @Test
    void report_routeItem_conditionIdAndName_correct() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        ConditionNode cond = condNode("my-cond-id", "my-cond-name", "x", "y");
        CeleroRule rule = RuleBuilder.create().id("r1").name("r1").root(cond).build();
        RuleContext ctx = RuleContext.of(Map.of("x", "y")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        Route.Item matched = ctx.getReports().get(rule).getRoutes().get(0).getMatched().iterator().next();
        assertEquals("my-cond-id", matched.getConditionId());
        assertEquals("my-cond-name", matched.getConditionName());
    }

    // ---- Multiple rules ----

    @Test
    void report_multipleRules_separateReportsPerRule() throws Throwable {
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule r1 = singleRule("r1", "status", "active");
        CeleroRule r2 = singleRule("r2", "role", "admin");
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
        // path1: B=TRUE, A=FALSE (cached as FALSE) → route1={matched=[B], unmatched=[A]}
        // path2: C=TRUE, A=cache-hit FALSE → cached-FALSE branch → route2={matched=[C], unmatched=[A]}
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = buildOrAndRule("r1");
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
        // Params: role=admin (A=TRUE), no status (B=MISS→FALSE), no level (C=MISS→FALSE)
        // path1: A=TRUE (cached), B=FALSE → route1={matched=[A], unmatched=[B]}
        // path2: A=cache-hit TRUE → cached-TRUE branch (matchedIdx.add), C=FALSE → route2={matched=[A], unmatched=[C]}
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        CeleroRule rule = buildAndOrRule("r1");
        RuleContext ctx = RuleContext.of(Map.of("role", "admin")).setEnableReports(true);
        engine.evaluate(rule, ctx);

        List<Route> routes = ctx.getReports().get(rule).getRoutes();
        assertEquals(2, routes.size());
        Route route1 = routes.get(0);
        assertTrue(route1.getMatched().contains(item("cond-a", "cond-a-name")));
        assertEquals(1, route1.getUnmatched().size());
        assertTrue(route1.getUnmatched().contains(item("cond-b", "cond-b-name")));
        Route route2 = routes.get(1);
        assertTrue(route2.getMatched().contains(item("cond-a", "cond-a-name")));
        assertEquals(1, route2.getUnmatched().size());
        assertTrue(route2.getUnmatched().contains(item("cond-c", "cond-c-name")));
    }
}
