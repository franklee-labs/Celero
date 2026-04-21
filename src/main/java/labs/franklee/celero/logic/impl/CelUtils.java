package labs.franklee.celero.logic.impl;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.ast.CelExpr;
import dev.cel.common.navigation.CelNavigableAst;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CelUtils {

    static CelRuntime.Program buildProgram(String expression) throws Exception {
        Set<String> varNames = extractTopVarNames(expression);
        Cel cel = buildCelWithVars(varNames, null);
        return buildProgram(expression, cel);
    }

    static CelRuntime.Program buildProgram(String expression, Cel cel) throws Exception {
        CelAbstractSyntaxTree ast = cel.compile(expression).getAst();
        return cel.createProgram(ast);
    }

    static Cel buildCelWithVars(Set<String> varNames, Map<String, CelType> types) {
        var builder = CelFactory.standardCelBuilder();
        if (types == null || types.isEmpty()) {
            varNames.forEach(v -> builder.addVar(v, SimpleType.DYN));
        } else {
            varNames.forEach(v -> builder.addVar(v, types.getOrDefault(v, SimpleType.DYN)));
        }
        return builder.build();
    }

    static Set<String> extractTopVarNames(String expression) throws Exception {
        Cel parser = CelFactory.standardCelBuilder().build();
        CelAbstractSyntaxTree parsed = parser.parse(expression).getAst();
        return CelNavigableAst.fromAst(parsed)
                .getRoot()
                .allNodes()
                .filter(node -> node.getKind() == CelExpr.ExprKind.Kind.IDENT)
                .map(node -> node.expr().ident().name())
                .collect(Collectors.toSet());
    }


    /**
     * Normalizes a value to a CEL-compatible type.
     * Handles deserialization artifacts (e.g. BigDecimal from JSON) that CEL cannot accept directly.
     * BigDecimal / Integer / Float → Long or Double; String and Boolean pass through unchanged.
     */
    static Object toCelValue(Object value) {
        if (value instanceof java.math.BigDecimal bd) {
            java.math.BigDecimal stripped = bd.stripTrailingZeros();
            return stripped.scale() <= 0 ? stripped.longValueExact() : stripped.doubleValue();
        }
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Float f) return f.doubleValue();
        return value;
    }
}
