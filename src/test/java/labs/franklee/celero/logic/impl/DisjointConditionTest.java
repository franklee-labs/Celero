package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.exceptions.InvalidConditionException;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DisjointConditionTest {

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
    void negate_returnsIntersectCondition() throws Exception {
        assertInstanceOf(IntersectCondition.class,
                new DisjointCondition("[\"admin\"]", "List", "[\"admin\"]", "List").negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        DisjointCondition cond = new DisjointCondition("[\"a\"]", "List", "[\"b\"]", "List");
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- constructors ----

    @Test
    void constructor_withPriority_setsPriority() {
        DisjointCondition cond = new DisjointCondition("[\"a\"]", "List", "[\"b\"]", "List", 7);
        assertEquals(7, cond.getPriority());
        assertFalse(cond.isIgnoreAbsence());
    }

    @Test
    void constructor_withPriorityAndIgnoreAbsence_setsFlags() {
        DisjointCondition cond = new DisjointCondition("[\"a\"]", "List", "[\"b\"]", "List", 7, true);
        assertEquals(7, cond.getPriority());
        assertTrue(cond.isIgnoreAbsence());
    }

    @Test
    void constructor_invalidValueTypeStr_throwsInvalidConditionException() {
        assertThrows(InvalidConditionException.class,
                () -> new DisjointCondition("[\"a\"]", "UNKNOWN", "[\"b\"]", "List"));
    }

    // ---- validate ----

    @Test
    void validate_listAndList_returnsValid() {
        assertTrue(new DisjointCondition("[\"a\"]", "List", "[\"b\"]", "List").validate().isValid());
    }

    @Test
    void validate_expressionAndExpression_returnsValid() {
        assertTrue(new DisjointCondition("list1", "Expression", "list2", "Expression").validate().isValid());
    }

    @Test
    void validate_listAndExpression_returnsValid() {
        assertTrue(new DisjointCondition("[\"a\"]", "List", "list2", "Expression").validate().isValid());
    }

    @Test
    void validate_expressionAndList_returnsValid() {
        assertTrue(new DisjointCondition("list1", "Expression", "[\"a\"]", "List").validate().isValid());
    }

    @Test
    void validate_invalidValueType1_returnsInvalid() {
        assertFalse(new DisjointCondition("[\"a\"]", "String", "[\"b\"]", "List").validate().isValid());
    }

    @Test
    void validate_invalidValueType2_returnsInvalid() {
        assertFalse(new DisjointCondition("[\"a\"]", "List", "[\"b\"]", "Number").validate().isValid());
    }

    // ---- compile + execute: List-List ----

    @Test
    void listList_noOverlap_strings_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\"]", "List", "[\"user\", \"ops\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_overlap_strings_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\", \"ops\"]", "List", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void listList_noOverlap_numbers_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("[1, 2]", "List", "[3, 4]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_overlap_numbers_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("[1, 2, 3]", "List", "[3, 4, 5]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void listList_emptyLeft_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("[]", "List", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_noOverlap_mixed_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\", 1]", "List", "[\"user\", 2]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_overlap_mixed_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\", 1, true]", "List", "[1, \"user\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    // ---- compile + execute: Expression-Expression ----

    @Test
    void expressionExpression_noOverlap_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("list1", List.of("a", "b"), "list2", List.of("c", "d"))).isTrue());
    }

    @Test
    void expressionExpression_overlap_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("list1", List.of("a", "b"), "list2", List.of("b", "c"))).isFalse());
    }

    // ---- compile + execute: List-Expression ----

    @Test
    void listExpression_noOverlap_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\"]", "List", "roles", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("user", "ops"))).isTrue());
    }

    @Test
    void listExpression_overlap_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\"]", "List", "roles", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("admin", "user"))).isFalse());
    }

    // ---- compile + execute: Expression-List ----

    @Test
    void expressionList_noOverlap_returnsTrue() throws Exception {
        DisjointCondition cond = new DisjointCondition("roles", "Expression", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("user", "ops"))).isTrue());
    }

    @Test
    void expressionList_overlap_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("roles", "Expression", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("admin", "user"))).isFalse());
    }

    // ---- builtin params not leaked into user context ----

    @Test
    void before_userContextUnmodified() throws Exception {
        DisjointCondition cond = new DisjointCondition("[\"admin\"]", "List", "[\"user\"]", "List");
        cond.compile();
        Context ctx = ctx();
        cond.execute(ctx);
        assertTrue(ctx.getParams().keySet().stream().noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_expressionVar_defaultContext_returnsFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_expressionVar_missableContext_returnsIndeterminate() throws Exception {
        DisjointCondition cond = new DisjointCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        DisjointCondition cond = new DisjointCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        // overlap → FALSE (not disjoint)
        assertTrue(cond.execute(ctx("list1", List.of("a"), "list2", List.of("a"))).isFalse());
        // var absent + missable → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        DisjointCondition cond = new DisjointCondition("[\"admin\"]", "List", "[\"user\"]", "List");
        assertThrows(Exception.class, () -> cond.execute(ctx()));
    }
}
