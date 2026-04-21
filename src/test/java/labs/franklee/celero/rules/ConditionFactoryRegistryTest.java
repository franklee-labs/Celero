package labs.franklee.celero.rules;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.logic.base.Priority;
import labs.franklee.celero.logic.base.ValueType;
import labs.franklee.celero.rules.internal.ConditionFactory;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConditionFactoryRegistryTest {

    // ---- built-in signs are all registered ----

    @Test
    void builtInConditionSigns_allPresent() {
        for (String sign : new String[]{"EQ", "NEQ", "GT", "GTE", "LT", "LTE", "IN", "NIN", "REGEX", "CEL"}) {
            assertNotNull(
                    ConditionFactoryRegistry.getConditionFactory("any", sign),
                    "missing built-in sign: " + sign
            );
        }
    }

    // ---- registerGlobalFactory ----

    @Test
    void registerGlobalFactory_nullSign_throws() {
        assertThrows(InvalidRuleNodeException.class, () ->
                ConditionFactoryRegistry.registerGlobalFactory(null, p -> null));
    }

    @Test
    void registerGlobalFactory_reservedConditionSign_throws() {
        for (String sign : new String[]{"EQ", "eq", "NIN", "REGEX", "CEL"}) {
            assertThrows(InvalidRuleNodeException.class, () ->
                    ConditionFactoryRegistry.registerGlobalFactory(sign, p -> null),
                    "should reject reserved sign: " + sign);
        }
    }

    @Test
    void registerGlobalFactory_reservedRelationSign_throws() {
        for (String sign : new String[]{"AND", "and", "OR", "NOT"}) {
            assertThrows(InvalidRuleNodeException.class, () ->
                    ConditionFactoryRegistry.registerGlobalFactory(sign, p -> null),
                    "should reject relation sign: " + sign);
        }
    }

    @Test
    @Order(1)
    void registerGlobalFactory_validSign_registeredAndQueryable() throws InvalidRuleNodeException {
        ConditionFactory factory = p -> {
            throw new UnsupportedOperationException("stub");
        };
        ConditionFactoryRegistry.registerGlobalFactory("TEST_GLOBAL_A", factory);
        assertSame(factory, ConditionFactoryRegistry.getConditionFactory("any-rule", "TEST_GLOBAL_A"));
    }

    @Test
    @Order(2)
    void registerGlobalFactory_duplicateSign_throws() {
        assertThrows(InvalidRuleNodeException.class, () ->
                ConditionFactoryRegistry.registerGlobalFactory("TEST_GLOBAL_A", p -> null));
    }

    // ---- registerFactory (rule-level) ----

    @Test
    void registerFactory_nullSign_throws() {
        assertThrows(InvalidRuleNodeException.class, () ->
                ConditionFactoryRegistry.registerFactory("rule-x", null, p -> null));
    }

    @Test
    void registerFactory_reservedSign_throws() {
        assertThrows(InvalidRuleNodeException.class, () ->
                ConditionFactoryRegistry.registerFactory("rule-x", "EQ", p -> null));
        assertThrows(InvalidRuleNodeException.class, () ->
                ConditionFactoryRegistry.registerFactory("rule-x", "AND", p -> null));
    }

    @Test
    void registerFactory_duplicateSignSameRule_throws() throws InvalidRuleNodeException {
        ConditionFactoryRegistry.registerFactory("rule-dup", "RULE_SIGN_B", p -> null);
        assertThrows(InvalidRuleNodeException.class, () ->
                ConditionFactoryRegistry.registerFactory("rule-dup", "RULE_SIGN_B", p -> null));
    }

    @Test
    void registerFactory_sameSignDifferentRules_succeeds() throws InvalidRuleNodeException {
        ConditionFactoryRegistry.registerFactory("rule-c1", "RULE_SIGN_C", p -> null);
        ConditionFactoryRegistry.registerFactory("rule-c2", "RULE_SIGN_C", p -> null);
    }

    // ---- getConditionFactory ----

    @Test
    void getConditionFactory_nullSign_returnsNull() {
        assertNull(ConditionFactoryRegistry.getConditionFactory("any", null));
    }

    @Test
    void getConditionFactory_unknownSign_returnsNull() {
        assertNull(ConditionFactoryRegistry.getConditionFactory("any", "NO_SUCH_SIGN"));
    }

    @Test
    void getConditionFactory_signIsCaseInsensitive() {
        assertNotNull(ConditionFactoryRegistry.getConditionFactory("any", "eq"));
        assertNotNull(ConditionFactoryRegistry.getConditionFactory("any", "Eq"));
        assertNotNull(ConditionFactoryRegistry.getConditionFactory("any", "EQ"));
    }

    @Test
    void getConditionFactory_ruleLevelPriorityOverGlobal() throws InvalidRuleNodeException {
        // register a custom sign globally
        ConditionFactory globalFactory = p -> { throw new UnsupportedOperationException("global"); };
        ConditionFactoryRegistry.registerGlobalFactory("TEST_PRIORITY", globalFactory);

        // override at rule level
        ConditionFactory ruleFactory = p -> { throw new UnsupportedOperationException("rule"); };
        ConditionFactoryRegistry.registerFactory("priority-rule", "TEST_PRIORITY", ruleFactory);

        assertSame(ruleFactory,  ConditionFactoryRegistry.getConditionFactory("priority-rule", "TEST_PRIORITY"));
        assertSame(globalFactory, ConditionFactoryRegistry.getConditionFactory("other-rule",   "TEST_PRIORITY"));
    }

    // ---- end-to-end: rule-level custom factory executes correctly ----

    @Test
    void ruleLevelFactory_usedDuringRuleBuild() throws Throwable {
        String ruleId = "custom-factory-rule";
        ConditionFactoryRegistry.registerFactory(ruleId, "ALWAYS_TRUE", props -> {
            var cond = new labs.franklee.celero.logic.impl.EqualCondition("__stub__", "true", ValueType.Boolean, Priority.DEFAULT) {
                @Override
                public boolean evaluate(labs.franklee.celero.context.Context ctx) {
                    return true;
                }
            };
            return cond;
        });

        String json = """
                {
                  "id": "%s",
                  "root": {
                    "id": "%s",
                    "type": "condition",
                    "sign": "ALWAYS_TRUE"
                  }
                }
                """.formatted(ruleId, "cond-1");

        Rule rule = RuleBuilder.fromJson(json).build();
        assertNotNull(rule.getPathGroup());
    }

    // ---- priority propagation through factories ----

    @Test
    void factory_withPriority_conditionHasExpectedPriority() {
        for (String sign : new String[]{"EQ", "NEQ", "GT", "GTE", "LT", "LTE"}) {
            ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory("any", sign);
            var props = Map.<String, Object>of("key", "x", "value", "1", "valueType", "Number", "priority", 10);
            assertEquals(10, factory.create(props).getPriority(), "priority mismatch for sign: " + sign);
        }
    }

    @Test
    void factory_withoutPriority_conditionHasDefaultPriority() {
        for (String sign : new String[]{"EQ", "NEQ", "GT", "GTE", "LT", "LTE"}) {
            ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory("any", sign);
            var props = Map.<String, Object>of("key", "x", "value", "1", "valueType", "Number");
            assertEquals(Priority.DEFAULT, factory.create(props).getPriority(), "expected default priority for sign: " + sign);
        }
    }

    @Test
    void celFactory_withPriority_conditionHasExpectedPriority() {
        ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory("any", "CEL");
        var props = Map.<String, Object>of("value", "x == 1", "priority", 5);
        assertEquals(5, factory.create(props).getPriority());
    }

    @Test
    void celFactory_withoutPriority_conditionHasDefaultPriority() {
        ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory("any", "CEL");
        var props = Map.<String, Object>of("value", "x == 1");
        assertEquals(Priority.DEFAULT, factory.create(props).getPriority());
    }

    @Test
    void inFactory_withPriority_conditionHasExpectedPriority() {
        for (String sign : new String[]{"IN", "NIN"}) {
            ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory("any", sign);
            var props = Map.<String, Object>of("key", "x", "value", "[1,2,3]", "valueType", "Number", "priority", 7);
            assertEquals(7, factory.create(props).getPriority(), "priority mismatch for sign: " + sign);
        }
    }

    @Test
    void regexFactory_withPriority_conditionHasExpectedPriority() {
        ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory("any", "REGEX");
        var props = Map.<String, Object>of("key", "x", "value", "^abc$", "priority", 3);
        assertEquals(3, factory.create(props).getPriority());
    }
}
