package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.base.ValueType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotEqualConditionTest {

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
    void negate_returnsEqualCondition() throws Exception {
        assertInstanceOf(EqualCondition.class,
                new NotEqualCondition("status", "active", ValueType.String).negate());
        assertInstanceOf(EqualCondition.class,
                new NotEqualCondition("age", "18", ValueType.Number).negate());
        assertInstanceOf(EqualCondition.class,
                new NotEqualCondition("active", "true", ValueType.Boolean).negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        NotEqualCondition cond = new NotEqualCondition("x", "1", ValueType.Number);
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- compile + execute: String ----

    @Test
    void string_sameValue_returnsFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("role", "admin", ValueType.String);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "admin")).isFalse());
    }

    @Test
    void string_differentValue_returnsTrue() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("role", "admin", ValueType.String);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "user")).isTrue());
    }

    @Test
    void string_valueWithSpecialChars_handledByBinding() throws Exception {
        // value contains quotes — safe because it goes through variable binding, not expression concat
        NotEqualCondition cond = new NotEqualCondition("name", "say \"hello\"", ValueType.String);
        cond.compile();
        assertTrue(cond.execute(ctx("name", "say \"hello\"")).isFalse());
        assertTrue(cond.execute(ctx("name", "say hello")).isTrue());
    }

    // ---- compile + execute: Boolean ----

    @Test
    void boolean_true_sameValue_returnsFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("active", "true", ValueType.Boolean);
        cond.compile();
        assertTrue(cond.execute(ctx("active", true)).isFalse());
    }

    @Test
    void boolean_false_sameValue_returnsFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("active", "false", ValueType.Boolean);
        cond.compile();
        assertTrue(cond.execute(ctx("active", false)).isFalse());
    }

    @Test
    void boolean_differentValue_returnsTrue() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("active", "true", ValueType.Boolean);
        cond.compile();
        assertTrue(cond.execute(ctx("active", false)).isTrue());
    }

    // ---- compile + execute: Number (integer) ----

    @Test
    void number_integer_sameValue_returnsFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void number_integer_differentValue_returnsTrue() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 20L)).isTrue());
    }

    @Test
    void number_integerWithTrailingZeros_treatedAsLong() throws Exception {
        // "18.00" strips to "18" → compiled as long
        NotEqualCondition cond = new NotEqualCondition("age", "18.00", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
        assertTrue(cond.execute(ctx("age", 19L)).isTrue());
    }

    // ---- compile + execute: Number (decimal) ----

    @Test
    void number_decimal_sameValue_returnsFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isFalse());
    }

    @Test
    void number_decimal_differentValue_returnsTrue() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.0)).isTrue());
    }

    // ---- compile + execute: Expression ----

    @Test
    void expression_crossFieldInequality() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("a", "b", ValueType.Expression);
        cond.compile();
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isFalse());
        assertTrue(cond.execute(ctx("a", 10L, "b", 20L)).isTrue());
    }

    // ---- before: builtin params not leaked into user context ----

    @Test
    void before_userContextUnmodified() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("role", "admin", ValueType.String);
        cond.compile();

        Context ctx = ctx("role", "user");
        cond.execute(ctx);

        assertTrue(ctx.getParams().keySet().stream()
                .noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_defaultContext_returnsFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_missableContext_returnsMiss() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        NotEqualCondition cond = new NotEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        // age=18 → FALSE (equal, so not-equal is false)
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
        // age absent + missable context → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → program is null → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        NotEqualCondition cond = new NotEqualCondition("role", "admin", ValueType.String);
        assertThrows(Exception.class, () -> cond.execute(ctx("role", "admin")));
    }

    // ---- validate ----

    @Test
    void validate_number_validInteger_returnsTrue() {
        assertTrue(new NotEqualCondition("age", "18", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_validDecimal_returnsTrue() {
        assertTrue(new NotEqualCondition("score", "99.5", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_invalidString_returnsFalse() {
        assertFalse(new NotEqualCondition("age", "abc", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_string_returnsTrue() {
        assertTrue(new NotEqualCondition("role", "admin", ValueType.String).validate().isValid());
    }

    @Test
    void validate_boolean_returnsTrue() {
        assertTrue(new NotEqualCondition("active", "true", ValueType.Boolean).validate().isValid());
    }

    @Test
    void validate_expression_returnsTrue() {
        assertTrue(new NotEqualCondition("a", "b", ValueType.Expression).validate().isValid());
    }
}
