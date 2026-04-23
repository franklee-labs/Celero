package labs.franklee.celero.logic.impl;

import labs.franklee.celero.logic.base.Priority;
import labs.franklee.celero.logic.base.ValueType;
import labs.franklee.celero.logic.helper.StubCondition;
import labs.franklee.celero.logic.path.Path;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionPriorityTest {

    // ---- Priority constants ----

    @Test
    void priority_constants_ordering() {
        assertTrue(Priority.HIGHEST < Priority.DEFAULT);
        assertTrue(Priority.DEFAULT < Priority.LOWEST);
        assertEquals(0, Priority.DEFAULT);
        assertEquals(Integer.MIN_VALUE, Priority.HIGHEST);
        assertEquals(Integer.MAX_VALUE, Priority.LOWEST);
    }

    // ---- getPriority: explicit value ----

    @Test
    void getPriority_equalCondition_returnsExplicitPriority() {
        assertEquals(5, new EqualCondition("x", "1", ValueType.Number, 5).getPriority());
    }

    @Test
    void getPriority_greaterThanCondition_returnsExplicitPriority() {
        assertEquals(10, new GreaterThanCondition("x", "1", ValueType.Number, 10).getPriority());
    }

    @Test
    void getPriority_celCondition_returnsExplicitPriority() {
        assertEquals(3, new CelCondition("x > 0", 3).getPriority());
    }

    @Test
    void getPriority_inCondition_returnsExplicitPriority() {
        assertEquals(7, new InCondition("x", "[\"a\"]", ValueType.List, 7).getPriority());
    }

    @Test
    void getPriority_regexCondition_returnsExplicitPriority() {
        assertEquals(2, new RegexCondition("x", "\\d+", 2).getPriority());
    }

    // ---- getPriority: default (0) ----

    @Test
    void getPriority_withZero_returnsDefault() {
        assertEquals(Priority.DEFAULT, new EqualCondition("x", "1", ValueType.Number, 0).getPriority());
    }

    @Test
    void getPriority_negativeValue_preserved() {
        assertEquals(-1, new EqualCondition("x", "1", ValueType.Number, -1).getPriority());
    }

    // ---- negate() preserves priority ----

    @Test
    void negate_equalCondition_preservesPriority() throws Exception {
        assertEquals(5, new EqualCondition("x", "1", ValueType.Number, 5).negate().getPriority());
    }

    @Test
    void negate_notEqualCondition_preservesPriority() throws Exception {
        assertEquals(8, new NotEqualCondition("x", "1", ValueType.Number, 8).negate().getPriority());
    }

    @Test
    void negate_greaterThanCondition_preservesPriority() throws Exception {
        assertEquals(3, new GreaterThanCondition("x", "1", ValueType.Number, 3).negate().getPriority());
    }

    @Test
    void negate_greaterThanOrEqualCondition_preservesPriority() throws Exception {
        assertEquals(4, new GreaterThanOrEqualCondition("x", "1", ValueType.Number, 4).negate().getPriority());
    }

    @Test
    void negate_lessThanCondition_preservesPriority() throws Exception {
        assertEquals(6, new LessThanCondition("x", "1", ValueType.Number, 6).negate().getPriority());
    }

    @Test
    void negate_lessThanOrEqualCondition_preservesPriority() throws Exception {
        assertEquals(9, new LessThanOrEqualCondition("x", "1", ValueType.Number, 9).negate().getPriority());
    }

    @Test
    void negate_inCondition_preservesPriority() throws Exception {
        assertEquals(2, new InCondition("x", "[\"admin\", \"ops\"]", ValueType.List, 2).negate().getPriority());
    }

    @Test
    void negate_notInCondition_preservesPriority() throws Exception {
        assertEquals(11, new NotInCondition("x", "[\"admin\", \"ops\"]", ValueType.List, 11).negate().getPriority());
    }

    @Test
    void negate_celCondition_preservesPriority() throws Exception {
        assertEquals(7, new CelCondition("x > 0", 7).negate().getPriority());
    }

    @Test
    void negate_regexCondition_preservesPriority() throws Exception {
        assertEquals(5, new RegexCondition("x", "\\d+", 5).negate().getPriority());
    }

    // ---- Path.sort() orders conditions ascending by priority ----

    @Test
    void pathSort_ordersConditionsAscendingByPriority() {
        StubCondition low = new StubCondition("low", 10);
        StubCondition mid = new StubCondition("mid", 5);
        StubCondition high = new StubCondition("high", 1);

        Path path = new Path();
        path.addCondition(low).addCondition(mid).addCondition(high);
        path.sort();

        List<?> sorted = path.conditions();
        assertSame(high, sorted.get(0));
        assertSame(mid, sorted.get(1));
        assertSame(low, sorted.get(2));
    }

    @Test
    void pathSort_conditionsWithSamePriority_preserveRelativeOrder() {
        StubCondition a = new StubCondition("a", 0);
        StubCondition b = new StubCondition("b", 0);
        StubCondition c = new StubCondition("c", 0);

        Path path = new Path();
        path.addCondition(a).addCondition(b).addCondition(c);
        path.sort();

        // stable sort: relative order unchanged when priority equal
        assertEquals(List.of(a, b, c), path.conditions());
    }

    @Test
    void pathSort_emptyPath_doesNotThrow() {
        assertDoesNotThrow(() -> new Path().sort());
    }

    @Test
    void pathSort_usePriorityConstants() {
        StubCondition highest = new StubCondition("highest", Priority.HIGHEST);
        StubCondition def = new StubCondition("default", Priority.DEFAULT);
        StubCondition lowest = new StubCondition("lowest", Priority.LOWEST);

        Path path = new Path();
        path.addCondition(lowest).addCondition(def).addCondition(highest);
        path.sort();

        assertSame(highest, path.conditions().get(0));
        assertSame(def, path.conditions().get(1));
        assertSame(lowest, path.conditions().get(2));
    }
}
