package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.exceptions.InvalidConditionException;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntersectConditionTest {

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
    void negate_returnsDisjointCondition() throws Exception {
        assertInstanceOf(DisjointCondition.class,
                new IntersectCondition("[\"admin\"]", "List", "[\"admin\"]", "List").negate());
    }

    // ---- resolve ----

    @Test
    void resolve_returnsAndContainingSelf() {
        IntersectCondition cond = new IntersectCondition("[\"a\"]", "List", "[\"a\"]", "List");
        Relation result = cond.resolve();
        assertEquals(RelationType.And, result.relation());
        assertSame(cond, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- constructors ----

    @Test
    void constructor_withPriority_setsPriority() {
        IntersectCondition cond = new IntersectCondition("[\"a\"]", "List", "[\"b\"]", "List", 5);
        assertEquals(5, cond.getPriority());
        assertFalse(cond.isIgnoreAbsence());
    }

    @Test
    void constructor_withPriorityAndIgnoreAbsence_setsFlags() {
        IntersectCondition cond = new IntersectCondition("[\"a\"]", "List", "[\"b\"]", "List", 5, true);
        assertEquals(5, cond.getPriority());
        assertTrue(cond.isIgnoreAbsence());
    }

    @Test
    void constructor_invalidValueTypeStr_throwsInvalidConditionException() {
        assertThrows(InvalidConditionException.class,
                () -> new IntersectCondition("[\"a\"]", "UNKNOWN", "[\"b\"]", "List"));
    }

    // ---- validate ----

    @Test
    void validate_listAndList_returnsValid() {
        assertTrue(new IntersectCondition("[\"a\"]", "List", "[\"b\"]", "List").validate().isValid());
    }

    @Test
    void validate_expressionAndExpression_returnsValid() {
        assertTrue(new IntersectCondition("list1", "Expression", "list2", "Expression").validate().isValid());
    }

    @Test
    void validate_listAndExpression_returnsValid() {
        assertTrue(new IntersectCondition("[\"a\"]", "List", "list2", "Expression").validate().isValid());
    }

    @Test
    void validate_expressionAndList_returnsValid() {
        assertTrue(new IntersectCondition("list1", "Expression", "[\"a\"]", "List").validate().isValid());
    }

    @Test
    void validate_invalidValueType1_returnsInvalid() {
        assertFalse(new IntersectCondition("[\"a\"]", "String", "[\"b\"]", "List").validate().isValid());
    }

    @Test
    void validate_invalidValueType2_returnsInvalid() {
        assertFalse(new IntersectCondition("[\"a\"]", "List", "[\"b\"]", "Number").validate().isValid());
    }

    // ---- compile + execute: List-List ----

    @Test
    void listList_overlap_strings_returnsTrue() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\", \"ops\"]", "List", "[\"admin\", \"user\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_noOverlap_strings_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\"]", "List", "[\"user\", \"ops\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void listList_overlap_numbers_returnsTrue() throws Exception {
        IntersectCondition cond = new IntersectCondition("[1, 2, 3]", "List", "[3, 4, 5]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_noOverlap_numbers_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("[1, 2]", "List", "[3, 4]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void listList_emptyLeft_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("[]", "List", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void listList_overlap_mixed_returnsTrue() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\", 1, true]", "List", "[1, \"user\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isTrue());
    }

    @Test
    void listList_noOverlap_mixed_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\", 1]", "List", "[\"user\", 2]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    // ---- compile + execute: Expression-Expression ----

    @Test
    void expressionExpression_overlap_returnsTrue() throws Exception {
        IntersectCondition cond = new IntersectCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("list1", List.of("a", "b"), "list2", List.of("b", "c"))).isTrue());
    }

    @Test
    void expressionExpression_noOverlap_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("list1", List.of("a", "b"), "list2", List.of("c", "d"))).isFalse());
    }

    // ---- compile + execute: List-Expression ----

    @Test
    void listExpression_overlap_returnsTrue() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\"]", "List", "roles", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("admin", "user"))).isTrue());
    }

    @Test
    void listExpression_noOverlap_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\"]", "List", "roles", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("user", "ops"))).isFalse());
    }

    // ---- compile + execute: Expression-List ----

    @Test
    void expressionList_overlap_returnsTrue() throws Exception {
        IntersectCondition cond = new IntersectCondition("roles", "Expression", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("admin", "user"))).isTrue());
    }

    @Test
    void expressionList_noOverlap_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("roles", "Expression", "[\"admin\"]", "List");
        cond.compile();
        assertTrue(cond.execute(ctx("roles", List.of("user", "ops"))).isFalse());
    }

    // ---- builtin params not leaked into user context ----

    @Test
    void before_userContextUnmodified() throws Exception {
        IntersectCondition cond = new IntersectCondition("[\"admin\"]", "List", "[\"admin\"]", "List");
        cond.compile();
        Context ctx = ctx();
        cond.execute(ctx);
        assertTrue(ctx.getParams().keySet().stream().noneMatch(k -> k.startsWith(Constant.BUILTIN_KEY)));
    }

    // ---- missing parameter ----

    @Test
    void missingParameter_expressionVar_defaultContext_returnsFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctx()).isFalse());
    }

    @Test
    void missingParameter_expressionVar_missableContext_returnsIndeterminate() throws Exception {
        IntersectCondition cond = new IntersectCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    @Test
    void missingParameter_distinguishedFromFalse() throws Exception {
        IntersectCondition cond = new IntersectCondition("list1", "Expression", "list2", "Expression");
        cond.compile();
        // no overlap → FALSE
        assertTrue(cond.execute(ctx("list1", List.of("a"), "list2", List.of("b"))).isFalse());
        // var absent + missable → MISS (distinct from FALSE)
        assertTrue(cond.execute(ctxMissable()).isIndeterminate());
    }

    // ---- compile not called → throws ----

    @Test
    void evaluate_withoutCompile_throws() {
        IntersectCondition cond = new IntersectCondition("[\"admin\"]", "List", "[\"admin\"]", "List");
        assertThrows(Exception.class, () -> cond.execute(ctx()));
    }
}
