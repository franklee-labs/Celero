package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InConditionTest {

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
    void negate_returnsNotInCondition() throws Exception {
        assertInstanceOf(NotInCondition.class,
                new InCondition("role", List.of("admin")).negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        InCondition cond = new InCondition("role", List.of("admin"));
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- validate ----

    @Test
    void validate_nullList_returnsFalse() {
        assertFalse(new InCondition("role", null).validate().isValid());
    }

    @Test
    void validate_emptyList_returnsFalse() {
        assertFalse(new InCondition("role", List.of()).validate().isValid());
    }

    @Test
    void validate_nonEmptyList_returnsTrue() {
        assertTrue(new InCondition("role", List.of("admin")).validate().isValid());
    }

    // ---- compile + execute: String ----

    @Test
    void string_match_returnsTrue() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin", "ops"));
        cond.compile();
        assertTrue(cond.execute(ctx("role", "admin")).isTrue());
        assertTrue(cond.execute(ctx("role", "ops")).isTrue());
    }

    @Test
    void string_noMatch_returnsFalse() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin", "ops"));
        cond.compile();
        assertTrue(cond.execute(ctx("role", "user")).isFalse());
    }

    @Test
    void string_singleElement_match() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin"));
        cond.compile();
        assertTrue(cond.execute(ctx("role", "admin")).isTrue());
        assertTrue(cond.execute(ctx("role", "ops")).isFalse());
    }

    // ---- compile + execute: Number (Long) ----

    @Test
    void long_match_returnsTrue() throws Exception {
        InCondition cond = new InCondition("age", List.of(18L, 25L, 30L));
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
        assertTrue(cond.execute(ctx("age", 30L)).isTrue());
    }

    @Test
    void long_noMatch_returnsFalse() throws Exception {
        InCondition cond = new InCondition("age", List.of(18L, 25L, 30L));
        cond.compile();
        assertTrue(cond.execute(ctx("age", 20L)).isFalse());
    }

    // ---- compile + execute: Number (Double) ----

    @Test
    void double_match_returnsTrue() throws Exception {
        InCondition cond = new InCondition("score", List.of(99.5, 88.0));
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isTrue());
    }

    @Test
    void double_noMatch_returnsFalse() throws Exception {
        InCondition cond = new InCondition("score", List.of(99.5, 88.0));
        cond.compile();
        assertTrue(cond.execute(ctx("score", 70.0)).isFalse());
    }

    // ---- compile + execute: Boolean ----

    @Test
    void boolean_match_returnsTrue() throws Exception {
        InCondition cond = new InCondition("flag", List.of(true, false));
        cond.compile();
        assertTrue(cond.execute(ctx("flag", true)).isTrue());
        assertTrue(cond.execute(ctx("flag", false)).isTrue());
    }

    @Test
    void boolean_singleTrue_noMatch_returnsFalse() throws Exception {
        InCondition cond = new InCondition("flag", List.of(true));
        cond.compile();
        assertTrue(cond.execute(ctx("flag", false)).isFalse());
    }

    // ---- compile + execute: mixed types ----

    @Test
    void mixed_eachTypeMatches() throws Exception {
        InCondition cond = new InCondition("val", List.of("admin", 18L, 99.5, true));
        cond.compile();
        assertTrue(cond.execute(ctx("val", "admin")).isTrue());
        assertTrue(cond.execute(ctx("val", 18L)).isTrue());
        assertTrue(cond.execute(ctx("val", 99.5)).isTrue());
        assertTrue(cond.execute(ctx("val", true)).isTrue());
    }

    @Test
    void mixed_noMatch_returnsFalse() throws Exception {
        InCondition cond = new InCondition("val", List.of("admin", 18L, 99.5, true));
        cond.compile();
        assertTrue(cond.execute(ctx("val", "unknown")).isFalse());
    }

    // ---- cross-type: Long field vs Double list, Double field vs Long list ----

    @Test
    void longField_againstDoubleList_sameNumericValue_returnsTrue() throws Exception {
        // CEL performs numeric coercion between int and double: 18L == 18.0
        InCondition cond = new InCondition("age", List.of(18.0, 25.0));
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    @Test
    void doubleField_againstLongList_sameNumericValue_returnsTrue() throws Exception {
        // CEL performs numeric coercion between int and double: 18.0 == 18L
        InCondition cond = new InCondition("score", List.of(18L, 25L));
        cond.compile();
        assertTrue(cond.execute(ctx("score", 18.0)).isTrue());
    }

    // ---- BigDecimal normalization (deserialization scenario) ----

    @Test
    void bigDecimal_integer_normalizedToLong() throws Exception {
        // BigDecimal("18.00") should normalize to 18L
        InCondition cond = new InCondition("age", List.of(new BigDecimal("18.00"), new BigDecimal("25")));
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
        assertTrue(cond.execute(ctx("age", 20L)).isFalse());
    }

    @Test
    void bigDecimal_decimal_normalizedToDouble() throws Exception {
        // BigDecimal("99.50") should normalize to 99.5
        InCondition cond = new InCondition("score", List.of(new BigDecimal("99.50"), new BigDecimal("88.0")));
        cond.compile();
        assertTrue(cond.execute(ctx("score", 99.5)).isTrue());
        assertTrue(cond.execute(ctx("score", 70.0)).isFalse());
    }

    @Test
    void integer_normalizedToLong() throws Exception {
        // Integer should normalize to Long
        InCondition cond = new InCondition("age", List.of((Object) Integer.valueOf(18)));
        cond.compile();
        assertTrue(cond.execute(ctx("age", 18L)).isTrue());
    }

    // ---- before: builtin params not leaked into user context ----

    @Test
    void before_userContextUnmodified() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin", "ops"));
        cond.compile();

        Context ctx = ctx("role", "admin");
        cond.execute(ctx);

        assertTrue(ctx.getParams().keySet().stream()
                .noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_defaultContext_returnsFalse() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin", "ops"));
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_missableContext_returnsMiss() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin", "ops"));
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        InCondition cond = new InCondition("role", List.of("admin", "ops"));
        cond.compile();
        // role="user" → FALSE (not in list)
        assertTrue(cond.execute(ctx("role", "user")).isFalse());
        // role absent + missable context → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        InCondition cond = new InCondition("role", List.of("admin"));
        assertThrows(Exception.class, () -> cond.execute(ctx("role", "admin")));
    }
}
