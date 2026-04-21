package labs.franklee.celero.rules;

import com.fasterxml.jackson.databind.json.JsonMapper;
import labs.franklee.celero.engine.DefaultCeleroEngine;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.rules.helper.StubObject;
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
        cond.setProperties(Map.of("key", "status", "value", "active", "valueType", "String"));

        RelationNode root = new RelationNode();
        root.setSign("AND");
        root.setChildren(List.of(cond));

        Rule rule = RuleBuilder.create()
                .id("r001").name("status check").description("status == active")
                .root(root)
                .build();

        assertEquals("r001", rule.getId());
        assertEquals("status check", rule.getName());
        assertNotNull(rule.getPathGroup());
    }

    @Test
    void build_conditionNodeAsRoot_autoWrappedInAnd() throws Throwable {
        ConditionNode cond = new ConditionNode();
        cond.setId("cond-1");
        cond.setType("condition");
        cond.setSign("EQ");
        cond.setProperties(Map.of("key", "status", "value", "active", "valueType", "String"));

        Rule rule = RuleBuilder.create()
                .id("r002").name("single cond")
                .root(cond)
                .build();

        assertEquals("r002", rule.getId());
        assertNotNull(rule.getPathGroup());
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
                  "id": "r010",
                  "name": "age check",
                  "description": "age > 18",
                  "root": {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "id": "cond-1",
                        "type": "condition",
                        "sign": "GT",
                        "key": "age",
                        "value": "18",
                        "valueType": "Number"
                      }
                    ]
                  }
                }
                """;

        Rule rule = RuleBuilder.fromJson(json).build();

        assertEquals("r010", rule.getId());
        assertEquals("age check", rule.getName());
        assertEquals("age > 18", rule.getDescription());
        assertNotNull(rule.getPathGroup());
    }

    @Test
    void fromJson_inCondition_parsesJsonArray() throws Throwable {
        String json = """
                {
                  "id": "r011",
                  "root": {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "id": "cond-1",
                        "type": "condition",
                        "sign": "IN",
                        "key": "role",
                        "value": "[\\"admin\\", \\"ops\\"]"
                      }
                    ]
                  }
                }
                """;

        Rule rule = RuleBuilder.fromJson(json).build();
        assertNotNull(rule.getPathGroup());
    }

    @Test
    void fromJson_nestedOrUnderAnd() throws Throwable {
        String json = """
                {
                  "id": "r020",
                  "root": {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "id": "cond-1",
                        "type": "condition",
                        "sign": "GTE",
                        "key": "score",
                        "value": "60",
                        "valueType": "Number"
                      },
                      {
                        "type": "relation",
                        "sign": "OR",
                        "children": [
                          {
                            "id": "cond-2",
                            "type": "condition",
                            "sign": "EQ",
                            "key": "vip",
                            "value": "true",
                            "valueType": "Boolean"
                          },
                          {
                            "id": "cond-3",
                            "type": "condition",
                            "sign": "EQ",
                            "key": "role",
                            "value": "admin",
                            "valueType": "String"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        Rule rule = RuleBuilder.fromJson(json).build();
        assertEquals("r020", rule.getId());
        assertNotNull(rule.getPathGroup());
    }

    @Test
    void fromJson_conditionNodeAsRoot_autoWrappedInAnd() throws Throwable {
        String json = """
                {
                  "id": "r040",
                  "name": "single condition",
                  "root": {
                    "id": "cond-1",
                    "type": "condition",
                    "sign": "EQ",
                    "key": "status",
                    "value": "active",
                    "valueType": "String"
                  }
                }
                """;

        Rule rule = RuleBuilder.fromJson(json).build();
        assertEquals("r040", rule.getId());
        assertNotNull(rule.getPathGroup());
    }

    @Test
    void fromJson_invalidSign_throwsException() {
        String json = """
                {
                  "id": "r030",
                  "root": {
                    "type": "relation",
                    "sign": "AND",
                    "children": [
                      {
                        "type": "condition",
                        "sign": "UNKNOWN",
                        "key": "x",
                        "value": "1",
                        "valueType": "Number"
                      }
                    ]
                  }
                }
                """;

        assertThrows(Exception.class, () -> RuleBuilder.fromJson(json).build());
    }

    @Test
    void fromJson_properties_anySetterGetter() throws Throwable {
        String json = """
                {
                  "name": "stub",
                  "key1": "x",
                  "key2": "y"
                }
                """;
        JsonMapper mapper = new JsonMapper();
        StubObject stubObject = mapper.readValue(json, StubObject.class);
        Map<String, Object> properties = stubObject.getProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals("x", properties.get("key1"));
        assertEquals("y", properties.get("key2"));
        assertEquals("stub", stubObject.getName());
    }

    @Test
    void fromJson_invalidRelationNode_throwException() throws Throwable {
        String json = """
                {
                  "id": "id-1",
                  "name": "name-1",
                  "root": {
                      "type": "relation",
                      "sign": "AND"
                  }
                }
                """;
        assertThrows(InvalidRuleNodeException.class, () -> RuleBuilder.fromJson(json).build());
    }

    // ---- AND + OR + NOT combined JSON test cases ----
    //
    // Rule: status=active AND (role=admin OR level=high) AND NOT(banned=true)

    private static final String COMPLEX_JSON = """
            {
              "id": "r-complex",
              "name": "complex rule",
              "root": {
                "type": "relation",
                "sign": "AND",
                "children": [
                  {
                    "id": "c-status",
                    "name": "status check",
                    "type": "condition",
                    "sign": "EQ",
                    "key": "status",
                    "value": "active",
                    "valueType": "String"
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
                        "key": "role",
                        "value": "admin",
                        "valueType": "String"
                      },
                      {
                        "id": "c-level",
                        "name": "level check",
                        "type": "condition",
                        "sign": "EQ",
                        "key": "level",
                        "value": "high",
                        "valueType": "String"
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
                        "key": "banned",
                        "value": "true",
                        "valueType": "Boolean"
                      }
                    ]
                  }
                ]
              }
            }
            """;

    @Test
    void fromJson_andOrNot_buildsSuccessfully() throws Throwable {
        Rule rule = RuleBuilder.fromJson(COMPLEX_JSON).build();
        assertEquals("r-complex", rule.getId());
        assertEquals("complex rule", rule.getName());
        assertNotNull(rule.getPathGroup());
    }

    @Test
    void fromJson_andOrNot_allConditionsMet_returnsTrue() throws Throwable {
        Rule rule = RuleBuilder.fromJson(COMPLEX_JSON).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertTrue(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "admin", "banned", false))));
    }

    @Test
    void fromJson_andOrNot_orSecondBranchMet_returnsTrue() throws Throwable {
        // role does not match, but level=high satisfies the OR branch
        Rule rule = RuleBuilder.fromJson(COMPLEX_JSON).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertTrue(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "user", "level", "high", "banned", false))));
    }

    @Test
    void fromJson_andOrNot_notViolated_returnsFalse() throws Throwable {
        // banned=true violates the NOT condition
        Rule rule = RuleBuilder.fromJson(COMPLEX_JSON).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertFalse(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "admin", "banned", true))));
    }

    @Test
    void fromJson_andOrNot_andFails_returnsFalse() throws Throwable {
        // status does not match — first AND condition fails
        Rule rule = RuleBuilder.fromJson(COMPLEX_JSON).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertFalse(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "inactive", "role", "admin", "banned", false))));
    }

    @Test
    void fromJson_andOrNot_orAllFail_returnsFalse() throws Throwable {
        // neither role nor level matches — OR fails
        Rule rule = RuleBuilder.fromJson(COMPLEX_JSON).build();
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        assertFalse(engine.evaluate(rule, RuleContext.of(
                Map.of("status", "active", "role", "user", "level", "low", "banned", false))));
    }
}
