package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Use cases for CEL evaluation with multiple independent primitive variables.
 * Each variable is a plain scalar type (string, int, bool, double),
 * not wrapped in a map — tests whether CEL eval accepts them directly.
 */
class CelPrimitiveVarTest {

    /**
     * CEL instance with four scalar variables:
     *   age      — int
     *   role     — string
     *   isPremium — bool
     *   score    — double
     */
    private Cel cel;

    @BeforeEach
    void setUp() throws Exception {
        cel = CelFactory.standardCelBuilder()
                .addVar("age",       SimpleType.INT)
                .addVar("role",      SimpleType.STRING)
                .addVar("isPremium", SimpleType.BOOL)
                .addVar("score",     SimpleType.DOUBLE)
                .build();
    }

    @Test
    void primitiveVar_intComparison() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("age >= 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "age",       20L,
                "role",      "user",
                "isPremium", false,
                "score",     0.0
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void primitiveVar_stringEquality() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("role == \"admin\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "age",       25L,
                "role",      "admin",
                "isPremium", false,
                "score",     0.0
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void primitiveVar_boolCheck() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("isPremium == true").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "age",       25L,
                "role",      "user",
                "isPremium", true,
                "score",     0.0
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void primitiveVar_doubleComparison() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("score >= 90.0").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "age",       25L,
                "role",      "user",
                "isPremium", false,
                "score",     95.5
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void primitiveVar_crossVar_combined() throws Exception {
        // Adult admin with a high score
        CelAbstractSyntaxTree ast = cel.compile(
                "age >= 18 && role == \"admin\" && score >= 90.0"
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object resultPass = program.eval(Map.of(
                "age",       25L,
                "role",      "admin",
                "isPremium", false,
                "score",     92.0
        ));
        Object resultFail = program.eval(Map.of(
                "age",       25L,
                "role",      "admin",
                "isPremium", false,
                "score",     80.0   // score too low
        ));

        assertTrue((Boolean) resultPass);
        assertFalse((Boolean) resultFail);
    }

    @Test
    void primitiveVar_stringFunc_endsWith() throws Exception {
        // endsWith works on a plain string variable too
        CelAbstractSyntaxTree ast = cel.compile("role.endsWith(\"min\")").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "age",       25L,
                "role",      "admin",
                "isPremium", false,
                "score",     0.0
        ));

        assertTrue((Boolean) result);
    }
}
