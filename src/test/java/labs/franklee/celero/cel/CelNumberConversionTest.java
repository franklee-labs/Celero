package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compares longValue() vs longValueExact() when converting BigDecimal
 * to pass as a CEL variable.
 */
class CelNumberConversionTest {

    private CelRuntime.Program compileInt(String expr) throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("key",   SimpleType.DYN)
                .addVar("value", SimpleType.INT)
                .build();
        return cel.createProgram(cel.compile(expr).getAst());
    }

    private CelRuntime.Program compileDouble(String expr) throws Exception {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("key",   SimpleType.DYN)
                .addVar("value", SimpleType.DOUBLE)
                .build();
        return cel.createProgram(cel.compile(expr).getAst());
    }

    // ---- longValue() silently overflows ----

    @Test
    void longValue_overflow_silentlyWrong() throws Exception {
        BigDecimal overflow = new BigDecimal("99999999999999999999"); // > Long.MAX_VALUE
        long silentlyWrong  = overflow.longValue();  // truncated, no exception

        System.out.printf("BigDecimal: %s%n", overflow);
        System.out.printf("longValue(): %d  ← silently wrong%n", silentlyWrong);

        // CEL receives a garbage value — comparison result is meaningless
        // overflow produces a large positive number, so 100 > garbage = false (wrong answer)
        CelRuntime.Program gt = compileInt("key > value");
        Object result = gt.eval(Map.of("key", 100L, "value", silentlyWrong));
        System.out.printf("key(100) > value(%d) → %s  ← comparison is correct, but value is garbage%n", silentlyWrong, result);

        assertFalse((Boolean) result); // 100 > large_garbage = false, semantically wrong
    }

    // ---- longValueExact() fails fast ----

    @Test
    void longValueExact_overflow_throwsImmediately() {
        BigDecimal overflow = new BigDecimal("99999999999999999999");

        // throws ArithmeticException at conversion time, before CEL is involved
        assertThrows(ArithmeticException.class, overflow::longValueExact);
    }

    @Test
    void longValueExact_normalInteger_works() throws Exception {
        BigDecimal bd = new BigDecimal("100");

        CelRuntime.Program eq = compileInt("key == value");
        Object result = eq.eval(Map.of("key", 100L, "value", bd.longValueExact()));

        assertTrue((Boolean) result);
    }

    // ---- doubleValue() for decimals ----

    @Test
    void doubleValue_withinPrecision_works() throws Exception {
        BigDecimal bd = new BigDecimal("99.5");

        CelRuntime.Program gt = compileDouble("key > value");
        Object result = gt.eval(Map.of("key", 100.0, "value", bd.doubleValue()));

        assertTrue((Boolean) result);
    }

    @Test
    void doubleValue_beyondPrecision_lostBeforeCel() throws Exception {
        // digits beyond ~17 significant figures are lost in doubleValue()
        BigDecimal precise    = new BigDecimal("1.12345678901234567890123");
        BigDecimal almostSame = new BigDecimal("1.12345678901234567000000"); // differ at digit 19+

        System.out.printf("precise.doubleValue()    = %.20f%n", precise.doubleValue());
        System.out.printf("almostSame.doubleValue() = %.20f%n", almostSame.doubleValue());

        CelRuntime.Program eq = compileDouble("key == value");
        // both collapse to the same double — CEL sees them as equal
        Object result = eq.eval(Map.of(
                "key",   precise.doubleValue(),
                "value", almostSame.doubleValue()
        ));
        assertTrue((Boolean) result);
    }
}
