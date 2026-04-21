package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Use cases for CEL (Common Expression Language) evaluation.
 * All examples use a single "params" variable of type map(string, dyn),
 * which supports arbitrary flat and nested key-value structures.
 */
class CelUseCaseTest {

    /**
     * Shared CEL instance configured with a single map variable "params".
     * map(string, dyn) allows values of any type, including nested maps.
     */
    private Cel cel;

    @BeforeEach
    void setUp() throws Exception {
        cel = CelFactory.standardCelBuilder()
                .addVar("params", MapType.create(SimpleType.STRING, SimpleType.DYN))
                .build();
    }

    // ---- Flat map ----

    @Test
    void flatMap_longComparison_true() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.age > 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("age", 20L)));

        assertTrue((Boolean) result);
    }

    @Test
    void flatMap_longComparison_false() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.age > 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("age", 16L)));

        assertFalse((Boolean) result);
    }

    @Test
    void flatMap_stringEquality() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.status == \"active\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("status", "active")));

        assertTrue((Boolean) result);
    }

    @Test
    void flatMap_booleanField() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.isVip == true").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("isVip", true)));

        assertTrue((Boolean) result);
    }

    @Test
    void flatMap_doubleComparison() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.score >= 90.0").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("score", 95.5)));

        assertTrue((Boolean) result);
    }

    // ---- Nested map (2 levels) ----

    @Test
    void nestedMap_twoLevels_numericComparison() throws Exception {
        // params.user.age > 18
        CelAbstractSyntaxTree ast = cel.compile("params.user.age > 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "user", Map.of("age", 20L)
        );

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    @Test
    void nestedMap_twoLevels_stringComparison() throws Exception {
        // params.user.role == "admin"
        CelAbstractSyntaxTree ast = cel.compile("params.user.role == \"admin\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "user", Map.of("role", "admin")
        );

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    // ---- Null / missing intermediate field ----

    @Test
    void nestedMap_intermediateKeyMissing_throwsAtEval() throws Exception {
        // params.user.address.city — "address" key does not exist in user map
        CelAbstractSyntaxTree ast = cel.compile("params.user.address.city == \"Beijing\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "user", Map.of("name", "frank")   // no "address" key
        );
        assertThrows(Exception.class, () -> program.eval(Map.of("params", params)));
    }

    @Test
    void nestedMap_intermediateKeyMissing_nullCheckExpression_throwsAtEval() throws Exception {
        // even a null check on the leaf still throws if an intermediate key is missing
        CelAbstractSyntaxTree ast = cel.compile("params.user.address == null").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "user", Map.of("name", "frank")   // no "address" key
        );

        assertThrows(Exception.class, () -> program.eval(Map.of("params", params)));
    }

    @Test
    void nestedMap_existenceGuard_preventsException() throws Exception {
        // guard with "in" operator before accessing deeper field
        CelAbstractSyntaxTree ast = cel.compile(
                "\"address\" in params.user && params.user.address.city == \"Beijing\""
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> paramsWithout = Map.of("user", Map.of("name", "frank"));
        Map<String, Object> paramsWith    = Map.of("user", Map.of("address", Map.of("city", "Beijing")));

        assertFalse((Boolean) program.eval(Map.of("params", paramsWithout)));
        assertTrue((Boolean)  program.eval(Map.of("params", paramsWith)));
    }

    // TODO: For dotted-path field access (e.g. user.address.city), every segment — both
    //       intermediate and the final field — must exist in the map at eval time. A missing
    //       segment at any position causes a runtime exception. Callers are responsible for
    //       ensuring full path existence, or use existence guards as shown above. Document this.

    // ---- List / array access ----

    @Test
    void list_indexAccess_returnsElement() throws Exception {
        // params.items[0] == "a"
        CelAbstractSyntaxTree ast = cel.compile("params.items[0] == \"a\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("items", java.util.List.of("a", "b", "c"))));

        assertTrue((Boolean) result);
    }

    @Test
    void list_indexAccess_nestedField() throws Exception {
        // params.orders[0].price > 100.0
        CelAbstractSyntaxTree ast = cel.compile("params.orders[0].price > 100.0").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of(
                "orders", java.util.List.of(Map.of("price", 150.0))
        )));

        assertTrue((Boolean) result);
    }

    @Test
    void list_outOfBounds_throwsAtEval() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.items[5] == \"a\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        assertThrows(Exception.class, () -> program.eval(
                Map.of("params", Map.of("items", java.util.List.of("a", "b")))
        ));
    }

    // ---- Deep nested map (3 levels) ----

    @Test
    void nestedMap_threeLevels_numericComparison() throws Exception {
        // params.order.item.price > 100.0
        CelAbstractSyntaxTree ast = cel.compile("params.order.item.price > 100.0").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "order", Map.of(
                        "item", Map.of("price", 150.0)
                )
        );

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    @Test
    void nestedMap_threeLevels_stringComparison() throws Exception {
        // params.user.address.city == "Beijing"
        CelAbstractSyntaxTree ast = cel.compile("params.user.address.city == \"Beijing\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "user", Map.of(
                        "address", Map.of("city", "Beijing")
                )
        );

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    // ---- Combined expressions ----

    @Test
    void combined_andExpression() throws Exception {
        // params.age > 18 && params.status == "active"
        CelAbstractSyntaxTree ast = cel.compile("params.age > 18 && params.status == \"active\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of("age", 25L, "status", "active");

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    @Test
    void combined_andExpression_failsWhenOneConditionFails() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("params.age > 18 && params.status == \"active\"").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of("age", 25L, "status", "inactive");

        Object result = program.eval(Map.of("params", params));

        assertFalse((Boolean) result);
    }

    @Test
    void combined_orExpression() throws Exception {
        // params.role == "admin" || params.level > 5
        CelAbstractSyntaxTree ast = cel.compile("params.role == \"admin\" || params.level > 5").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of("role", "user", "level", 8L);

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    // ---- Bracket syntax ----

    @Test
    void bracketSyntax_flatAccess() throws Exception {
        // params["age"] > 18
        CelAbstractSyntaxTree ast = cel.compile("params[\"age\"] > 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("age", 20L)));

        assertTrue((Boolean) result);
    }

    @Test
    void bracketSyntax_nestedAccess() throws Exception {
        // params["user"]["age"] > 18
        CelAbstractSyntaxTree ast = cel.compile("params[\"user\"][\"age\"] > 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of(
                "user", Map.of("age", 20L)
        );

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    // ---- field existence ----

    @Test
    void fieldExists_in_presentKey_returnsTrue() throws Exception {
        // "age" in params — true when the key exists in the map
        CelAbstractSyntaxTree ast = cel.compile("\"age\" in params").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("age", 30L)));

        assertTrue((Boolean) result);
    }

    @Test
    void fieldExists_in_absentKey_returnsFalse() throws Exception {
        // "age" in params — false when the key is absent
        CelAbstractSyntaxTree ast = cel.compile("\"age\" in params").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("name", "Alice")));

        assertFalse((Boolean) result);
    }

    @Test
    void fieldExists_in_nestedMap_presentKey() throws Exception {
        // "role" in params.user — true when the nested map contains the key
        CelAbstractSyntaxTree ast = cel.compile("\"role\" in params.user").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of("user", Map.of("role", "admin", "age", 25L));

        Object result = program.eval(Map.of("params", params));

        assertTrue((Boolean) result);
    }

    @Test
    void fieldExists_in_nestedMap_absentKey() throws Exception {
        // "role" in params.user — false when the nested map does not contain the key
        CelAbstractSyntaxTree ast = cel.compile("\"role\" in params.user").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Map<String, Object> params = Map.of("user", Map.of("age", 25L));

        Object result = program.eval(Map.of("params", params));

        assertFalse((Boolean) result);
    }

    @Test
    void fieldExists_combined_existenceAndValueCheck() throws Exception {
        // "discount" in params && params.discount > 0.0 — guard key existence before accessing value
        CelAbstractSyntaxTree ast = cel.compile("\"discount\" in params && params.discount > 0.0").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object resultPresent = program.eval(Map.of("params", Map.of("discount", 10.0)));
        Object resultAbsent  = program.eval(Map.of("params", Map.of("price", 50.0)));

        assertTrue((Boolean) resultPresent);
        assertFalse((Boolean) resultAbsent);
    }

    // ---- string functions ----

    @Test
    void stringFunc_endsWith_true() throws Exception {
        // params.email.endsWith("@gmail.com") — true when the string ends with the suffix
        CelAbstractSyntaxTree ast = cel.compile("params.email.endsWith(\"@gmail.com\")").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("email", "alice@gmail.com")));

        assertTrue((Boolean) result);
    }

    @Test
    void stringFunc_endsWith_false() throws Exception {
        // false when the string does not end with the suffix
        CelAbstractSyntaxTree ast = cel.compile("params.email.endsWith(\"@gmail.com\")").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("params", Map.of("email", "bob@outlook.com")));

        assertFalse((Boolean) result);
    }

    @Test
    void stringFunc_endsWith_combined() throws Exception {
        // guard with endsWith before checking another field
        CelAbstractSyntaxTree ast = cel.compile(
                "params.email.endsWith(\"@gmail.com\") && params.age >= 18"
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object resultPass = program.eval(Map.of("params", Map.of("email", "alice@gmail.com", "age", 20L)));
        Object resultFail = program.eval(Map.of("params", Map.of("email", "alice@gmail.com", "age", 16L)));

        assertTrue((Boolean) resultPass);
        assertFalse((Boolean) resultFail);
    }

}
