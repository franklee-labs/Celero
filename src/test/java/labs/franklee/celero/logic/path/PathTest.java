package labs.franklee.celero.logic.path;

import labs.franklee.celero.logic.helper.StubCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathTest {

    @Test
    void conditions_emptyOnConstruction() {
        assertTrue(new Path().conditions().isEmpty());
    }

    @Test
    void addCondition_appendsAndReturnsSelf() {
        StubCondition c = new StubCondition("a");
        Path path = new Path();
        Path returned = path.addCondition(c);
        assertSame(path, returned, "addCondition must return 'this' for chaining");
        assertEquals(1, path.conditions().size());
        assertSame(c, path.conditions().get(0));
    }

    @Test
    void addAllCondition_appendsAllAndReturnsSelf() {
        StubCondition c1 = new StubCondition("a");
        StubCondition c2 = new StubCondition("b");
        Path path = new Path();
        Path returned = path.addAllCondition(List.of(c1, c2));
        assertSame(path, returned, "addAllCondition must return 'this' for chaining");
        assertEquals(2, path.conditions().size());
        assertSame(c1, path.conditions().get(0));
        assertSame(c2, path.conditions().get(1));
    }

    @Test
    void addCondition_thenAddAllCondition_accumulatesInOrder() {
        StubCondition c1 = new StubCondition("a");
        StubCondition c2 = new StubCondition("b");
        StubCondition c3 = new StubCondition("c");
        Path path = new Path();
        path.addCondition(c1).addAllCondition(List.of(c2, c3));
        assertEquals(List.of(c1, c2, c3), path.conditions());
    }
}
