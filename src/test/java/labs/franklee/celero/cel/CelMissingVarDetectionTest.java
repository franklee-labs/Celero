package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelErrorCode;
import dev.cel.common.CelOptions;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two approaches to distinguish "false" from "missing variable" during CEL evaluation.
 *
 * Key observations (CEL Java 0.12.0):
 *   - Missing top-level DYN variable: eval() returns CelUnknownSet (no exception thrown)
 *   - Missing map key (params.age on map without "age"): throws CelEvaluationException(ATTRIBUTE_NOT_FOUND)
 *   - Short circuit: false && <missing> → Boolean false (missing var never evaluated)
 *   - Short circuit: <missing> && true  → CelUnknownSet (missing var evaluated, propagated)
 *
 * Approach A — Standard eval() + result inspection:
 *   Call program.eval(Map). Check result instanceof CelUnknownSet for missing top-level vars.
 *   Catch CelEvaluationException(ATTRIBUTE_NOT_FOUND) for missing map keys.
 *   Zero overhead on the happy path; no API changes.
 *
 * Approach B — advanceEvaluation() + CelVariableResolver:
 *   Enable CelOptions.enableUnknownTracking(true), use program.advanceEvaluation(UnknownContext).
 *   Same CelUnknownSet behavior for top-level vars; still throws for missing map keys.
 *   Bonus: CelAttributePattern explicitly marks vars as unknown and populates
 *   CelUnknownSet.attributes() with the variable name for richer diagnostics.
 */
class CelMissingVarDetectionTest {

    // ---- shared helpers ----

    private static Cel buildStandardCel(String... varNames) throws Exception {
        var builder = CelFactory.standardCelBuilder();
        for (String v : varNames) builder.addVar(v, SimpleType.DYN);
        return builder.build();
    }

    private static Cel buildMapCel() throws Exception {
        return CelFactory.standardCelBuilder()
                .addVar("params", MapType.create(SimpleType.STRING, SimpleType.DYN))
                .build();
    }

    private static Cel buildUnknownTrackingCel(String... varNames) throws Exception {
        var builder = CelFactory.standardCelBuilder()
                .setOptions(CelOptions.current().enableUnknownTracking(true).build());
        for (String v : varNames) builder.addVar(v, SimpleType.DYN);
        return builder.build();
    }

    // ==================== Approach A: Standard eval() + result inspection ====================

    @Nested
    class ApproachA_StandardEval {

        /**
         * Wraps program.eval(Map):
         *   - Returns true/false for normal Boolean results.
         *   - Throws MissingVarException when a top-level DYN variable is absent
         *     (eval returns CelUnknownSet instead of Boolean).
         *   - Throws MissingVarException when a map key is absent
         *     (eval throws CelEvaluationException with ATTRIBUTE_NOT_FOUND).
         */
        static boolean eval(CelRuntime.Program program, Map<String, Object> vars) throws Exception {
            try {
                Object result = program.eval(vars);
                if (result instanceof CelUnknownSet) {
                    throw new MissingVarException("missing top-level variable(s)");
                }
                return result instanceof Boolean b && b;
            } catch (CelEvaluationException e) {
                if (e.getErrorCode() == CelErrorCode.ATTRIBUTE_NOT_FOUND) {
                    throw new MissingVarException(e.getMessage(), e);
                }
                throw e;
            }
        }

        // ---- top-level DYN variable ----

        @Test
        void topLevel_returnsTrue() throws Exception {
            var cel = buildStandardCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());
            assertTrue(eval(prog, Map.of("age", 25L)));
        }

