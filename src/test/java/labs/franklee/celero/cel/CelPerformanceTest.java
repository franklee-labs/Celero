package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures the cost of each CEL phase to guide caching strategy.
 *
 * Three phases:
 *   build()    — creates the CEL environment (type registry, standard lib)
 *   compile()  — parses + type-checks the expression, produces an AST
 *   eval()     — executes the program against a concrete activation map
 *
 * Expected order of magnitude: build >> compile >> eval
 */
class CelPerformanceTest {

    private static final int WARM_UP   = 200;
    private static final int ROUNDS    = 2_000;
    private static final String EXPR   = "age >= 18 && role == \"admin\"";

    // ---- phase 1: build() ----

    @Test
    void cost_build_perCall() throws Exception {
        // Warm up
        for (int i = 0; i < WARM_UP; i++) {
            CelFactory.standardCelBuilder()
                    .addVar("age",  SimpleType.DYN)
                    .addVar("role", SimpleType.DYN)
                    .build();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            CelFactory.standardCelBuilder()
                    .addVar("age",  SimpleType.DYN)
                    .addVar("role", SimpleType.DYN)
                    .build();
        }
        long avgNs = (System.nanoTime() - start) / ROUNDS;

        System.out.printf("[build]   avg %,d ns  (%.3f ms)%n", avgNs, avgNs / 1_000_000.0);
        assertTrue(true);
    }

    // ---- phase 2: compile() ----

    @Test
    void cost_compile_perCall() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age",  SimpleType.DYN)
                .addVar("role", SimpleType.DYN)
                .build();

        // Warm up
        for (int i = 0; i < WARM_UP; i++) {
            cel.compile(EXPR).getAst();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            cel.compile(EXPR).getAst();
        }
        long avgNs = (System.nanoTime() - start) / ROUNDS;

        System.out.printf("[compile] avg %,d ns  (%.3f ms)%n", avgNs, avgNs / 1_000_000.0);
        assertTrue(true);
    }

    // ---- phase 3: eval() ----

    @Test
    void cost_eval_perCall() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age",  SimpleType.DYN)
                .addVar("role", SimpleType.DYN)
                .build();

        CelAbstractSyntaxTree ast = cel.compile(EXPR).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> vars = Map.of("age", 25L, "role", "admin");

        // Warm up
        for (int i = 0; i < WARM_UP; i++) {
            program.eval(vars);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            program.eval(vars);
        }
        long avgNs = (System.nanoTime() - start) / ROUNDS;

        System.out.printf("[eval]    avg %,d ns  (%.3f ms)%n", avgNs, avgNs / 1_000_000.0);
        assertTrue(true);
    }

    // ---- worst case: rebuild everything on every request ----

    @Test
    void cost_rebuildEverythingPerRequest() throws Exception {
        Map<String, Object> vars = Map.of("age", 25L, "role", "admin");

        // Warm up
        for (int i = 0; i < WARM_UP; i++) {
            Cel cel = CelFactory.standardCelBuilder()
                    .addVar("age",  SimpleType.DYN)
                    .addVar("role", SimpleType.DYN)
                    .build();
            CelAbstractSyntaxTree ast = cel.compile(EXPR).getAst();
            cel.createProgram(ast).eval(vars);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            Cel cel = CelFactory.standardCelBuilder()
                    .addVar("age",  SimpleType.DYN)
                    .addVar("role", SimpleType.DYN)
                    .build();
            CelAbstractSyntaxTree ast = cel.compile(EXPR).getAst();
            cel.createProgram(ast).eval(vars);
        }
        long avgNs = (System.nanoTime() - start) / ROUNDS;

        System.out.printf("[rebuild-all] avg %,d ns  (%.3f ms) — DO NOT use in production%n",
                avgNs, avgNs / 1_000_000.0);
        assertTrue(true);
    }

    // ---- recommended: cache Program, only eval() on hot path ----

    @Test
    void cost_cachedProgram_evalOnly() throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("age",  SimpleType.DYN)
                .addVar("role", SimpleType.DYN)
                .build();
        CelAbstractSyntaxTree ast = cel.compile(EXPR).getAst();
        CelRuntime.Program program = cel.createProgram(ast);   // cached once

        Map<String, Object> vars = Map.of("age", 25L, "role", "admin");

        // Warm up
        for (int i = 0; i < WARM_UP; i++) {
            program.eval(vars);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            program.eval(vars);
        }
        long avgNs = (System.nanoTime() - start) / ROUNDS;

        System.out.printf("[cached-program] avg %,d ns  (%.3f ms) — recommended%n",
                avgNs, avgNs / 1_000_000.0);
        assertTrue(true);
    }
}
