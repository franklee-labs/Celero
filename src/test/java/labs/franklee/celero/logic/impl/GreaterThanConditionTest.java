package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.base.ValueType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GreaterThanConditionTest {

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
    void negate_returnsLessThanOrEqualCondition() throws Exception {
        assertInstanceOf(LessThanOrEqualCondition.class,
                new GreaterThanCondition("age", "18", ValueType.Number).negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        GreaterThanCondition cond = new GreaterThanCondition("x", "10", ValueType.Number);
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- compile + execute: Number (integer) ----

    @Test
    void number_integer_greater_returnsTrue() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isTrue());
    }

    @Test
    void number_integer_equal_returnsFalse() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void number_integer_less_returnsFalse() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isFalse());
    }

    @Test
    void number_integerWithTrailingZeros_treatedAsLong() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18.00", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isTrue());
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    // ---- compile + execute: Number (decimal) ----

    @Test
    void number_decimal_greater_returnsTrue() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 100.0)).isTrue());
    }

    @Test
    void number_decimal_equal_returnsFalse() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isFalse());
    }

    @Test
    void number_decimal_less_returnsFalse() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.0)).isFalse());
    }

    // ---- compile + execute: Expression ----

    @Test
    void expression_crossField() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("a", "b", ValueType.Expression);
        cond.compile();
        assertTrue(cond.execute(ctx("a", 20L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isFalse());
        assertTrue(cond.execute(ctx("a", 5L, "b", 10L)).isFalse());
    }

    // ---- unsupported type ----

    @Test
    void compile_unsupportedType_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> new GreaterThanCondition("role", "admin", ValueType.String).compile());
        assertThrows(UnsupportedOperationException.class,
                () -> new GreaterThanCondition("active", "true", ValueType.Boolean).compile());
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_defaultContext_returnsFalse() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_missableContext_returnsMiss() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        cond.compile();
        // age=17 → FALSE (not greater than 18)
        assertTrue(cond.execute(ctx("age", 17L)).isFalse());
        // age absent + missable context → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        GreaterThanCondition cond = new GreaterThanCondition("age", "18", ValueType.Number);
        assertThrows(Exception.class, () -> cond.execute(ctx("age", 20L)));
    }

    // ---- validate ----

    @Test
    void validate_number_validInteger_returnsTrue() {
        assertTrue(new GreaterThanCondition("age", "18", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_validDecimal_returnsTrue() {
        assertTrue(new GreaterThanCondition("score", "99.5", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_invalidString_returnsFalse() {
        assertFalse(new GreaterThanCondition("age", "abc", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_expression_returnsTrue() {
        assertTrue(new GreaterThanCondition("a", "b", ValueType.Expression).validate().isValid());
    }
}