        @Test
        void topLevel_returnsFalse() throws Exception {
            var cel = buildStandardCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());
            assertFalse(eval(prog, Map.of("age", 16L)));
        }

        @Test
        void topLevel_missingVar_throwsMissingVarException_notFalse() throws Exception {
            var cel = buildStandardCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            assertThrows(MissingVarException.class, () -> eval(prog, Map.of()));
        }

        @Test
        void topLevel_missingVar_evalReturnsCelUnknownSet_notBoolean() throws Exception {
            // Verify the raw return value to explain WHY the wrapper throws
            var cel = buildStandardCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            Object raw = prog.eval(Map.of());

            assertInstanceOf(CelUnknownSet.class, raw,
                    "CEL returns CelUnknownSet (not false) when a DYN variable is absent");
        }

        // ---- map key access ----

        @Test
        void mapKey_returnsTrue() throws Exception {
            var cel = buildMapCel();
            var prog = cel.createProgram(cel.compile("params.age >= 18").getAst());
            assertTrue(eval(prog, Map.of("params", Map.of("age", 25L))));
        }

        @Test
        void mapKey_returnsFalse() throws Exception {
            var cel = buildMapCel();
            var prog = cel.createProgram(cel.compile("params.age >= 18").getAst());
            assertFalse(eval(prog, Map.of("params", Map.of("age", 16L))));
        }

        @Test
        void mapKey_missingKey_throwsMissingVarException() throws Exception {
            var cel = buildMapCel();
            var prog = cel.createProgram(cel.compile("params.age >= 18").getAst());

            // params is present but "age" key is absent → ATTRIBUTE_NOT_FOUND
            assertThrows(MissingVarException.class,
                    () -> eval(prog, Map.of("params", Map.of("name", "frank"))));
        }

        @Test
        void mapKey_missingKey_rawThrowsCelEvaluationExceptionWithAttributeNotFound() throws Exception {
            // Verify the raw exception to explain WHY the wrapper throws for map key misses
            var cel = buildMapCel();
            var prog = cel.createProgram(cel.compile("params.age >= 18").getAst());

            CelEvaluationException ex = assertThrows(CelEvaluationException.class,
                    () -> prog.eval(Map.of("params", Map.of("name", "frank"))));

            assertEquals(CelErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        }

        // ---- AND short-circuit behaviour ----

        @Test
        void andShortCircuit_falseBeforeMissing_returnsFalse() throws Exception {
            // false && <missing> — right side is never evaluated; result is false, not missing
            var cel = buildStandardCel("a", "b");
            var prog = cel.createProgram(cel.compile("a > 100 && b > 0").getAst());

            assertFalse(eval(prog, Map.of("a", 0L)));  // b absent, but short-circuited
        }

        @Test
        void andShortCircuit_missingBeforeEvaluated_throwsMissingVar() throws Exception {
            // <missing> && anything — left side evaluates to CelUnknownSet
            var cel = buildStandardCel("a", "b");
            var prog = cel.createProgram(cel.compile("a > 0 && b > 0").getAst());

            assertThrows(MissingVarException.class,
                    () -> eval(prog, Map.of("b", 5L)));  // a is absent
        }

        @Test
        void andShortCircuit_falseBeforeMissing_ignoreOrder_returnsFalse() throws Exception {
            // <missing> && anything — left side evaluates to CelUnknownSet
            var cel = buildStandardCel("a", "b");
            var prog = cel.createProgram(cel.compile("a > 0 && b > 0").getAst());

            assertFalse(eval(prog, Map.of("b", -5L)));  // a absent, but short-circuited although a is written before b
        }
    }

    // ==================== Approach B: advanceEvaluation() + CelVariableResolver ====================

    @Nested
    class ApproachB_AdvanceEvaluation {

        /**
         * Wraps program.advanceEvaluation(UnknownContext):
         *   - Returns true/false for normal Boolean results.
         *   - Returns CelUnknownSet when a top-level variable is absent
         *     (resolver returns Optional.empty() → null → CelUnknownSet).
         *   - Still throws CelEvaluationException(ATTRIBUTE_NOT_FOUND) for missing map keys.
         *
         * The advantage over Approach A: the CelVariableResolver is explicit and injectable,
         * making it easier to test, mock, or extend (e.g. lazy-loading, per-var error handling).
         */
        static Object eval(Cel.Program program, Map<String, Object> vars) throws Exception {
            CelVariableResolver resolver = name -> Optional.ofNullable(vars.get(name));
            UnknownContext ctx = UnknownContext.create(resolver, List.of());
            return program.advanceEvaluation(ctx);
        }

        // ---- top-level DYN variable ----

        @Test
        void topLevel_returnsTrue() throws Exception {
            var cel = buildUnknownTrackingCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            Object result = eval(prog, Map.of("age", 25L));
            assertTrue((Boolean) result);
        }

        @Test
        void topLevel_returnsFalse() throws Exception {
            var cel = buildUnknownTrackingCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            Object result = eval(prog, Map.of("age", 16L));
            assertFalse((Boolean) result);
        }

        @Test
        void topLevel_missingVar_returnsCelUnknownSet_notBoolean() throws Exception {
            var cel = buildUnknownTrackingCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            Object result = eval(prog, Map.of());

            assertInstanceOf(CelUnknownSet.class, result);
        }

        @Test
        void topLevel_missingVar_clearlyDistinguishableFromFalse() throws Exception {
            var cel = buildUnknownTrackingCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            Object missingResult = eval(prog, Map.of());
            Object falseResult   = eval(prog, Map.of("age", 16L));

            assertInstanceOf(CelUnknownSet.class, missingResult);
            assertInstanceOf(Boolean.class, falseResult);
        }

        // ---- CelAttributePattern: populates CelUnknownSet.attributes() with the variable name ----

        @Test
        void attributePattern_missingVar_populatesAttributesWithVarName() throws Exception {
            // Without CelAttributePattern: attributes() is empty, only unknownExprIds() is populated.
            // With CelAttributePattern: attributes() contains the declared var name.
            var cel = buildUnknownTrackingCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            CelVariableResolver resolver = name -> Optional.empty(); // always missing
            UnknownContext ctx = UnknownContext.create(resolver,
                    List.of(CelAttributePattern.create("age")));

            Object result = prog.advanceEvaluation(ctx);

            CelUnknownSet unknowns = assertInstanceOf(CelUnknownSet.class, result);
            assertTrue(
                    unknowns.attributes().stream()
                            .anyMatch(attr -> attr.toString().contains("age")),
                    "CelAttributePattern causes the missing var name to appear in attributes()"
            );
        }

        @Test
        void withoutAttributePattern_missingVar_onlyExprIdsPopulated() throws Exception {
            // Without CelAttributePattern, attributes() is empty; only unknownExprIds() is set.
            var cel = buildUnknownTrackingCel("age");
            var prog = cel.createProgram(cel.compile("age >= 18").getAst());

            Object result = eval(prog, Map.of()); // no pattern declared

            CelUnknownSet unknowns = assertInstanceOf(CelUnknownSet.class, result);
            assertTrue(unknowns.attributes().isEmpty(),
                    "Without CelAttributePattern, attributes() is empty");
            assertFalse(unknowns.unknownExprIds().isEmpty(),
                    "unknownExprIds() is always populated for missing vars");
        }

        // ---- map key access: same exception behavior as Approach A ----

        @Test
        void mapKey_missingKey_stillThrowsCelEvaluationException() throws Exception {
            // advanceEvaluation does NOT suppress CelEvaluationException for missing map keys.
            // Only top-level variable resolution goes through the resolver → CelUnknownSet path.
            var cel = CelFactory.standardCelBuilder()
                    .setOptions(CelOptions.current().enableUnknownTracking(true).build())
                    .addVar("params", MapType.create(SimpleType.STRING, SimpleType.DYN))
                    .build();
            var prog = cel.createProgram(cel.compile("params.age >= 18").getAst());

            CelVariableResolver resolver = name -> Optional.ofNullable(
                    Map.<String, Object>of("params", Map.of("name", "frank")).get(name));
            UnknownContext ctx = UnknownContext.create(resolver, List.of());

            CelEvaluationException ex = assertThrows(CelEvaluationException.class,
                    () -> prog.advanceEvaluation(ctx));
            assertEquals(CelErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void chainedMapKey_innerFieldMissing_stillThrowsCelEvaluationException() throws Exception {
            // params.user.age: params and user exist, but "age" key is absent inside user.
            // advanceEvaluation cannot convert a chained map key miss to CelUnknownSet;
            // it still throws CelEvaluationException(ATTRIBUTE_NOT_FOUND), same as Approach A.
            var cel = CelFactory.standardCelBuilder()
                    .setOptions(CelOptions.current().enableUnknownTracking(true).build())
                    .addVar("params", MapType.create(SimpleType.STRING, SimpleType.DYN))
                    .build();
            var prog = cel.createProgram(cel.compile("params.user.age >= 18").getAst());

            // params and user are present; only the "age" key is missing
            Map<String, Object> vars = Map.of("params", Map.of("user", Map.of("name", "frank")));
            CelVariableResolver resolver = name -> Optional.ofNullable(vars.get(name));
            UnknownContext ctx = UnknownContext.create(resolver, List.of());

            CelEvaluationException ex = assertThrows(CelEvaluationException.class,
                    () -> prog.advanceEvaluation(ctx));
            assertEquals(CelErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        }

        // ---- AND short-circuit behaviour ----

        @Test
        void andShortCircuit_falseBeforeMissing_returnsFalse() throws Exception {
            var cel = buildUnknownTrackingCel("a", "b");
            var prog = cel.createProgram(cel.compile("a > 100 && b > 0").getAst());

            Object result = eval(prog, Map.of("a", 0L)); // b absent, short-circuited

            assertInstanceOf(Boolean.class, result);
            assertFalse((Boolean) result);
        }

        @Test
        void andShortCircuit_missingBeforeEvaluated_returnsCelUnknownSet() throws Exception {
            var cel = buildUnknownTrackingCel("a", "b");
            var prog = cel.createProgram(cel.compile("a > 0 && b > 0").getAst());

            Object result = eval(prog, Map.of("b", 5L)); // a absent

            assertInstanceOf(CelUnknownSet.class, result);
        }
    }

    // ---- custom exception used by Approach A ----

    static class MissingVarException extends RuntimeException {
        MissingVarException(String message) {
            super(message);
        }
        MissingVarException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
