package labs.franklee.celero.logic.impl;

import labs.franklee.celero.logic.base.NodeType;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.helper.NonNegatableNode;
import labs.franklee.celero.logic.helper.StubCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ORTest {

    @Test
    void relation_returnsOr() {
        assertEquals(RelationType.Or, new OR().relation());
    }

    @Test
    void constructor_setsRelationNodeType() {
        assertEquals(NodeType.Relation, new OR().getType());
    }

    @Test
    void validate_falseWhenZeroChildren() {
        assertFalse(new OR().validate().isValid());
    }

    @Test
    void validate_falseWhenOneChild() {
        OR or = new OR();
        or.setChildren(List.of(new StubCondition("a")));
        assertFalse(or.validate().isValid());
    }

    @Test
    void validate_trueWhenTwoOrMoreChildren() {
        OR or = new OR();
        or.setChildren(List.of(new StubCondition("a"), new StubCondition("b")));
        assertTrue(or.validate().isValid());
    }

    // ---- resolve() ----

    @Test
    void validate_fewerThanTwoChildren_returnsInvalid() {
        OR or = new OR();
        or.setChildren(List.of(new StubCondition("a")));
        assertFalse(or.validate().isValid());
    }

    @Test
    void resolve_twoConditions_returnsTwoPaths() throws Exception {
        // OR(A, B) -> OR[ [A], [B] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        OR or = new OR();
        or.setChildren(List.of(a, b));
        Relation result = or.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getPathGroup().paths().size());
        assertSame(a, result.getPathGroup().paths().get(0).conditions().get(0));
        assertSame(b, result.getPathGroup().paths().get(1).conditions().get(0));
    }

    @Test
    void resolve_threeConditions_coversCurrentNodeAlreadySetBranch() throws Exception {
        // OR(A, B, C) -> OR[ [A], [B], [C] ]
        // The second loop iteration (i=2) hits the currentNode-already-set path
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        StubCondition c = new StubCondition("c");
        OR or = new OR();
        or.setChildren(List.of(a, b, c));
        Relation result = or.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(3, result.getPathGroup().paths().size());
    }

    @Test
    void resolve_conditionAndAndNode_unionOfPaths() throws Exception {
        // OR(A, AND(B, C)) -> OR[ [A], [B,C] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        StubCondition c = new StubCondition("c");
        AND and = new AND();
        and.setChildren(List.of(b, c));
        OR or = new OR();
        or.setChildren(List.of(a, and));
        Relation result = or.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getPathGroup().paths().size());
        assertEquals(1, result.getPathGroup().paths().get(0).conditions().size());
        assertEquals(2, result.getPathGroup().paths().get(1).conditions().size());
    }

    // ---- negate() ----

    @Test
    void negate_twoConditions_returnsUnresolvedAndWithNegatedChildren() throws Exception {
        // NOT(A OR B) = NOT(A) AND NOT(B)
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        OR or = new OR();
        or.setChildren(List.of(a, b));
        Relation result = or.negate();
        assertEquals(RelationType.And, result.relation());
        assertEquals(2, result.getChildren().size());
    }

    @Test
    void negate_resolvedResult_isAndWithNegatedPath() throws Exception {
        // NOT(A OR B).resolve() = AND[ [NOT(A), NOT(B)] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        OR or = new OR();
        or.setChildren(List.of(a, b));
        Relation result = or.negate().resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertEquals(2, result.getPathGroup().paths().get(0).conditions().size());
    }

    @Test
    void negate_nonNegatableChild_throwsException() {
        // A child that does not implement Negatable must cause negate() to fail fast
        StubCondition a = new StubCondition("a");
        NonNegatableNode nonNeg = new NonNegatableNode();
        OR or = new OR();
        or.setChildren(List.of(a, nonNeg));
        assertThrows(UnsupportedOperationException.class, or::negate);
    }
}
