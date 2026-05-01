package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.base.ValueType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CompareConditionTest {

    private static Context ctx(Object... kvs) {
        var m = new HashMap<String, Object>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return Context.Builder.createBuilder(RuleContext.of(m)).build();
    }

    private static Context ctxMissable(Object... kvs) {
        var m = new HashMap<String, Object>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return Context.Builder.createBuilder(RuleContext.of(m)).enableMissState().build();
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        CompareCondition cond = new CompareCondition("x", "10", ValueType.Number, "GT");
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- negate ----

    @Test
    void negate_GT_returnsLTECondition() throws Exception {
        CompareCondition negated = (CompareCondition) new CompareCondition("age", "18", ValueType.Number, "GT").negate();
        assertInstanceOf(CompareCondition.class, negated);
        negated.compile();
        assertTrue(negated.execute(ctx("age", 18L)).isTrue());
        assertTrue(negated.execute(ctx("age", 19L)).isFalse());
    }

    @Test
    void negate_GTE_returnsLTCondition() throws Exception {
        CompareCondition negated = (CompareCondition) new CompareCondition("age", "18", ValueType.Number, "GTE").negate();
        assertInstanceOf(CompareCondition.class, negated);
        negated.compile();
        assertTrue(negated.execute(ctx("age", 17L)).isTrue());
        assertTrue(negated.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void negate_LT_returnsGTECondition() throws Exception {
        CompareCondition negated = (CompareCondition) new CompareCondition("age", "18", ValueType.Number, "LT").negate();
        assertInstanceOf(CompareCondition.class, negated);
        negated.compile();
        assertTrue(negated.execute(ctx("age", 18L)).isTrue());
        assertTrue(negated.execute(ctx("age", 17L)).isFalse());
    }

    @Test
    void negate_LTE_returnsGTCondition() throws Exception {
        CompareCondition negated = (CompareCondition) new CompareCondition("age", "18", ValueType.Number, "LTE").negate();
        assertInstanceOf(CompareCondition.class, negated);
        negated.compile();
        assertTrue(negated.execute(ctx("age", 19L)).isTrue());
        assertTrue(negated.execute(ctx("age", 18L)).isFalse());
    }

    // ---- GT: compile + execute ----

    @Test
    void gt_integer_greater_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isTrue());
    }

    @Test
    void gt_integer_equal_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void gt_integer_less_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isFalse());
    }

    @Test
    void gt_decimal_greater_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("score", "99.5", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("score", 100.0)).isTrue());
    }

    @Test
    void gt_decimal_equal_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("score", "99.5", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isFalse());
    }

    @Test
    void gt_integerWithTrailingZeros_treatedAsLong() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18.00", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isTrue());
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void gt_expression_crossField() throws Exception {
        CompareCondition cond = new CompareCondition("a", "b", ValueType.Expression, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("a", 20L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isFalse());
        assertTrue(cond.execute(ctx("a", 5L, "b", 10L)).isFalse());
    }

    // ---- GTE: compile + execute ----

    @Test
    void gte_integer_greater_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isTrue());
    }

    @Test
    void gte_integer_equal_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    @Test
    void gte_integer_less_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isFalse());
    }

    @Test
    void gte_decimal_equal_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("score", "99.5", ValueType.Number, "GTE");
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isTrue());
    }

    @Test
    void gte_integerWithTrailingZeros_treatedAsLong() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18.00", ValueType.Number, "GTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
        assertTrue(cond.execute(ctx("age", 17L)).isFalse());
    }

    @Test
    void gte_expression_crossField() throws Exception {
        CompareCondition cond = new CompareCondition("a", "b", ValueType.Expression, "GTE");
        cond.compile();
        assertTrue(cond.execute(ctx("a", 20L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 5L, "b", 10L)).isFalse());
    }

    // ---- LT: compile + execute ----

    @Test
    void lt_integer_less_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isTrue());
    }

    @Test
    void lt_integer_equal_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void lt_integer_greater_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isFalse());
    }

    @Test
    void lt_decimal_less_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("score", "99.5", ValueType.Number, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.0)).isTrue());
    }

    @Test
    void lt_decimal_equal_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("score", "99.5", ValueType.Number, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isFalse());
    }

    @Test
    void lt_integerWithTrailingZeros_treatedAsLong() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18.00", ValueType.Number, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isTrue());
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void lt_expression_crossField() throws Exception {
        CompareCondition cond = new CompareCondition("a", "b", ValueType.Expression, "LT");
        cond.compile();
        assertTrue(cond.execute(ctx("a", 5L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isFalse());
        assertTrue(cond.execute(ctx("a", 20L, "b", 10L)).isFalse());
    }

    // ---- LTE: compile + execute ----

    @Test
    void lte_integer_less_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "LTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isTrue());
    }

    @Test
    void lte_integer_equal_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "LTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    @Test
    void lte_integer_greater_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "LTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 19L)).isFalse());
    }

    @Test
    void lte_decimal_equal_returnsTrue() throws Exception {
        CompareCondition cond = new CompareCondition("score", "99.5", ValueType.Number, "LTE");
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isTrue());
    }

    @Test
    void lte_integerWithTrailingZeros_treatedAsLong() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18.00", ValueType.Number, "LTE");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
        assertTrue(cond.execute(ctx("age", 19L)).isFalse());
    }

    @Test
    void lte_expression_crossField() throws Exception {
        CompareCondition cond = new CompareCondition("a", "b", ValueType.Expression, "LTE");
        cond.compile();
        assertTrue(cond.execute(ctx("a", 5L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 10L, "b", 10L)).isTrue());
        assertTrue(cond.execute(ctx("a", 20L, "b", 10L)).isFalse());
    }

    // ---- unsupported value type ----

    @Test
    void compile_unsupportedType_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> new CompareCondition("role", "admin", ValueType.String, "GT").compile());
        assertThrows(UnsupportedOperationException.class,
                () -> new CompareCondition("active", "true", ValueType.Boolean, "LT").compile());
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_defaultContext_returnsFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_missableContext_returnsIndeterminate() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 17L)).isFalse());
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- evaluate without compile ----

    @Test
    void evaluate_withoutCompile_throws() {
        CompareCondition cond = new CompareCondition("age", "18", ValueType.Number, "GT");
        assertThrows(Exception.class, () -> cond.execute(ctx("age", 20L)));
    }

    // ---- validate ----

    @Test
    void validate_number_validInteger_returnsTrue() {
        assertTrue(new CompareCondition("age", "18", ValueType.Number, "GT").validate().isValid());
    }

    @Test
    void validate_number_validDecimal_returnsTrue() {
        assertTrue(new CompareCondition("score", "99.5", ValueType.Number, "LTE").validate().isValid());
    }

    @Test
    void validate_number_invalidString_returnsFalse() {
        assertFalse(new CompareCondition("age", "abc", ValueType.Number, "GT").validate().isValid());
    }

    @Test
    void validate_expression_returnsTrue() {
        assertTrue(new CompareCondition("a", "b", ValueType.Expression, "GTE").validate().isValid());
    }
}
