package labs.franklee.celero.logic.path;

import labs.franklee.celero.logic.helper.StubCondition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathGroupTest {

    @Test
    void defaultConstructor_pathsIsEmpty() {
        assertTrue(new PathGroup().paths().isEmpty());
    }

    @Test
    void pathConstructor_containsSinglePath() {
        Path path = new Path().addCondition(new StubCondition("a"));
        PathGroup group = new PathGroup(path);
        assertEquals(1, group.paths().size());
        assertSame(path, group.paths().get(0));
    }

    @Test
    void addPath_appendsPaths() {
        PathGroup group = new PathGroup();
        Path p1 = new Path().addCondition(new StubCondition("a"));
        Path p2 = new Path().addCondition(new StubCondition("b"));
        group.addPath(p1);
        group.addPath(p2);
        assertEquals(2, group.paths().size());
        assertSame(p1, group.paths().get(0));
        assertSame(p2, group.paths().get(1));
    }
}
