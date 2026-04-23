package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.exceptions.InvalidConditionException;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.base.ValueType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotInConditionTest {

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
    void negate_returnsInCondition() throws Exception {
        assertInstanceOf(InCondition.class,
                new NotInCondition("role", "[\"admin\"]", ValueType.List).negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        NotInCondition cond = new NotInCondition("role", "[\"admin\"]", ValueType.List);
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- validate ----

    @Test
    void validate_nullList_returnsFalse() {
        assertThrows(InvalidConditionException.class, () -> new NotInCondition("role", null, ValueType.List).validate().isValid());
    }

    @Test
    void validate_emptyList_returnsFalse() {
        assertFalse(new NotInCondition("role", "[]", ValueType.List).validate().isValid());
    }

    @Test
    void validate_nonEmptyList_returnsTrue() {
        assertTrue(new NotInCondition("role", "[\"admin\"]", ValueType.List).validate().isValid());
    }

    // ---- compile + execute: String ----

    @Test
    void string_inList_returnsFalse() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\", \"ops\"]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "admin")).isFalse());
        assertTrue(cond.execute(ctx("role", "ops")).isFalse());
    }

    @Test
    void string_notInList_returnsTrue() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\", \"ops\"]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "user")).isTrue());
    }

    @Test
    void string_singleElement_notMatch_returnsTrue() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\"]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("role", "ops")).isTrue());
        assertTrue(cond.execute(ctx("role", "admin")).isFalse());
    }

    // ---- compile + execute: Number (Long) ----

    @Test
    void long_inList_returnsFalse() throws Exception {
        NotInCondition cond = new NotInCondition("age", "[18, 25, 30]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void long_notInList_returnsTrue() throws Exception {
        NotInCondition cond = new NotInCondition("age", "[18, 25, 30]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 20L)).isTrue());
    }

    // ---- compile + execute: Number (Double) ----

    @Test
    void double_inList_returnsFalse() throws Exception {
        NotInCondition cond = new NotInCondition("score", "[99.5, 88.0]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isFalse());
    }

    @Test
    void double_notInList_returnsTrue() throws Exception {
        NotInCondition cond = new NotInCondition("score", "[99.5, 88.0]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 70.0)).isTrue());
    }

    // ---- compile + execute: Boolean ----

    @Test
    void boolean_inList_returnsFalse() throws Exception {
        NotInCondition cond = new NotInCondition("flag", "[true]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("flag", true)).isFalse());
    }

    @Test
    void boolean_notInList_returnsTrue() throws Exception {
        NotInCondition cond = new NotInCondition("flag", "[true]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("flag", false)).isTrue());
    }

    // ---- compile + execute: mixed types ----

    @Test
    void mixed_inList_returnsFalse() throws Exception {
        NotInCondition cond = new NotInCondition("val", "[\"admin\", 18, 99.5, true]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("val", "admin")).isFalse());
        assertTrue(cond.execute(ctx("val", 18L)).isFalse());
        assertTrue(cond.execute(ctx("val", 99.5)).isFalse());
        assertTrue(cond.execute(ctx("val", true)).isFalse());
    }

    @Test
    void mixed_notInList_returnsTrue() throws Exception {
        NotInCondition cond = new NotInCondition("val", "[\"admin\", 18, 99.5, true]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("val", "unknown")).isTrue());
    }

    // ---- cross-type: Long field vs Double list, Double field vs Long list ----

    @Test
    void longField_againstDoubleList_sameNumericValue_returnsFalse() throws Exception {
        // CEL performs numeric coercion between int and double: 18L == 18.0 → in list → not-in is false
        NotInCondition cond = new NotInCondition("age", "[18.0, 25.0]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isFalse());
    }

    @Test
    void doubleField_againstLongList_sameNumericValue_returnsFalse() throws Exception {
        // CEL performs numeric coercion between int and double: 18.0 == 18L → in list → not-in is false
        NotInCondition cond = new NotInCondition("score", "[18, 25]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx("score", 18.0)).isFalse());
    }

    // ---- BigDecimal ----

    @Test
    void bigDecimal_integer_normalizedToLong() throws Exception {
        // BigDecimal is not supported by CEL
        NotInCondition cond = new NotInCondition("age", "value", ValueType.Expression);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L, "value", List.of(new BigDecimal("18.00"), new BigDecimal("25")))).isTrue());
        assertTrue(cond.execute(ctx("age", 25, "value", List.of(new BigDecimal("18.00"), new BigDecimal("25")))).isTrue());
    }


    @Test
    void integer_normalizedToLong() throws Exception {
        NotInCondition cond = new NotInCondition("age", "value", ValueType.Expression);
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L, "value", List.of((Object) Integer.valueOf(18)))).isFalse());
        assertTrue(cond.execute(ctx("age", 20L, "value", List.of((Object) Integer.valueOf(18)))).isTrue());
    }

    // ---- before: builtin params not leaked into user context ----

    @Test
    void before_userContextUnmodified() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\", \"ops\"]", ValueType.List);
        cond.compile();

        Context ctx = ctx("role", "user");
        cond.execute(ctx);

        assertTrue(ctx.getParams().keySet().stream()
                .noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_defaultContext_returnsFalse() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\", \"ops\"]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_missableContext_returnsMiss() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\", \"ops\"]", ValueType.List);
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        NotInCondition cond = new NotInCondition("role", "[\"admin\", \"ops\"]", ValueType.List);
        cond.compile();
        // role="admin" → FALSE (in list, so not-in is false)
        assertTrue(cond.execute(ctx("role", "admin")).isFalse());
        // role absent + missable context → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        NotInCondition cond = new NotInCondition("role", "[\"admin\"]", ValueType.List);
        assertThrows(Exception.class, () -> cond.execute(ctx("role", "admin")));
    }

    // expression

    @Test
    void expression_validate_true() {
        NotInCondition cond = new NotInCondition("role", "value", ValueType.Expression);
        assertTrue(cond.validate().isValid());
    }


    // ---- invalid condition → throws ----

    @Test
    void invalid_nullValue_throws() {
        assertThrows(InvalidConditionException.class, () -> new NotInCondition("role", null, ValueType.List));
    }

    @Test
    void invalid_nullValue_withPriority_throws() {
        assertThrows(InvalidConditionException.class, () -> new NotInCondition("role", null, ValueType.List, 10));
    }

    @Test
    void invalid_valueType_throws() {
        assertFalse(new NotInCondition("role", "[\"admin\"]", ValueType.String).validate().isValid());
    }

    @Test
    void invalid_jsonList_withPriority_throws() {
        assertThrows(InvalidConditionException.class, () -> new NotInCondition("role", "[18L]", ValueType.List, 10));
    }

    @Test
    void invalid_jsonList_throws() {
        assertThrows(InvalidConditionException.class, () -> new NotInCondition("role", "[18L]", ValueType.List));
    }
}
