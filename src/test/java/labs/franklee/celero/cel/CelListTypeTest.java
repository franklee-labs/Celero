package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.ast.CelExpr;
import dev.cel.common.navigation.CelNavigableAst;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import dev.cel.runtime.CelUnknownSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    // =============================== list.filter/exists

    private Set<String> extract(String expression) throws Throwable {
        Cel parser = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = parser.parse(expression).getAst();
        return CelNavigableAst.fromAst(parsed)
                .getRoot()
                .allNodes()
                .filter(node -> node.getKind() == CelExpr.ExprKind.Kind.IDENT)
                .map(node -> node.expr().ident().name())
                .collect(Collectors.toSet());
    }


    @Test
    void extract_Filter_contains() throws Throwable {
        String expression = "list1.filter(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of("11", "22", "33"), "list2", List.of("aa", "22", "bb")));
        System.out.println(o);
        assertInstanceOf(List.class, o);
        assertEquals(1, ((List<String>)o).size());
        assertTrue(((List<String>) o).contains("22"));
    }

    @Test
    void extract_Filter_contains_multi_type() throws Throwable {
        String expression = "list1.filter(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of("11", 22, true), "list2", List.of("aa", 22, false)));
        System.out.println(o);
        assertInstanceOf(List.class, o);
        assertEquals(1, ((List<Object>)o).size());
        assertEquals(22L, ((List<Object>)o).get(0));  // o.get(0) is long, not int
    }

    @Test
    void extract_Filter_notContains() throws Throwable {
        String expression = "list1.filter(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of("11", "22", "33"), "list2", List.of("aa", "222", "bb")));
        System.out.println(o);
        assertInstanceOf(List.class, o);
        assertEquals(0, ((List<String>)o).size());
    }

    @Test
    void extract_Filter_listNull() throws Throwable {
        String expression = "list1.filter(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Map<String, Object> params = new HashMap<>();
        params.put("list1", null);
        params.put("list2", List.of("aa", "222", "bb"));
        Object o = program.eval(params);
        System.out.println(o);
        assertInstanceOf(CelUnknownSet.class, o);
    }

    @Test
    void extract_Exists_true() throws Throwable {
        String expression = "list1.exists(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of("11", "22", "33"), "list2", List.of("aa", "22", "bb")));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && b);
    }

    @Test
    void extract_Exists_false() throws Throwable {
        String expression = "!(list1.exists(x, list2.exists(y, y == x)))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of("11", "22", "33"), "list2", List.of("aa", "222", "bb")));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && b);
    }

    @Test
    void extract_emptyList_Exists_false() throws Throwable {
        String expression = "list1.exists(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of(), "list2", List.of("aa", "222", "bb")));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && !b);
    }

    @Test
    void extract_emptyList2_Exists_false() throws Throwable {
        String expression = "list1.exists(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of("aa", "222", "bb"), "list2", List.of()));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && !b);
    }

    @Test
    void extract_emptyList3_Exists_false() throws Throwable {
        String expression = "list1.exists(x, list2.exists(y, y == x))";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder()
                .setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Object o = program.eval(Map.of("list1", List.of(), "list2", List.of()));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && !b);
    }
}
