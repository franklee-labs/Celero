package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExistsConditionTest {

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
    void negate_returnsAbsentCondition() throws Exception {
        assertInstanceOf(AbsentCondition.class, new ExistsCondition("params.age").negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        ExistsCondition cond = new ExistsCondition("params.age");
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- validate ----

    @Test
    void validate_alwaysReturnsValid() {
        assertTrue(new ExistsCondition("params.age").validate().isValid());
    }

    // ---- constructors ----

    @Test
    void constructor_withPriority_setsPriority() {
        ExistsCondition cond = new ExistsCondition("params.age", 3);
        assertEquals(3, cond.getPriority());
        assertFalse(cond.isIgnoreAbsence());
    }

    // ---- generateName ----

    @Test
    void generateName_returnsHasExpression() {
        ExistsCondition cond = new ExistsCondition("params.age");
        assertEquals("has(params.age)", cond.generateName());
    }

    // ---- compile + execute: field present ----

    @Test
    void fieldPresent_returnsTrue() throws Exception {
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("age", 25L))).isTrue());
    }

    @Test
    void fieldPresent_stringValue_returnsTrue() throws Exception {
        ExistsCondition cond = new ExistsCondition("params.role");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("role", "admin"))).isTrue());
    }

    @Test
    void fieldPresent_listValue_returnsTrue() throws Exception {
        ExistsCondition cond = new ExistsCondition("params.tags");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("tags", java.util.List.of("a", "b")))).isTrue());
    }

    @Test
    void fieldPresent_nullValue_returnsTrue() throws Exception {
        // has() returns true when the key exists even if the value is null
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        Map<String, Object> inner = new HashMap<>();
        inner.put("age", null);
        assertTrue(cond.execute(ctx("params", inner)).isTrue());
    }

    // ---- compile + execute: field absent ----

    @Test
    void fieldAbsent_mapKeyMissing_returnsFalse() throws Exception {
        // params is present but "age" key is missing → has() returns false without exception
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of("name", "frank"))).isFalse());
    }

    @Test
    void fieldAbsent_emptyMap_returnsFalse() throws Exception {
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx("params", Map.of())).isFalse());
    }

    // ---- compile + execute: parent variable absent ----

    @Test
    void parentAbsent_defaultContext_returnsFalse() throws Exception {
        // params not in context → CelUnknownSet → MissingParameterException → FALSE
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void parentAbsent_missableContext_returnsFalse() throws Exception {
        // parent absent → always false regardless of missable context
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isFalse());
    }

    @Test
    void parentAbsent_sameResultAsKeyMissing() throws Exception {
        ExistsCondition cond = new ExistsCondition("params.age");
        cond.compile();
        // params present, age key missing → FALSE
        assertTrue(cond.execute(ctx("params", Map.of("name", "frank"))).isFalse());
        // params absent → also FALSE
        assertTrue(cond.execute(ctx()).isFalse());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        ExistsCondition cond = new ExistsCondition("params.age");
        assertThrows(Exception.class, () -> cond.execute(ctx()));
    }
}
