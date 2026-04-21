package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Condition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexConditionTest {

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

    // ---- validate ----

    @Test
    void validate_validRegex_returnsTrue() {
        assertTrue(new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+").validate().isValid());
    }

    @Test
    void validate_invalidRegex_returnsFalse() {
        assertFalse(new RegexCondition("email", "[invalid(").validate().isValid());
    }

    // ---- negate ----

    @Test
    void negate_returnsNotRegexCondition() throws Exception {
        assertInstanceOf(NegateRegexCondition.class,
                new RegexCondition("email", "\\d+").negate());
    }

    @Test
    void negate_sameRegex() throws Exception {
        RegexCondition cond = new RegexCondition("email", "\\d+");
        assertEquals("\\d+", ((NegateRegexCondition) cond.negate()).getRegex());
    }

    // ---- double negate ----

    @Test
    void doubleNegate_returnsRegexCondition() throws Exception {
        Condition doubleNegated = new RegexCondition("email", "\\d+").negate().negate();
        assertInstanceOf(RegexCondition.class, doubleNegated);
    }

    // ---- compile + execute: match ----

    @Test
    void match() throws Exception {
        RegexCondition cond = new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        cond.compile();
        assertTrue(cond.execute(ctx("email", "user@example.com")).isTrue());
    }

    @Test
    void noMatch() throws Exception {
        RegexCondition cond = new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        cond.compile();
        assertTrue(cond.execute(ctx("email", "not-an-email")).isFalse());
    }

    @Test
    void digits_matchAndNoMatch() throws Exception {
        RegexCondition cond = new RegexCondition("code", "\\d+");
        cond.compile();
        assertTrue(cond.execute(ctx("code", "12345")).isTrue());
        assertTrue(cond.execute(ctx("code", "123abc")).isFalse());
    }

    // ---- non-String value → false ----

    @Test
    void nonStringValue_returnsFalse() throws Exception {
        RegexCondition cond = new RegexCondition("age", "\\d+");
        cond.compile();
        assertTrue(cond.execute(ctx("age", 42L)).isFalse());
    }

    // ---- missing field ----

    @Test
    void missingField_defaultContext_returnsFalse() throws Exception {
        RegexCondition cond = new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        cond.compile();
        assertTrue(cond.execute(ctx("other", "user@example.com")).isFalse());
    }

    @Test
    void missingField_missableContext_returnsMiss() throws Exception {
        RegexCondition cond = new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        cond.compile();
        assertTrue(cond.execute(ctxMissable("other", "user@example.com")).isIndeterminate());
    }

    @Test
    void missingField_distinguishedFromFalse() throws Exception {
        RegexCondition cond = new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        cond.compile();
        // email present but no match → FALSE
        assertTrue(cond.execute(ctx("email", "not-an-email")).isFalse());
        // email absent + missable context → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- before: user context unmodified ----

    @Test
    void before_userContextUnmodified() throws Exception {
        RegexCondition cond = new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        cond.compile();

        Context ctx = ctx("email", "user@example.com");
        cond.execute(ctx);

        assertTrue(ctx.getParams().keySet().stream()
                .noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        RegexCondition cond = new RegexCondition("email", "\\d+");
        assertThrows(Exception.class, () -> cond.execute(ctx("email", "123")));
    }

    // ---- NotRegexCondition ----

    @Test
    void notRegex_matchAndNoMatch() throws Exception {
        NegateRegexCondition cond = (NegateRegexCondition) new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+").negate();
        cond.compile();
        assertTrue(cond.execute(ctx("email", "not-an-email")).isTrue());
        assertTrue(cond.execute(ctx("email", "user@example.com")).isFalse());
    }

    @Test
    void notRegex_nonStringValue_returnsFalse() throws Exception {
        NegateRegexCondition cond = (NegateRegexCondition) new RegexCondition("age", "\\d+").negate();
        cond.compile();
        assertTrue(cond.execute(ctx("age", 42L)).isFalse());
    }
}
