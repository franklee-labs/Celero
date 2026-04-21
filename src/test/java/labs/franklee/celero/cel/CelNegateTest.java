package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that wrapping a CEL expression with !() is a valid negate strategy.
 * Tests: basic negation, double negation, and De Morgan consistency.
 */
class CelNegateTest {

    private Cel cel;

    @BeforeEach
    void setUp() throws Exception {
        cel = CelFactory.standardCelBuilder()
                .addVar("age",  SimpleType.DYN)
                .addVar("role", SimpleType.DYN)
                .build();
    }

    private CelRuntime.Program compile(String expr) throws Exception {
        return cel.createProgram(cel.compile(expr).getAst());
    }

    private boolean eval(CelRuntime.Program program, Map<String, Object> vars) throws Exception {
        return (Boolean) program.eval(vars);
    }

    // ---- basic negation ----

    @Test
    void negate_simpleComparison() throws Exception {
        String expr        = "age > 18";
        String negatedExpr = "!(" + expr + ")";

        CelRuntime.Program original = compile(expr);
        CelRuntime.Program negated  = compile(negatedExpr);

        Map<String, Object> pass = Map.of("age", 20L, "role", "user");
        Map<String, Object> fail = Map.of("age", 16L, "role", "user");

        assertTrue (eval(original, pass));
        assertFalse(eval(negated,  pass));   // negated flips the result
        assertFalse(eval(original, fail));
        assertTrue (eval(negated,  fail));
    }

    @Test
    void negate_compoundExpression() throws Exception {
        String expr        = "age > 18 && role == \"admin\"";
        String negatedExpr = "!(" + expr + ")";

        CelRuntime.Program original = compile(expr);
        CelRuntime.Program negated  = compile(negatedExpr);

        Map<String, Object> both = Map.of("age", 25L, "role", "admin");
        Map<String, Object> one  = Map.of("age", 25L, "role", "user");

        assertTrue (eval(original, both));
        assertFalse(eval(negated,  both));
        assertFalse(eval(original, one));
        assertTrue (eval(negated,  one));
    }

    // ---- double negation ----

    @Test
    void doubleNegate_restoresOriginal() throws Exception {
        String expr              = "age > 18";
        String doubleNegatedExpr = "!(!(" + expr + "))";

        CelRuntime.Program original      = compile(expr);
        CelRuntime.Program doubleNegated = compile(doubleNegatedExpr);

        Map<String, Object> pass = Map.of("age", 20L, "role", "user");
        Map<String, Object> fail = Map.of("age", 16L, "role", "user");

        // double negation must produce identical results to the original
        assertEquals(eval(original, pass), eval(doubleNegated, pass));
        assertEquals(eval(original, fail), eval(doubleNegated, fail));
    }

    // ---- De Morgan consistency ----

    @Test
    void deMorgan_negateAnd_equalsOrOfNegations() throws Exception {
        // !(A && B)  ==  (!A) || (!B)
        String negatedAnd    = "!(age > 18 && role == \"admin\")";
        String deMorganEquiv = "!(age > 18) || !(role == \"admin\")";

        CelRuntime.Program left  = compile(negatedAnd);
        CelRuntime.Program right = compile(deMorganEquiv);

        for (var vars : cases()) {
            assertEquals(eval(left, vars), eval(right, vars),
                    "De Morgan mismatch for vars: " + vars);
        }
    }

    @Test
    void deMorgan_negateOr_equalsAndOfNegations() throws Exception {
        // !(A || B)  ==  (!A) && (!B)
        String negatedOr     = "!(age > 18 || role == \"admin\")";
        String deMorganEquiv = "!(age > 18) && !(role == \"admin\")";

        CelRuntime.Program left  = compile(negatedOr);
        CelRuntime.Program right = compile(deMorganEquiv);

        for (var vars : cases()) {
            assertEquals(eval(left, vars), eval(right, vars),
                    "De Morgan mismatch for vars: " + vars);
        }
    }

    private static Map<String, Object> vars(long age, String role) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("age", age);
        m.put("role", role);
        return m;
    }

    private static java.util.List<Map<String, Object>> cases() {
        return java.util.List.of(
                vars(25L, "admin"),
                vars(25L, "user"),
                vars(16L, "admin"),
                vars(16L, "user")
        );
    }
}
