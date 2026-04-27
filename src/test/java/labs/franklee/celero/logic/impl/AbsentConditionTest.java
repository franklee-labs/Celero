package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AbsentConditionTest {

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
    void negate_returnsExistsCondition() throws Exception {
        assertInstanceOf(ExistsCondition.class, new AbsentCondition("params.age").negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        AbsentCondition cond = new AbsentCondition("params.age");
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- validate ----

    @Test
    void validate_alwaysReturnsValid() {
        assertTrue(new AbsentCondition("params.age").validate().isValid());
    }

    // ---- constructors ----

    @Test
    void constructor_withPriority_setsPriority() {
        AbsentCondition cond = new AbsentCondition("params.age", 4);
        assertEquals(4, cond.getPriority());
        assertFalse(cond.isIgnoreAbsence());
    }

    // ---- generateName ----

    @Test
    void generateName_returnsNotHasExpression() {
        AbsentCondition cond = new AbsentCondition("params.age");
        assertEquals("!has(params.age)", cond.generateName());
    }

    // ---- compile + execute: field present ----

    @Test
    void fieldPresent_returnsFalse() throws Exception {
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("age", 25L))).isFalse());
    }

    @Test
    void fieldPresent_stringValue_returnsFalse() throws Exception {
        AbsentCondition cond = new AbsentCondition("params.role");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("role", "admin"))).isFalse());
    }

    @Test
    void fieldPresent_nullValue_returnsFalse() throws Exception {
        // !has() returns false when the key exists even if the value is null
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        Map<String, Object> inner = new HashMap<>();
        inner.put("age", null);
        assertTrue(cond.execute(ctx("params", inner)).isFalse());
    }

    // ---- compile + execute: field absent ----

    @Test
    void fieldAbsent_mapKeyMissing_returnsTrue() throws Exception {
        // params present but "age" key missing → !has() returns true
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("name", "frank"))).isTrue());
    }

    @Test
    void fieldAbsent_emptyMap_returnsTrue() throws Exception {
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of())).isTrue());
    }

    // ---- compile + execute: parent variable absent ----

    @Test
    void parentAbsent_defaultContext_returnsTrue() throws Exception {
        // params not in context → absent is treated as true
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void parentAbsent_missableContext_returnsTrue() throws Exception {
        // parent absent → always true regardless of missable context
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isTrue());
    }

    @Test
    void parentAbsent_sameResultAsKeyMissing() throws Exception {
        AbsentCondition cond = new AbsentCondition("params.age");
        cond.compile();
        // params present, age key missing → TRUE
        assertTrue(cond.execute(ctx("params", Map.of("name", "frank"))).isTrue());
        // params absent → also TRUE
        assertTrue(cond.execute(ctx()).isTrue());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        AbsentCondition cond = new AbsentCondition("params.age");
        assertThrows(Exception.class, () -> cond.execute(ctx()));
    }
}
