package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.base.ValueType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LessThanOrEqualConditionTest {

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
    void negate_returnsGreaterThanCondition() throws Exception {
        assertInstanceOf(GreaterThanCondition.class,
                new LessThanOrEqualCondition("age", "18", ValueType.Number).negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("x", "10", ValueType.Number);
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- compile + execute: Number (integer) ----

    @Test
    void number_integer_less_returnsTrue() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isTrue());
    }

    @Test
    void number_integer_equal_returnsTrue() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    @Test
    void number_integer_greater_returnsFalse() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isFalse());
    }

    @Test
    void number_integerWithTrailingZeros_treatedAsLong() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18.00", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
        assertTrue(cond.execute(ctx("age", 19L)).isFalse());
    }

    // ---- compile + execute: Number (decimal) ----

    @Test
    void number_decimal_less_returnsTrue() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.0)).isTrue());
    }

    @Test
    void number_decimal_equal_returnsTrue() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isTrue());
    }

    @Test
    void number_decimal_greater_returnsFalse() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("score", "99.5", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 100.0)).isFalse());
    }

    // ---- compile + execute: Expression ----

    @Test
    void expression_crossField() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("a", "b", ValueType.Expression);
        cond.compile();
        assertTrue(cond.execute(ctx("a", 5L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 20L, "b", 10L)).isFalse());
    }

    // ---- unsupported type ----

    @Test
    void compile_unsupportedType_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> new LessThanOrEqualCondition("role", "admin", ValueType.String).compile());
        assertThrows(UnsupportedOperationException.class,
                () -> new LessThanOrEqualCondition("active", "true", ValueType.Boolean).compile());
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_defaultContext_returnsFalse() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_missableContext_returnsMiss() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        cond.compile();
        // age=19 → FALSE (greater than 18)
        assertTrue(cond.execute(ctx("age", 19L)).isFalse());
        // age absent + missable context → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        LessThanOrEqualCondition cond = new LessThanOrEqualCondition("age", "18", ValueType.Number);
        assertThrows(Exception.class, () -> cond.execute(ctx("age", 17L)));
    }

    // ---- validate ----

    @Test
    void validate_number_validInteger_returnsTrue() {
        assertTrue(new LessThanOrEqualCondition("age", "18", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_validDecimal_returnsTrue() {
        assertTrue(new LessThanOrEqualCondition("score", "99.5", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_number_invalidString_returnsFalse() {
        assertFalse(new LessThanOrEqualCondition("age", "abc", ValueType.Number).validate().isValid());
    }

    @Test
    void validate_expression_returnsTrue() {
        assertTrue(new LessThanOrEqualCondition("a", "b", ValueType.Expression).validate().isValid());
    }
}
