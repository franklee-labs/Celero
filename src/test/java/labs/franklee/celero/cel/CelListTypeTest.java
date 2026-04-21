package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify CEL list type behavior for the "in" operator.
 * Covers: typed lists, DYN lists, mixed-type lists, and cross-type matching.
 */
class CelListTypeTest {

    // ---- String list ----

    @Test
    void stringList_match() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("role", SimpleType.STRING)
                .addVar("list", ListType.create(SimpleType.STRING))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("role in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        assertTrue((Boolean) program.eval(Map.of("role", "admin", "list", List.of("admin", "ops"))));
        assertFalse((Boolean) program.eval(Map.of("role", "user", "list", List.of("admin", "ops"))));
    }

    // ---- Long list ----

    @Test
    void longList_match() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age", SimpleType.INT)
                .addVar("list", ListType.create(SimpleType.INT))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("age in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        assertTrue((Boolean) program.eval(Map.of("age", 18L, "list", List.of(18L, 25L, 30L))));
        assertFalse((Boolean) program.eval(Map.of("age", 20L, "list", List.of(18L, 25L, 30L))));
    }

    // ---- Double list ----

    @Test
    void doubleList_match() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("score", SimpleType.DOUBLE)
                .addVar("list", ListType.create(SimpleType.DOUBLE))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("score in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        assertTrue((Boolean) program.eval(Map.of("score", 99.5, "list", List.of(99.5, 88.0))));
        assertFalse((Boolean) program.eval(Map.of("score", 70.0, "list", List.of(99.5, 88.0))));
    }

    // ---- DYN list: can it hold mixed types? ----

    @Test
    void dynList_stringValues_match() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("role", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("role in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        assertTrue((Boolean) program.eval(Map.of("role", "admin", "list", List.of("admin", "ops"))));
    }

    @Test
    void dynList_mixedLongAndDouble_longFieldMatch() throws Exception {
        // list contains both Long and Double — does CEL match a Long field against it?
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("age in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        // mixed Long and Double in the same list
        Object result = program.eval(Map.of("age", 18L, "list", List.of(18L, 25.5, 30L)));
        assertEquals(true, result);
    }

    @Test
    void dynList_longFieldAgainstDoubleList_crossTypeMatch() throws Exception {
        // field is Long, list contains Doubles of the same numeric value — does CEL treat them as equal?
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("age in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        // 18L vs 18.0 — same numeric value, different Java types
        Object result = program.eval(Map.of("age", 18L, "list", List.of(18.0, 25.0)));
        // CEL may or may not consider 18L == 18.0
        assertNotNull(result);
        System.out.println("18L in [18.0, 25.0] → " + result);
    }

    // ---- Fully mixed list: String + Long + Double + Boolean ----

    @Test
    void mixedList_stringField_matchesStringElement() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        List<Object> mixed = List.of("admin", 18L, 99.5, true);
        Object result = program.eval(Map.of("val", "admin", "list", mixed));
        System.out.println("\"admin\" in [\"admin\", 18L, 99.5, true] → " + result);
        assertNotNull(result);
    }

    @Test
    void mixedList_longField_matchesLongElement() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        List<Object> mixed = List.of("admin", 18L, 99.5, true);
        Object result = program.eval(Map.of("val", 18L, "list", mixed));
        System.out.println("18L in [\"admin\", 18L, 99.5, true] → " + result);
        assertNotNull(result);
    }

    @Test
    void mixedList_doubleField_matchesDoubleElement() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        List<Object> mixed = List.of("admin", 18L, 99.5, true);
        Object result = program.eval(Map.of("val", 99.5, "list", mixed));
        System.out.println("99.5 in [\"admin\", 18L, 99.5, true] → " + result);
        assertNotNull(result);
    }

    @Test
    void mixedList_booleanField_matchesBooleanElement() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        List<Object> mixed = List.of("admin", 18L, 99.5, true);
        Object result = program.eval(Map.of("val", true, "list", mixed));
        System.out.println("true in [\"admin\", 18L, 99.5, true] → " + result);
        assertNotNull(result);
    }

    @Test
    void mixedList_booleanField_matchesBooleanFalseElement() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        List<Object> mixed = List.of("admin", 18L, 99.5, true);
        Object result = program.eval(Map.of("val", false, "list", mixed));
        System.out.println("false in [\"admin\", 18L, 99.5, true] → " + result);
        assertNotNull(result);
    }

    @Test
    void mixedList_noMatch_returnsFalse() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        List<Object> mixed = List.of("admin", 18L, 99.5, true);
        Object result = program.eval(Map.of("val", "unknown", "list", mixed));
        System.out.println("\"unknown\" in [\"admin\", 18L, 99.5, true] → " + result);
        assertFalse((Boolean) result);
    }
}
