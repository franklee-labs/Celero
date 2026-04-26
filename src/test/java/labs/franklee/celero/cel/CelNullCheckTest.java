package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelBuilder;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.ast.CelExpr;
import dev.cel.common.navigation.CelNavigableAst;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntime;
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

    static Cel buildCelWithVars(Set<String> varNames, Map<String, CelType> types) {
        CelBuilder builder = CelFactory.standardCelBuilder();
        if (types == null || types.isEmpty()) {
            varNames.forEach(v -> builder.addVar(v, SimpleType.DYN));
        } else {
            varNames.forEach(v -> builder.addVar(v, types.getOrDefault(v, SimpleType.DYN)));
        }
        return builder.build();
    }

    static CelRuntime.Program buildProgram(String expression, Cel cel) throws Exception {
        CelAbstractSyntaxTree ast = cel.compile(expression).getAst();
        return cel.createProgram(ast);
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
        Cel compiler = buildCelWithVars(vars, null);
        CelRuntime.Program program = buildProgram(expression, compiler);
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
        Cel compiler = buildCelWithVars(vars, null);
        CelRuntime.Program program = buildProgram(expression, compiler);
        Map<String, Object> p = new HashMap<>();
        p.put("location", NullValue.NULL_VALUE);
        Object o = program.eval(Map.of("params", p));
        System.out.println(o);
        assertTrue(o instanceof Boolean b && b);
    }

    @Test
    void eval_null() throws Throwable {
        String expression = "isNull(params.location.number)";
        Set<String> vars = extract(expression);
        System.out.println(vars);
        CelBuilder builder = CelFactory.standardCelBuilder()
                .addFunctionDeclarations(
                    CelFunctionDecl.newFunctionDeclaration(
                            "isNull",
                            CelOverloadDecl.newGlobalOverload(
                                    "isNull_dyn",
                                    SimpleType.BOOL,
                                    SimpleType.DYN
                            )
                    )
                )
                .addFunctionBindings(
                    CelFunctionBinding.from(
                            "isNull_dyn",
                            Object.class,
                            arg -> arg == null || arg instanceof NullValue
                    )
                );
        vars.forEach(v -> builder.addVar(v, SimpleType.DYN));
        Cel compiler = builder.build();

        CelRuntime.Program program = buildProgram(expression, compiler);
        Map<String, Object> p = new HashMap<>();
        p.put("number", null);
        Object o = program.eval(Map.of("params", Map.of("location", p)));
        System.out.println(o);
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
