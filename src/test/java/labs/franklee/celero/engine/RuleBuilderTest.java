package labs.franklee.celero.engine;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.path.Path;
import labs.franklee.celero.logic.path.PathGroup;
import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.RelationNode;
import labs.franklee.celero.rules.Rule;
import labs.franklee.celero.rules.RuleBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleBuilderTest {

    // ---- programmatic ----

    @Test
    void build_singleEqCondition() throws Throwable {
        ConditionNode cond = new ConditionNode();
        cond.setId("cond-1");
        cond.setType("condition");
        cond.setSign("EQ");
        cond.setProperties(Map.of("field", "status", "value", "active", "valueType", "String"));

        RelationNode root = new RelationNode();
        root.setSign("AND");
        root.setChildren(List.of(cond));

        CeleroRule rule = RuleBuilder.create()
                .id("r001").name("status check").description("status == active")
                .root(root)
                .build();

        assertEquals("r001", rule.getId());
        assertEquals("status check", rule.getName());
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void build_conditionNodeAsRoot_autoWrappedInAnd() throws Throwable {
        ConditionNode cond = new ConditionNode();
        cond.setId("cond-1");
        cond.setType("condition");
        cond.setSign("EQ");
        cond.setProperties(Map.of("field", "status", "value", "active", "valueType", "String"));

        CeleroRule rule = RuleBuilder.create()
                .id("r002").name("single cond")
                .root(cond)
                .build();

        assertEquals("r002", rule.getId());
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void build_nullRoot_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () ->
                RuleBuilder.create().id("r003").build());
    }

    // ---- fromJson ----

    @Test
    void fromJson_singleCondition_metadataAndPathGroupCorrect() throws Throwable {
        String json = """
                {
                  "type": "relation",
                  "sign": "AND",
                  "children": [
                    {
                      "id": "cond-1",
                      "type": "condition",
                      "sign": "GT",
                      "properties": {
                          "field": "age",
                          "value": "18",
                          "valueType": "Number"
                      }
                    }
                  ]
                }
                """;

        CeleroRule rule = RuleBuilder.fromJson("r010", json).name("age check").description("age > 18").build();

        assertEquals("r010", rule.getId());
        assertEquals("age check", rule.getName());
        assertEquals("age > 18", rule.getDescription());
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void fromJson_inCondition_parsesJsonArray() throws Throwable {
        String json = """
                {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "id": "cond-1",
                        "type": "condition",
                        "sign": "IN",
                        "properties": {
                            "field": "role",
                            "value": "[\\"admin\\", \\"ops\\"]",
                            "valueType": "List"
                        }
                      }
                    ]
                }
                """;

        CeleroRule rule = RuleBuilder.fromJson("r011", json).build();
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void fromJson_nestedOrUnderAnd() throws Throwable {
        String json = """
                {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "id": "cond-1",
                        "type": "condition",
                        "sign": "GTE",
                        "properties": {
                            "field": "score",
                            "value": "60",
                            "valueType": "Number"
                        }
                      },
                      {
                        "type": "relation",
                        "sign": "OR",
                        "children": [
                          {
                            "id": "cond-2",
                            "type": "condition",
                            "sign": "EQ",
                            "properties": {
                                "field": "vip",
                                "value": "true",
                                "valueType": "Boolean"
                            }
                          },
                          {
                            "id": "cond-3",
                            "type": "condition",
                            "sign": "EQ",
                            "properties": {
                                "field": "role",
                                "value": "admin",
                                "valueType": "String"
                            }
                          }
                        ]
                      }
                    ]
                }
                """;

        CeleroRule rule = RuleBuilder.fromJson("r020", json).build();
        assertEquals("r020", rule.getId());
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void fromJson_conditionNodeAsRoot_autoWrappedInAnd() throws Throwable {
        String json = """
                {
                    "id": "cond-1",
                    "type": "condition",
                    "sign": "EQ",
                    "properties": {
                        "field": "status",
                        "value": "active",
                        "valueType": "String"
                    }
                }
                """;

        CeleroRule rule = RuleBuilder.fromJson("r040", json).name("single condition").build();
        assertEquals("r040", rule.getId());
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void fromJson_invalidSign_throwsException() {
        String json = """
                {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "type": "condition",
                        "sign": "UNKNOWN",
                        "properties": {
                            "field": "x",
                            "value": "1",
                            "valueType": "Number"
                        }
                      }
                    ]
                }
                """;

        assertThrows(Exception.class, () -> RuleBuilder.fromJson("r030", json).build());
    }

    @Test
    void fromJson_invalidRelationNode_throwException() throws Throwable {
        String json = """
                {
                      "type": "relation",
                      "sign": "AND"
                }
                """;
        assertThrows(InvalidRuleNodeException.class, () -> RuleBuilder.fromJson("id-1", json).name("name-1").build());
    }

    // ---- AND + OR + NOT combined JSON test cases ----
    //
    // Rule: status=active AND (role=admin OR level=high) AND NOT(banned=true)

    private static final String COMPLEX_JSON = """
            {
                "type": "relation",
                "sign": "AND",
                "children": [
                  {
                    "id": "c-status",
                    "name": "status check",
                    "type": "condition",
                    "sign": "EQ",
                    "properties": {
                        "field": "status",
                        "value": "active",
                        "valueType": "String"
                    }
                  },
                  {
                    "type": "relation",
                    "sign": "OR",
                    "children": [
                      {
                        "id": "c-role",
                        "name": "role check",
                        "type": "condition",
                        "sign": "EQ",
                        "properties": {
                            "field": "role",
                            "value": "admin",
                            "valueType": "String"
                        }
                      },
                      {
                        "id": "c-level",
                        "name": "level check",
                        "type": "condition",
                        "sign": "EQ",
                        "properties": {
                            "field": "level",
                            "value": "high",
                            "valueType": "String"
                        }
                      }
                    ]
                  },
                  {
                    "type": "relation",
                    "sign": "NOT",
                    "children": [
                      {
                        "id": "c-banned",
                        "name": "banned check",
                        "type": "condition",
                        "sign": "EQ",
                        "properties": {
                            "field": "banned",
                            "value": "true",
                            "valueType": "Boolean"
                        }
                      }
                    ]
                  }
                ]
            }
            """;

    @Test
    void fromJson_andOrNot_buildsSuccessfully() throws Throwable {
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        assertEquals("r-complex", rule.getId());
        assertEquals("complex rule", rule.getName());
        assertNotNull(rule.getRule().getPathGroup());
    }

    @Test
    void fromJson_andOrNot_allConditionsMet_returnsTrue() throws Throwable {
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertTrue(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "admin", "banned", false))));
    }

    @Test
    void fromJson_andOrNot_orSecondBranchMet_returnsTrue() throws Throwable {
        // role does not match, but level=high satisfies the OR branch
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertTrue(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "user", "level", "high", "banned", false))));
    }

    @Test
    void fromJson_andOrNot_notViolated_returnsFalse() throws Throwable {
        // banned=true violates the NOT condition
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertFalse(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "admin", "banned", true))));
    }

    @Test
    void fromJson_andOrNot_andFails_returnsFalse() throws Throwable {
        // status does not match — first AND condition fails
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertFalse(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "inactive", "role", "admin", "banned", false))));
    }

    @Test
    void fromJson_andOrNot_orAllFail_returnsFalse() throws Throwable {
        // neither role nor level matches — OR fails
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertFalse(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "user", "level", "low", "banned", false))));
    }

    // CeleroRule

    @Test
    void celeroRule_rule() throws Throwable {
        CeleroRule rule = RuleBuilder.fromJson("r-complex", COMPLEX_JSON).name("complex rule")
                .description("complex rule description").build();
        Rule r = rule.getRule();
        assertEquals(rule.getId(), r.getId());
        assertEquals(rule.getName(), r.getName());
        assertEquals(rule.getDescription(), r.getDescription());
        assertEquals(rule.isCacheable(), r.isCacheable());
        rule.setCacheable(!rule.isCacheable());
        assertEquals(rule.isCacheable(), r.isCacheable());
        Solutions solutions = rule.getSolutions();
        PathGroup pathGroup = r.getPathGroup();
        assertEquals(solutions.getSolutionCount(), pathGroup.paths().size());
        for (int i = 0; i < solutions.getSolutionCount(); i++) {
            Solutions.Solution solution = solutions.getSolutionAt(i);
            Path path = pathGroup.paths().get(i);
            assertEquals(solution.getConditionCount(), path.conditions().size());
            for (int i1 = 0; i1 < solution.getConditionCount(); i1++) {
                Solutions.Condition condition = solution.getConditionAt(i1);
                Condition condition1 = path.conditions().get(i1);
                assertEquals(condition.getId(), condition1.getId());
                assertEquals(condition.getName(), condition1.getName());
            }
            assertNull(solution.getConditionAt(-1));
            assertNull(solution.getConditionAt(solution.getConditionCount()));
        }

        assertNull(solutions.getSolutionAt(-1));
        assertNull(solutions.getSolutionAt(solutions.getSolutionCount()));
    }
}
