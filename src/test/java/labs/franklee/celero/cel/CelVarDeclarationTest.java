package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify whether CEL variables must be declared at build time.
 *
 * Conclusion (CEL Java 0.12.0):
 *   - Variables MUST be declared via addVar() before compile().
 *     There is no option to skip type-checking for undeclared identifiers.
 *   - Two workarounds are available:
 *     (A) Declare a variable as SimpleType.DYN — name is known, type is flexible.
 *     (B) Use a single map(string, dyn) catch-all — neither name nor type need
 *         to be decided at build time; new keys can be added freely at eval time.
 */
class CelVarDeclarationTest {

    // ---- 1. undeclared variable — compile fails ----

    @Test
    void undeclaredVar_throwsCelValidationExceptionAtCompileTime() {
        // Without addVar(), compile() rejects an unknown identifier immediately.
        // Variables cannot be passed at eval time without prior declaration.
        assertThrows(CelValidationException.class, () -> {
            Cel cel = CelFactory.standardCelBuilder().build();
            cel.compile("age >= 18").getAst();   // "age" was never declared
        });
    }

    // ---- 2. workaround A: declare as DYN (flexible type, fixed name) ----

    @Test
    void dynVar_acceptsAnyRuntimeType() throws Exception {
        // SimpleType.DYN defers type checking to runtime.
        // The variable name must still be known at build time.
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("value", SimpleType.DYN)
                .build();

        var ast     = cel.compile("value == \"hello\"").getAst();
        var program = cel.createProgram(ast);

        assertTrue((Boolean)  program.eval(Map.of("value", "hello")));
        assertFalse((Boolean) program.eval(Map.of("value", 42L)));    // type mismatch → false
    }

    @Test
    void dynVar_multipleVars_allDeclaredAsDyn() throws Exception {
        // Each variable name is declared once as DYN;
        // any runtime value can be passed for each.
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age",  SimpleType.DYN)
                .addVar("role", SimpleType.DYN)
                .build();

        var ast     = cel.compile("age >= 18 && role == \"admin\"").getAst();
        var program = cel.createProgram(ast);

        Object resultPass = program.eval(Map.of("age", 25L, "role", "admin"));
        Object resultFail = program.eval(Map.of("age", 16L, "role", "admin"));

        assertTrue((Boolean)  resultPass);
        assertFalse((Boolean) resultFail);
    }

    // ---- 3. workaround B: single map(string, dyn) — fully open at eval time ----

    @Test
    void mapVar_newKeysAddedAtEvalTime_noRebuild() throws Exception {
        // Declare one map variable once; any key can appear at eval time
        // without touching the Cel instance again.
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("params", MapType.create(SimpleType.STRING, SimpleType.DYN))
                .build();

        // At build time we had no idea these keys would exist.
        var ast     = cel.compile("params.score >= 90.0").getAst();
        var program = cel.createProgram(ast);

        assertTrue((Boolean)  program.eval(Map.of("params", Map.of("score", 95.0))));
        assertFalse((Boolean) program.eval(Map.of("params", Map.of("score", 80.0))));
    }
}
