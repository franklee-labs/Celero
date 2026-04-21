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
 * Use cases for CEL evaluation with multiple top-level variables.
 * Unlike CelUseCaseTest (which bundles everything into a single "params" map),
 * these tests declare each domain object as its own typed variable,
 * e.g. "user", "order", "context".
 */
class CelMultiVarTest {

    /**
     * CEL instance with three independent map variables:
     *   user    — map(string, dyn)  e.g. { "age": 25, "role": "admin" }
     *   order   — map(string, dyn)  e.g. { "amount": 500.0, "status": "paid" }
     *   context — map(string, dyn)  e.g. { "region": "CN", "isPremium": true }
     */
    private Cel cel;

    @BeforeEach
    void setUp() throws Exception {
        cel = CelFactory.standardCelBuilder()
                .addVar("user",    MapType.create(SimpleType.STRING, SimpleType.DYN))
                .addVar("order",   MapType.create(SimpleType.STRING, SimpleType.DYN))
                .addVar("context", MapType.create(SimpleType.STRING, SimpleType.DYN))
                .build();
    }

    // ---- single variable ----

    @Test
    void singleVar_user_ageComparison() throws Exception {
        // Evaluate an expression that only references "user"
        CelAbstractSyntaxTree ast = cel.compile("user.age >= 18").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("age", 20L),
                "order",   Map.of(),
                "context", Map.of()
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void singleVar_order_amountComparison() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("order.amount > 100.0").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of(),
                "order",   Map.of("amount", 500.0),
                "context", Map.of()
        ));

        assertTrue((Boolean) result);
    }

    // ---- cross-variable AND ----

    @Test
    void crossVar_userAndOrder_bothConditionsMet() throws Exception {
        // user must be an adult AND order amount must exceed threshold
        CelAbstractSyntaxTree ast = cel.compile(
                "user.age >= 18 && order.amount > 100.0"
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("age", 25L),
                "order",   Map.of("amount", 500.0),
                "context", Map.of()
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void crossVar_userAndOrder_oneConditionFails() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile(
                "user.age >= 18 && order.amount > 100.0"
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("age", 16L),   // fails
                "order",   Map.of("amount", 500.0),
                "context", Map.of()
        ));

        assertFalse((Boolean) result);
    }

    // ---- cross-variable OR ----

    @Test
    void crossVar_userOrContext_eitherConditionSuffices() throws Exception {
        // Approve if user is admin OR region is whitelisted
        CelAbstractSyntaxTree ast = cel.compile(
                "user.role == \"admin\" || context.region == \"CN\""
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        // user is not admin, but region matches
        Object result = program.eval(Map.of(
                "user",    Map.of("role", "member"),
                "order",   Map.of(),
                "context", Map.of("region", "CN")
        ));

        assertTrue((Boolean) result);
    }

    // ---- three variables combined ----

    @Test
    void threeVars_combined_allConditionsMet() throws Exception {
        // Adult premium user with a paid order over 200
        CelAbstractSyntaxTree ast = cel.compile(
                "user.age >= 18 && context.isPremium == true && order.amount > 200.0"
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("age", 30L),
                "order",   Map.of("amount", 350.0),
                "context", Map.of("isPremium", true)
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void threeVars_combined_premiumFlagMissing() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile(
                "user.age >= 18 && context.isPremium == true && order.amount > 200.0"
        ).getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        // isPremium is false
        Object result = program.eval(Map.of(
                "user",    Map.of("age", 30L),
                "order",   Map.of("amount", 350.0),
                "context", Map.of("isPremium", false)
        ));

        assertFalse((Boolean) result);
    }

    // ---- field existence across variables ----

    @Test
    void fieldExists_inUser_presentKey() throws Exception {
        // Check whether "role" field is present in "user"
        CelAbstractSyntaxTree ast = cel.compile("\"role\" in user").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("age", 25L, "role", "admin"),
                "order",   Map.of(),
                "context", Map.of()
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void fieldExists_inOrder_absentKey() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("\"discount\" in order").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of(),
                "order",   Map.of("amount", 100.0),
                "context", Map.of()
        ));

        assertFalse((Boolean) result);
    }

    @Test
    void fieldExists_orderContext_compareValue() throws Exception {
        CelAbstractSyntaxTree ast = cel.compile("user.balance > order.price").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("balance", 100.0),
                "order",   Map.of("price", 90.0),
                "context", Map.of()
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void fieldExists_lackOrderParam_notUsed() throws Exception {
        // declared order param is not passed
        CelAbstractSyntaxTree ast = cel.compile("\"role\" in user").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of(
                "user",    Map.of("age", 25L, "role", "admin"),
                "context", Map.of()
        ));

        assertTrue((Boolean) result);
    }

    @Test
    void declaredVar_notPassedInEval_returnsUnknownSet() throws Exception {
        // "user" is declared but not present in the eval activation map.
        // CEL does NOT throw — it returns a CelUnknownSet wrapping the unresolved expr IDs.
        // Casting the result to Boolean causes ClassCastException.
        CelAbstractSyntaxTree ast = cel.compile("\"role\" in user").getAst();
        CelRuntime.Program program = cel.createProgram(ast);

        Object result = program.eval(Map.of("context", Map.of()));

        System.out.println("result type : " + result.getClass().getSimpleName());
        System.out.println("result value: " + result);

        // result is a CelUnknownSet, not a Boolean
        assertFalse(result instanceof Boolean);
    }
}
