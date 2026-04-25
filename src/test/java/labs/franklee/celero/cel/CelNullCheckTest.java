package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.ast.CelExpr;
import dev.cel.common.navigation.CelNavigableAst;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import dev.cel.runtime.CelUnknownSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CelNullCheckTest {

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

    private static Cel buildDynCel(String... varNames) throws Exception {
        var builder = CelFactory.standardCelBuilder();
        for (String v : varNames) builder.addVar(v, SimpleType.DYN);
        return builder.build();
    }

    @Test
    void check_null_unknown() throws Throwable {
        String expression = "params.location == null";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder();
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Map<String, Object> p = new HashMap<>();
        p.put("location", null);
        Object o = program.eval(Map.of("params", p));
        System.out.println(o);
        assertInstanceOf(CelUnknownSet.class, o);
    }

    @Test
    void check_null_true() throws Throwable {
        String expression = "params.location == null";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder();
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        CelCompiler compiler = builder.build();
        CelAbstractSyntaxTree ast = compiler.compile(expression).getAst();
        CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build();
        CelRuntime.Program program = runtime.createProgram(ast);
        Map<String, Object> p = new HashMap<>();
        p.put("location", NullValue.NULL_VALUE);
        Object o = program.eval(Map.of("params", p));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && b);
    }

    /*
    private Map<String, Object> convertParams(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        params.forEach((k, v) -> result.put(k, convertValue(v)));
        return result;
    }

    private Object convertValue(Object value) {
        if (value == null) {
            return NullValue.NULL_VALUE;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new HashMap<>();
            map.forEach((k, v) -> nested.put((String) k, convertValue(v)));
            return nested;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::convertValue)
                    .toList();
        }
        return value;
    }
     */
}
