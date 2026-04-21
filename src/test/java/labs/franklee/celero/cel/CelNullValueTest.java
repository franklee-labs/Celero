package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelUnknownSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Explores CEL behavior when a DYN-typed variable is present in the map but its value is null.
 *
 * Key findings (CEL Java, DYN-typed variables):
 *   - null value → eval() returns CelUnknownSet — SAME as absent variable
 *   - CEL cannot distinguish "key present, value null" from "key absent" for DYN vars
 *   - null == null, null > 18, null == "x" all return CelUnknownSet (not boolean, not exception)
 *
 * Conclusion:
 *   Stripping null values from the context map is safe — CEL would treat them identically
 *   to absent variables anyway. There is no information loss from the CEL evaluation perspective.
 */
class CelNullValueTest {

    private static Cel buildDynCel(String... varNames) throws Exception {
        var builder = CelFactory.standardCelBuilder();
        for (String v : varNames) builder.addVar(v, SimpleType.DYN);
        return builder.build();
    }

    private static Map<String, Object> mapWithNull(String key) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, null);
        return m;
    }

    // ==================== null value == absent: both return CelUnknownSet ====================

    @Test
    void nullValue_evalReturnsCelUnknownSet_sameAsAbsent() throws Exception {
        // key present, value null → CelUnknownSet, indistinguishable from absent
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age").getAst());

        Object result = prog.eval(mapWithNull("age"));
        assertInstanceOf(CelUnknownSet.class, result,
                "null value produces CelUnknownSet — same as absent variable");
    }

    @Test
    void absentVar_evalReturnsCelUnknownSet() throws Exception {
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age").getAst());

        Object result = prog.eval(Map.of());
        assertInstanceOf(CelUnknownSet.class, result);
    }

    @Test
    void nullValue_indistinguishableFromAbsent() throws Exception {
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age").getAst());

        Object nullResult   = prog.eval(mapWithNull("age"));
        Object absentResult = prog.eval(Map.of());

        // both are CelUnknownSet — CEL treats null-value and absent-key identically for DYN vars
        assertInstanceOf(CelUnknownSet.class, nullResult);
        assertInstanceOf(CelUnknownSet.class, absentResult);
    }

    // ==================== null in expressions: all return CelUnknownSet ====================

    @Test
    void nullValue_equalsNull_returnsCelUnknownSet_notTrue() throws Exception {
        // Might expect true, but CEL treats null DYN var as unknown → CelUnknownSet
        Cel cel = buildDynCel("role");
        var prog = cel.createProgram(cel.compile("role == null").getAst());

        Object result = prog.eval(mapWithNull("role"));
        assertInstanceOf(CelUnknownSet.class, result,
                "null == null for DYN var returns CelUnknownSet, not Boolean true");
    }

    @Test
    void nullValue_greaterThan_returnsCelUnknownSet_noException() throws Exception {
        // null > 18: does NOT throw — returns CelUnknownSet (unknown propagates through expression)
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age > 18").getAst());

        Object result = prog.eval(mapWithNull("age"));
        assertInstanceOf(CelUnknownSet.class, result,
                "null value in comparison returns CelUnknownSet, not a type error exception");
    }

    @Test
    void nullValue_stringEquality_returnsCelUnknownSet() throws Exception {
        Cel cel = buildDynCel("role");
        var prog = cel.createProgram(cel.compile("role == \"admin\"").getAst());

        Object result = prog.eval(mapWithNull("role"));
        assertInstanceOf(CelUnknownSet.class, result);
    }

    @Test
    void nullValue_intEquality_returnsCelUnknownSet_noException() throws Exception {
        // null == 18: no type error thrown — unknown propagates
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age == 18").getAst());

        Object result = prog.eval(mapWithNull("age"));
        assertInstanceOf(CelUnknownSet.class, result,
                "null == int for DYN var: no exception, returns CelUnknownSet");
    }

    // ==================== chained field access with null/absent intermediates ====================

    @Test
    void chain_topLevelNull_returnsCelUnknownSet() throws Exception {
        // params = null → params.user.age: top-level null → CelUnknownSet, no exception
        Cel cel = buildDynCel("params");
        var prog = cel.createProgram(cel.compile("params.user.age").getAst());

        Object result = prog.eval(mapWithNull("params"));
        assertInstanceOf(CelUnknownSet.class, result);
    }

    @Test
    void chain_topLevelAbsent_returnsCelUnknownSet() throws Exception {
        // params absent → params.user.age: CelUnknownSet (consistent with top-level null)
        Cel cel = buildDynCel("params");
        var prog = cel.createProgram(cel.compile("params.user.age").getAst());

        Object result = prog.eval(Map.of());
        assertInstanceOf(CelUnknownSet.class, result);
    }

    @Test
    void chain_intermediateKeyAbsent_throwsAttributeNotFound() throws Exception {
        // params present, "user" key absent → ATTRIBUTE_NOT_FOUND exception
        Cel cel = buildDynCel("params");
        var prog = cel.createProgram(cel.compile("params.user.age").getAst());

        Map<String, Object> vars = new HashMap<>();
        vars.put("params", Map.of("role", "admin")); // "user" key missing

        assertThrows(dev.cel.runtime.CelEvaluationException.class,
                () -> prog.eval(vars));
    }

    @Test
    void chain_intermediateValueNull_returnsCelUnknownSet() throws Exception {
        // params.user = null (key exists, value null): null propagates as unknown through chain
        Cel cel = buildDynCel("params");
        var prog = cel.createProgram(cel.compile("params.user.age").getAst());

        Map<String, Object> user = new HashMap<>();
        user.put("user", null);
        Map<String, Object> vars = new HashMap<>();
        vars.put("params", user);

        Object result = prog.eval(vars);
        assertInstanceOf(CelUnknownSet.class, result,
                "null intermediate value propagates as CelUnknownSet — same as null top-level var");
    }

    @Test
    void chain_leafValueNull_returnsCelUnknownSet() throws Exception {
        // params.user.age = null (leaf null): null value at the end also returns CelUnknownSet
        Cel cel = buildDynCel("params");
        var prog = cel.createProgram(cel.compile("params.user.age").getAst());

        Map<String, Object> age = new HashMap<>();
        age.put("age", null);
        Map<String, Object> vars = new HashMap<>();
        vars.put("params", Map.of("user", age));

        Object result = prog.eval(vars);
        assertInstanceOf(CelUnknownSet.class, result,
                "null leaf value returns CelUnknownSet — consistent with null top-level var");
    }

    @Test
    void chain_leafKeyAbsent_throwsAttributeNotFound() throws Exception {
        // params.user present, "age" key absent → ATTRIBUTE_NOT_FOUND
        Cel cel = buildDynCel("params");
        var prog = cel.createProgram(cel.compile("params.user.age").getAst());

        Map<String, Object> vars = new HashMap<>();
        vars.put("params", Map.of("user", Map.of("name", "frank"))); // "age" missing

        assertThrows(dev.cel.runtime.CelEvaluationException.class,
                () -> prog.eval(vars));
    }

    // ==================== stripping null is safe ====================

    @Test
    void strippingNull_producesIdenticalResult() throws Exception {
        // Stripping null-value keys from the map produces the same CelUnknownSet result
        Cel cel = buildDynCel("age");
        var prog = cel.createProgram(cel.compile("age > 18").getAst());

        Object withNull    = prog.eval(mapWithNull("age")); // key present, value null
        Object withStripped = prog.eval(Map.of());           // key stripped (absent)

        // Both CelUnknownSet — stripping null values causes no behavioral difference
        assertInstanceOf(CelUnknownSet.class, withNull);
        assertInstanceOf(CelUnknownSet.class, withStripped);
    }
}
