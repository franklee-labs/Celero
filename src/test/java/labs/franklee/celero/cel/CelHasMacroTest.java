package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelUnknownSet;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Explores CEL key-existence detection options for ExistsCondition implementation.
 *
 * Key findings:
 *   - has(m.field): compile error for DYN-typed variables — has() only works on proto messages
 *   - "field" in m: works for typed Map<String, DYN> variable; checks key presence safely (no exception)
 *   - Top-level DYN var absent: eval() returns CelUnknownSet (not exception, not false)
 *   - Map key absent via dot access: throws CelEvaluationException(ATTRIBUTE_NOT_FOUND)
 *
 * Implementation recommendation for ExistsCondition:
 *   - For top-level key existence: evaluate key, check result instanceof CelUnknownSet → absent
 *   - For map sub-key existence (params.field): use '"field" in params' expression
 */
class CelHasMacroTest {

    private static Cel buildDynCel(String... varNames) throws Exception {
        var builder = CelFactory.standardCelBuilder();
        for (String v : varNames) builder.addVar(v, SimpleType.DYN);
        return builder.build();
    }

    private static Cel buildMapCel(String... mapVarNames) throws Exception {
        var builder = CelFactory.standardCelBuilder();
        for (String v : mapVarNames)
            builder.addVar(v, MapType.create(SimpleType.STRING, SimpleType.DYN));
        return builder.build();
    }

    // ==================== has() limitation: DYN-typed variables ====================

    @Test
    void has_dynVar_compileFails() {
        // has(params.age) where params is DYN: CEL rejects — has() requires proto message type
        assertThrows(CelValidationException.class, () -> {
            Cel cel = buildDynCel("params");
            cel.compile("has(params.age)").getAst();
        });
    }

    @Test
    void has_bareIdentifier_compileFails() {
        // has(age) where age is a top-level DYN var: also rejected — needs field selection syntax
        assertThrows(CelValidationException.class, () -> {
            Cel cel = buildDynCel("age");
            cel.compile("has(age)").getAst();
        });
    }

    // ==================== "key" in map: correct approach for map key existence ====================

    @Test
    void inOperator_typedMap_keyPresent_returnsTrue() throws Exception {
        // "age" in params checks if "age" key exists in a typed map variable — safe, no exception
        Cel cel = buildMapCel("params");
        var prog = cel.createProgram(cel.compile("\"age\" in params").getAst());

        Object result = prog.eval(Map.of("params", Map.of("age", 25L)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    void inOperator_typedMap_keyAbsent_returnsFalse_noException() throws Exception {
        Cel cel = buildMapCel("params");
        var prog = cel.createProgram(cel.compile("\"age\" in params").getAst());

        Object result = prog.eval(Map.of("params", Map.of("name", "frank")));
        assertTrue(result instanceof Boolean b && !b);
    }

    @Test
    void inOperator_typedMap_emptyMap_returnsFalse() throws Exception {
        Cel cel = buildMapCel("params");
        var prog = cel.createProgram(cel.compile("\"age\" in params").getAst());

        Object result = prog.eval(Map.of("params", Map.of()));
        assertTrue(result instanceof Boolean b && !b);
    }

    @Test
    void inOperator_contrast_directAccess_throws() throws Exception {
        // Without in: accessing a missing map key throws
        Cel cel = buildMapCel("params");
        var prog = cel.createProgram(cel.compile("params.age > 0").getAst());

        assertThrows(Exception.class,
                () -> prog.eval(Map.of("params", Map.of("name", "frank"))));
    }

    // ==================== Top-level DYN variable: CelUnknownSet approach ====================

    @Test
    void topLevelDyn_present_returnsValue() throws Exception {
        // Top-level DYN var present: eval() returns the actual value
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age").getAst());

        Object result = prog.eval(Map.of("age", 25L));
        assertEquals(25L, result);
    }

    @Test
    void topLevelDyn_absent_returnsCelUnknownSet() throws Exception {
        // Top-level DYN var absent: eval() returns CelUnknownSet — not false, not an exception
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age").getAst());

        Object result = prog.eval(Map.of());
        assertInstanceOf(CelUnknownSet.class, result,
                "absent top-level DYN var returns CelUnknownSet, distinguishable from any real value");
    }

    @Test
    void topLevelDyn_absent_distinguishedFromFalseAndNull() throws Exception {
        Cel cel = buildDynCel("flag");
        var prog = cel.createProgram(cel.compile("flag").getAst());

        // flag=false → Boolean false
        Object falseResult = prog.eval(Map.of("flag", false));
        // flag absent → CelUnknownSet
        Object missingResult = prog.eval(Map.of());

        assertInstanceOf(Boolean.class, falseResult);
        assertInstanceOf(CelUnknownSet.class, missingResult);
        assertNotEquals(falseResult.getClass(), missingResult.getClass());
    }

    // ==================== Combined: safe existence + value check ====================

    @Test
    void inOperator_combined_existsAndValue() throws Exception {
        // "age" in params && params.age > 18: safe because in-check short-circuits dot access
        Cel cel = buildMapCel("params");
        var prog = cel.createProgram(
                cel.compile("\"age\" in params && params.age > 18").getAst());

        Object present = prog.eval(Map.of("params", Map.of("age", 25L)));
        Object absent  = prog.eval(Map.of("params", Map.of("name", "frank")));

        assertTrue(present instanceof Boolean b && b);
        assertTrue(absent instanceof Boolean b && !b);
    }
}
