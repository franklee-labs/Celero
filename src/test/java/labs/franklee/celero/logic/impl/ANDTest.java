package labs.franklee.celero.logic.impl;

import labs.franklee.celero.logic.base.Node;
import labs.franklee.celero.logic.path.PathGroup;
import labs.franklee.celero.logic.base.NodeType;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.RelationType;
import labs.franklee.celero.logic.helper.NonNegatableNode;
import labs.franklee.celero.logic.helper.StubCondition;
import labs.franklee.celero.logic.path.Path;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ANDTest {

    // ---- Node base class coverage ----

    @Test
    void node_idGetterAndSetter() {
        AND and = new AND();
        and.setId("test-id");
        assertEquals("test-id", and.getId());
    }

    @Test
    void constructor_default_setsRelationNodeType() {
        assertEquals(NodeType.Relation, new AND().getType());
    }

    @Test
    void constructor_withPath_setsRelationNodeType() {
        AND and = new AND(new Path());
        assertEquals(NodeType.Relation, and.getType());
    }


    @Test
    void node_childrenGetterAndSetter() {
        AND and = new AND();
        List<Node> children = List.of(new StubCondition("x"));
        and.setChildren(children);
        assertSame(children, and.getChildren());
    }

    // ---- Relation base class coverage ----

    @Test
    void relation_pathGroupGetterAndSetter() {
        AND and = new AND();
        PathGroup pg = new PathGroup();
        and.setPathGroup(pg);
        assertSame(pg, and.getPathGroup());
    }

    // ---- AND-specific tests ----

    @Test
    void relation_returnsAnd() {
        assertEquals(RelationType.And, new AND().relation());
    }

    @Test
    void validate_falseWhenChildrenEmpty() {
        assertFalse(new AND().validate().isValid());
    }

    @Test
    void validate_trueWhenChildrenNotEmpty() {
        AND and = new AND();
        and.setChildren(List.of(new StubCondition("a")));
        assertTrue(and.validate().isValid());
    }

    @Test
    void constructor_withPath_setsPathGroup() {
        StubCondition c = new StubCondition("a");
        Path path = new Path().addCondition(c);
        AND and = new AND(path);
        assertNotNull(and.getPathGroup());
        assertEquals(1, and.getPathGroup().paths().size());
        assertSame(c, and.getPathGroup().paths().get(0).conditions().get(0));
    }

    // ---- resolve() ----

    @Test
    void validate_emptyChildren_returnsInvalid() {
        assertFalse(new AND().validate().isValid());
    }

    @Test
    void resolve_singleConditionChild_returnsAndWithOnePath() throws Exception {
        StubCondition a = new StubCondition("a");
        AND and = new AND();
        and.setChildren(List.of(a));
        Relation result = and.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertSame(a, result.getPathGroup().paths().get(0).conditions().get(0));
    }

    @Test
    void resolve_singleRelationChild_delegatesToChildResolve() throws Exception {
        // AND wrapping another AND: outer delegates to inner
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        AND inner = new AND();
        inner.setChildren(List.of(a, b));
        AND outer = new AND();
        outer.setChildren(List.of(inner));
        Relation result = outer.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(2, result.getPathGroup().paths().get(0).conditions().size());
    }

    @Test
    void resolve_andPlusAnd_mergesIntoSinglePath() throws Exception {
        // AND(A, B) -> AND[ [A, B] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        AND and = new AND();
        and.setChildren(List.of(a, b));
        Relation result = and.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertEquals(2, result.getPathGroup().paths().get(0).conditions().size());
    }

    @Test
    void resolve_andPlusOr_expandsToPaths() throws Exception {
        // AND(A, OR(B, C)) -> OR[ [A,B], [A,C] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        StubCondition c = new StubCondition("c");
        OR or = new OR();
        or.setChildren(List.of(b, c));
        AND and = new AND();
        and.setChildren(List.of(a, or));
        Relation result = and.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getPathGroup().paths().size());
        assertEquals(2, result.getPathGroup().paths().get(0).conditions().size());
        assertEquals(2, result.getPathGroup().paths().get(1).conditions().size());
    }

    @Test
    void resolve_orPlusAnd_mergesOrPathsWithCondition() throws Exception {
        // AND(OR(A, B), C) -> OR[ [A,C], [B,C] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        StubCondition c = new StubCondition("c");
        OR or = new OR();
        or.setChildren(List.of(a, b));
        AND and = new AND();
        and.setChildren(List.of(or, c));
        Relation result = and.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getPathGroup().paths().size());
        assertEquals(2, result.getPathGroup().paths().get(0).conditions().size());
    }

    @Test
    void resolve_orPlusOr_cartesianProduct() throws Exception {
        // AND(OR(A,B), OR(C,D)) -> OR[ [A,C], [A,D], [B,C], [B,D] ]
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        StubCondition c = new StubCondition("c");
        StubCondition d = new StubCondition("d");
        OR or1 = new OR();
        or1.setChildren(List.of(a, b));
        OR or2 = new OR();
        or2.setChildren(List.of(c, d));
        AND and = new AND();
        and.setChildren(List.of(or1, or2));
        Relation result = and.resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(4, result.getPathGroup().paths().size());
        result.getPathGroup().paths().forEach(p -> assertEquals(2, p.conditions().size()));
    }

    @Test
    void resolve_threeConditions_coversCurrentNodeAlreadySetBranch() throws Exception {
        // AND(A, B, C) -> AND[ [A, B, C] ]
        // The second loop iteration (i=2) hits the currentNode-already-set path
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        StubCondition c = new StubCondition("c");
        AND and = new AND();
        and.setChildren(List.of(a, b, c));
        Relation result = and.resolve();
        assertEquals(RelationType.And, result.relation());
        assertEquals(1, result.getPathGroup().paths().size());
        assertEquals(3, result.getPathGroup().paths().get(0).conditions().size());
    }

    // ---- negate() ----

    @Test
    void negate_twoConditions_returnsUnresolvedOrWithNegatedChildren() throws Exception {
        // NOT(A AND B) = A_neg OR B_neg
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        AND and = new AND();
        and.setChildren(List.of(a, b));
        Relation result = and.negate();
        // negate() returns unresolved OR; caller is responsible for resolve()
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getChildren().size());
    }

    @Test
    void negate_resolvedResult_isOrWithNegatedPaths() throws Exception {
        // Verify that negate().resolve() produces the correct PathGroup
        StubCondition a = new StubCondition("a");
        StubCondition b = new StubCondition("b");
        AND and = new AND();
        and.setChildren(List.of(a, b));
        Relation result = and.negate().resolve();
        assertEquals(RelationType.Or, result.relation());
        assertEquals(2, result.getPathGroup().paths().size());
    }

    @Test
    void negate_nonNegatableChild_throwsException() {
        // A child that does not implement Negatable must cause negate() to fail fast
        StubCondition a = new StubCondition("a");
        NonNegatableNode nonNeg = new NonNegatableNode();
        AND and = new AND();
        and.setChildren(List.of(a, nonNeg));
        assertThrows(UnsupportedOperationException.class, and::negate);
    }
}
