package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelOptions;
import dev.cel.common.ast.CelExpr;
import dev.cel.common.ast.CelExpr.ExprKind.Kind;
import dev.cel.common.navigation.CelNavigableAst;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.extensions.CelExtensions;
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
 * Demonstrates how to extract variable names from a CEL expression using
 * the official CelNavigableAst API (dev.cel.common.navigation).
 *
 * Strategy:
 *   1. parse()  — no addVar() needed, produces an untyped AST
 *   2. CelNavigableAst.fromAst().getRoot().allNodes() — flat stream of every node
 *   3. Filter for IDENT nodes
 *   4. addVar(name, DYN) for each discovered name, then compile() + eval()
 */
class CelExtractVarTest {

    private static Set<String> extractVarNames(CelAbstractSyntaxTree ast) {
        return CelNavigableAst.fromAst(ast)
                .getRoot()
                .allNodes()
                .filter(node -> node.getKind() == Kind.IDENT)
                .map(node -> node.expr().ident().name())
                .collect(Collectors.toSet());
    }

    // ---- tests ----

    @Test
    void extract_builtinLiterals_areCONSTANT_notIDENT() throws Exception {
        // true / false / null are parsed as CONSTANT nodes, never IDENT.
        // extractVarNames only visits IDENT nodes, so they are naturally absent.
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse(
                "isPremium == true && value == null && flag == false"
        ).getAst();

        // print all node kinds to confirm true/false/null are CONSTANT
        CelNavigableAst.fromAst(parsed).getRoot().allNodes()
                .forEach(n -> System.out.printf("kind=%-12s %s%n",
                        n.getKind(),
                        n.getKind() == Kind.IDENT    ? "name=" + n.expr().ident().name() :
                        n.getKind() == Kind.CONSTANT ? "value=" + n.expr().constant()    : ""));

        assertEquals(Set.of("isPremium", "value", "flag"), extractVarNames(parsed));
    }

    @Test
    void extract_capitalizedLiterals_appearAsIDENT() throws Exception {
        // True / False / NULL (non-standard casing) are NOT recognized as literals —
        // CEL treats them as plain identifiers (IDENT nodes), i.e. variable references.
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse(
                "flag == True && value == NULL"
        ).getAst();

        Set<String> vars = extractVarNames(parsed);
        System.out.println("capitalized literals → idents: " + vars);

        // True and NULL are treated as variables, not literals
        assertTrue(vars.contains("True"));
        assertTrue(vars.contains("NULL"));
    }

    @Test
    void extract_flatVar() throws Exception {
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse("age >= 18").getAst();

        assertEquals(Set.of("age"), extractVarNames(parsed));
    }

    @Test
    void extract_multipleVars() throws Exception {
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse("age >= 18 && role == \"admin\"").getAst();

        assertEquals(Set.of("age", "role"), extractVarNames(parsed));
    }

    @Test
    void extract_nestedFieldAccess_returnsRootOnly() throws Exception {
        // user.address.city == "Beijing"  →  only "user" is a variable
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse("user.address.city == \"Beijing\"").getAst();

        assertEquals(Set.of("user"), extractVarNames(parsed));
    }

    @Test
    void extract_mixedFlatAndNested() throws Exception {
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse(
                "age >= 18 && user.role == \"admin\" && order.amount > 100.0"
        ).getAst();

        assertEquals(Set.of("age", "user", "order"), extractVarNames(parsed));
    }

    @Test
    void extract_stringFunction_doesNotIncludeFunctionName() throws Exception {
        // email.endsWith("@gmail.com")  →  only "email" is a variable
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse("email.endsWith(\"@gmail.com\")").getAst();

        assertEquals(Set.of("email"), extractVarNames(parsed));
    }

    // ---- built-in functions and extensions ----

    @Test
    void extract_builtinGlobalFunction_size() throws Exception {
        // size(name) — size is a global built-in function call, not a variable
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse("size(name) > 3").getAst();

        Set<String> vars = extractVarNames(parsed);
        System.out.println("size(name) → idents: " + vars);

        assertEquals(Set.of("name"), vars);
    }

    @Test
    void extract_builtinTypeConversion_int() throws Exception {
        // int(score) — int() is a type-conversion function, not a variable
        Cel cel = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = cel.parse("int(score) > 90").getAst();

        Set<String> vars = extractVarNames(parsed);
        System.out.println("int(score) → idents: " + vars);

        assertEquals(Set.of("score"), vars);
    }

    @Test
    void extract_mathExtension_ceil() throws Exception {
        // math.ceil(score) — "math" is a namespace, not a variable
        Cel cel = CelFactory.standardCelBuilder()
                .addCompilerLibraries(CelExtensions.math(CelOptions.DEFAULT))
                .addRuntimeLibraries(CelExtensions.math(CelOptions.DEFAULT))
                .build();
        CelAbstractSyntaxTree parsed = cel.parse("math.ceil(score) > 90.0").getAst();

        Set<String> vars = extractVarNames(parsed);
        System.out.println("math.ceil(score) → idents: " + vars);
    }

    @Test
    void extract_mathAsVariable_conflictsWithExtensionNamespace() throws Exception {
        // What if the user passes a variable literally named "math"?
        Cel cel = CelFactory.standardCelBuilder()
                .addCompilerLibraries(CelExtensions.math(CelOptions.DEFAULT))
                .addRuntimeLibraries(CelExtensions.math(CelOptions.DEFAULT))
                .addVar("math", SimpleType.DYN)
                .addVar("score", SimpleType.DYN)
                .build();

        // Expression uses both math.ceil (extension) and a variable named math
        CelRuntime.Program program = cel.createProgram(
                cel.compile("math.ceil(score) > 90.0").getAst()
        );

        Object result = program.eval(Map.of("math", "anything", "score", 95.5));
        System.out.println("result: " + result);
    }

    @Test
    void extract_thenCompileAndEval_endToEnd() throws Exception {
        String expression = "age >= 18 && role == \"admin\"";

        // Step 1: parse to extract variable names (no declarations needed)
        Cel parser = CelFactory.standardCelBuilder().build();
        Set<String> vars = extractVarNames(parser.parse(expression).getAst());

        // Step 2: build a typed CEL instance with discovered names
        var builder = CelFactory.standardCelBuilder();
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        Cel cel = builder.build();

        // Step 3: compile + cache program (done once per rule)
        CelRuntime.Program program = cel.createProgram(cel.compile(expression).getAst());

        // Step 4: eval on hot path
        assertTrue((Boolean)  program.eval(Map.of("age", 25L, "role", "admin")));
        assertFalse((Boolean) program.eval(Map.of("age", 16L, "role", "admin")));
    }

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
}
