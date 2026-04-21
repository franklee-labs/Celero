package labs.franklee.celero.logic.impl;

import labs.franklee.celero.logic.base.NodeType;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.helper.NonNegatableNode;
import labs.franklee.celero.logic.helper.StubCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NOTTest {

    @Test
    void relation_returnsNot() {
        assertEquals(RelationType.Not, new NOT().relation());
    }

    @Test
    void constructor_setsRelationNodeType() {
        assertEquals(NodeType.Relation, new NOT().getType());
    }

    @Test
    void validate_falseWhenZeroChildren() {
        assertFalse(new NOT().validate().isValid());
    }

    @Test
    void validate_trueWhenExactlyOneChild() {
        NOT not = new NOT();
        not.setChildren(List.of(new StubCondition("a")));
        assertTrue(not.validate().isValid());
    }

    @Test
    void validate_falseWhenTwoChildren() {
        NOT not = new NOT();
        not.setChildren(List.of(new StubCondition("a"), new StubCondition("b")));
        assertFalse(not.validate().isValid());
    }

    // ---- resolve() ----

    @Test
    void resolve_zeroChildren_throwsException() {
        assertThrows(RuntimeException.class, () -> new NOT().resolve());
    }

    @Test
    void validate_twoChildren_returnsInvalid() {
        NOT not = new NOT();
        not.setChildren(List.of(new StubCondition("a"), new StubCondition("b")));
        assertFalse(not.validate().isValid());
    }

    @Test
    void resolve_andChild_returnsOrWithNegatedPaths() throws Exception {
        // NOT(AND(A, B)) = OR[ [NOT(A)], [NOT(B)] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        AND and = new AND();
        and.setChildren(List.of(a, b));
        NOT not = new NOT();
        not.setChildren(List.of(and));
        Relation result = not.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getPathGroup().paths().size());
        assertEquals(1, result.getPathGroup().paths().get(0).conditions().size());
        assertEquals(1, result.getPathGroup().paths().get(1).conditions().size());
    }

    @Test
    void resolve_orChild_returnsAndWithNegatedPath() throws Exception {
        // NOT(OR(A, B)) = AND[ [NOT(A), NOT(B)] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        OR or = new OR();
        or.setChildren(List.of(a, b));
        NOT not = new NOT();
        not.setChildren(List.of(or));
        Relation result = not.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertEquals(2, result.getPathGroup().paths().get(0).conditions().size());
    }

    @Test
    void resolve_conditionChild_returnsAndWithNegatedCondition() throws Exception {
        // NOT(A) = AND[ [NOT(A)] ]
        StubCondition a = new StubCondition("a");
        NOT not = new NOT();
        not.setChildren(List.of(a));
        Relation result = not.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertEquals(1, result.getPathGroup().paths().get(0).conditions().size());
    }

    @Test
    void resolve_nonNegatableChild_throwsException() {
        // A child that does not implement Negatable triggers the unsupported-type exception
        NOT not = new NOT();
        not.setChildren(List.of(new NonNegatableNode()));
        assertThrows(UnsupportedOperationException.class, not::resolve);
    }

    // ---- negate() ----

    @Test
    void negate_relationChild_returnsInnerRelationDirectly() throws Exception {
        // NOT.negate() with a Relation child performs double-negation elimination
        AND inner = new AND();
        inner.setChildren(List.of(new StubCondition("a")));
        NOT not = new NOT();
        not.setChildren(List.of(inner));
        Relation result = not.negate();
        assertSame(inner, result);
    }

    @Test
    void negate_conditionChild_returnsUnresolvedAndWithConditionAsChild() throws Exception {
        // NOT.negate() with a Condition child returns an unresolved AND (children set, no pathGroup)
        // so the caller can chain .resolve() — consistent with AND.negate() and OR.negate()
        StubCondition c = new StubCondition("a");
        NOT not = new NOT();
        not.setChildren(List.of(c));
        Relation result = not.negate();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getChildren().size());
        assertSame(c, result.getChildren().get(0));
    }
}
