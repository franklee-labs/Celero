package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.base.ValueType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EqualConditionTest {

    private static Context ctx(Object... kvs) {
        var m = new java.util.HashMap<String, Object>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return Context.Builder.createBuilder(RuleContext.of(m)).build();
    }

    private static Context ctxMissable(Object... kvs) {
        var m = new java.util.HashMap<String, Object>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return Context.Builder.createBuilder(RuleContext.of(m)).enableMissState().build();
    }

    // ---- negate ----

    @Test
    void negate_returnsNotEqualCondition() throws Exception {
        assertInstanceOf(NotEqualCondition.class,
                new EqualCondition("status", "active", ValueType.String).negate());
        assertInstanceOf(NotEqualCondition.class,
                new EqualCondition("age", "18", ValueType.Number).negate());
        assertInstanceOf(NotEqualCondition.class,
                new EqualCondition("active", "true", ValueType.Boolean).negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        EqualCondition cond = new EqualCondition("x", "1", ValueType.Number);
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- compile + execute: String ----

    @Test
    void string_match() throws Exception {
        EqualCondition cond = new EqualCondition("role", "admin", ValueType.String);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "admin")).isTrue());
    }

    @Test
    void string_noMatch() throws Exception {
        EqualCondition cond = new EqualCondition("role", "admin", ValueType.String);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "user")).isFalse());
    }

    @Test
    void string_valueWithSpecialChars_handledByBinding() throws Exception {
        // value contains quotes — safe because it goes through variable binding, not expression concat
        EqualCondition cond = new EqualCondition("name", "say \"hello\"", ValueType.String);
        cond.compile();
        assertTrue(cond.execute(ctx("name", "say \"hello\"")).isTrue());
        assertTrue(cond.execute(ctx("name", "say hello")).isFalse());
    }

    // ---- compile + execute: Boolean ----

    @Test
    void boolean_true_match() throws Exception {
        EqualCondition cond = new EqualCondition("active", "true", ValueType.Boolean);
        cond.compile();
        assertTrue(cond.execute(ctx("active", true)).isTrue());
    }

    @Test
    void boolean_false_match() throws Exception {
        EqualCondition cond = new EqualCondition("active", "false", ValueType.Boolean);
        cond.compile();
        assertTrue(cond.execute(ctx("active", false)).isTrue());
    }

    @Test
    void boolean_noMatch() throws Exception {
        EqualCondition cond = new EqualCondition("active", "true", ValueType.Boolean);
        cond.compile();
        assertTrue(cond.execute(ctx("active", false)).isFalse());
    }

    // ---- compile + execute: Number (integer) ----

    @Test
    void number_integer_match() throws Exception {
        EqualCondition cond = new EqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    @Test
    void number_integer_noMatch() throws Exception {
        EqualCondition cond = new EqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 20L)).isFalse());
    }

    @Test
    void number_integerWithTrailingZeros_treatedAsLong() throws Exception {
        // "18.00" strips to "18" → compiled as long
        EqualCondition cond = new EqualCondition("age", "18.00", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    // ---- compile + execute: Number (decimal) ----

    @Test
    void number_decimal_match() throws Exception {
        EqualCondition cond = new EqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isTrue());
    }

    @Test
    void number_decimal_noMatch() throws Exception {
        EqualCondition cond = new EqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.0)).isFalse());
    }

    // ---- compile + execute: Expression ----

    @Test
    void expression_crossFieldEquality() throws Exception {
        // value is a raw CEL expression referencing another variable
        EqualCondition cond = new EqualCondition("a", "b", ValueType.Expression);
        cond.compile();
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 20L)).isFalse());
    }

    // ---- before: builtin params merged, user params unchanged ----

    @Test
    void before_userContextUnmodified() throws Exception {
        EqualCondition cond = new EqualCondition("role", "admin", ValueType.String);
        cond.compile();

        Context ctx = ctx("role", "admin");
        cond.execute(ctx);

        // original context must not contain any builtin keys
        assertTrue(ctx.getParams().keySet().stream()
                .noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- missing parameter: execute() catches internally ----

    @Test
    void missingParameter_topLevelVar_defaultContext_returnsFalse() throws Exception {
        EqualCondition cond = new EqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_topLevelVar_missableContext_returnsMiss() throws Exception {
        EqualCondition cond = new EqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_topLevelVar_distinguishedFromFalse() throws Exception {
        EqualCondition cond = new EqualCondition("age", "18", ValueType.Number);
        cond.compile();
        // age=20 → EvalResult.FALSE (not equal to 18)
        assertTrue(cond.execute(ctx("age", 20L)).isFalse());
        // age absent + missable context → EvalResult.MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_chainedMapKey_defaultContext_returnsFalse() throws Exception {
        // chained map key miss: params.user exists but params.user.age does not
        EqualCondition cond = new EqualCondition("params.user.age", "18", ValueType.Number);
        cond.compile();
        Context ctxMissingAge = ctx("params", Map.of("user", Map.of("name", "frank")));
        assertTrue(cond.execute(ctxMissingAge).isFalse());
    }

    @Test
    void missingParameter_chainedMapKey_missableContext_returnsMiss() throws Exception {
        EqualCondition cond = new EqualCondition("params.user.age", "18", ValueType.Number);
        cond.compile();
        var m = new java.util.HashMap<String, Object>();
        m.put("params", Map.of("user", Map.of("name", "frank")));
        Context missable = Context.Builder.createBuilder(RuleContext.of(m)).enableMissState().build();
        assertTrue(cond.execute(missable).isIndeterminate());
    }

    @Test
    void missingParameter_chainedMapKey_distinguishedFromFalse() throws Exception {
        EqualCondition cond = new EqualCondition("params.user.age", "18", ValueType.Number);
        cond.compile();
        // params.user.age=20 → EvalResult.FALSE
        assertTrue(cond.execute(ctx("params", Map.of("user", Map.of("age", 20L)))).isFalse());
        // params.user.age key absent + missable context → EvalResult.MISS
        Map<String, Object> m = new HashMap<>();
        m.put("params", Map.of("user", Map.of("name", "frank")));
        Context missable = Context.Builder.createBuilder(RuleContext.of(m)).enableMissState().build();
        assertTrue(cond.execute(missable).isIndeterminate());
    }

    // ---- compile not called → program is null → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        EqualCondition cond = new EqualCondition("role", "admin", ValueType.String);
        assertThrows(Exception.class, () -> cond.execute(ctx("role", "admin")));
    }

    // ---- validate ----

    @Test
    void validate_number_validInteger_returnsTrue() {
        assertTrue(new EqualCondition("age", "18", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_validDecimal_returnsTrue() {
        assertTrue(new EqualCondition("score", "99.5", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_invalidString_returnsFalse() {
        assertFalse(new EqualCondition("age", "abc", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_string_returnsTrue() {
        assertTrue(new EqualCondition("role", "admin", ValueType.String).validate().isValid());
    }

    @Test
    void validate_boolean_returnsTrue() {
        assertTrue(new EqualCondition("active", "true", ValueType.Boolean).validate().isValid());
    }

    @Test
    void validate_expression_returnsTrue() {
        assertTrue(new EqualCondition("a", "b", ValueType.Expression).validate().isValid());
    }
}
