package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Explores CEL's floating point precision behavior.
 * CEL uses IEEE 754 double precision — same as Java's double.
 */
class CelFloatPrecisionTest {

    private Cel cel;

    @BeforeEach
    void setUp() throws Exception {
        cel = CelFactory.standardCelBuilder()
                .addVar("a", SimpleType.DOUBLE)
                .addVar("b", SimpleType.DOUBLE)
                .build();
    }

    private CelRuntime.Program compile(String expr) throws Exception {
        return cel.createProgram(cel.compile(expr).getAst());
    }

    private Object eval(CelRuntime.Program p, double a, double b) throws Exception {
        return p.eval(Map.of("a", a, "b", b));
    }

    // ---- classic floating point pitfalls ----

    @Test
    void classicPitfall_0_1_plus_0_2_notEqualTo_0_3() throws Exception {
        // 0.1 + 0.2 in IEEE 754 is not exactly 0.3
        CelRuntime.Program eq = compile("a + b == 0.3");

        Object result = eval(eq, 0.1, 0.2);
        System.out.printf("0.1 + 0.2 == 0.3  →  %s  (actual sum: %.20f)%n",
                result, 0.1 + 0.2);

        // CEL inherits the same IEEE 754 behavior — this is false
        assertFalse((Boolean) result);
    }

    @Test
    void classicPitfall_magnitude_affects_precision() throws Exception {
        // Adding a tiny value to a large one may have no effect
        CelRuntime.Program eq = compile("a + b == a");

        double large = 1_000_000_000_000_000.0;
        double tiny  = 0.001;
        Object result = eval(eq, large, tiny);
        System.out.printf("%.0f + %f == %.0f  →  %s%n", large, tiny, large, result);

        assertTrue((Boolean) result);  // tiny is lost in the large magnitude
    }

    // ---- BigDecimal → double precision loss ----

    @Test
    void bigDecimalToDouble_precisionLost_beforeReachingCel() throws Exception {
        // BigDecimal stores the value exactly; converting to double may lose precision
        BigDecimal precise = new BigDecimal("1.12345678901234567890");
        double asDouble    = precise.doubleValue();
        System.out.printf("BigDecimal: %s%nAs double:  %.20f%n", precise, asDouble);

        CelRuntime.Program eq = compile("a == b");
        // Two BigDecimals that differ only beyond double precision collapse to the same double
        BigDecimal another = new BigDecimal("1.12345678901234568000");
        Object result = eval(eq, asDouble, another.doubleValue());
        System.out.printf("distinct BigDecimals equal after double conversion: %s%n", result);

        assertTrue((Boolean) result);  // lost in double precision — CEL sees them as equal
    }

    // ---- safe comparison patterns ----

    @Test
    void comparison_greaterThan_unaffectedBySmallError() throws Exception {
        // Ordering comparisons are generally safe when values are clearly apart
        CelRuntime.Program gt = compile("a > b");

        assertTrue((Boolean) eval(gt, 0.2, 0.1));
        assertFalse((Boolean) eval(gt, 0.1, 0.2));
    }

    @Test
    void comparison_epsilon_pattern_worksInCel() throws Exception {
        // Idiomatic workaround: check that |a - b| < epsilon instead of a == b
        Cel epsilonCel = CelFactory.standardCelBuilder()
                .addVar("a",       SimpleType.DOUBLE)
                .addVar("b",       SimpleType.DOUBLE)
                .addVar("epsilon", SimpleType.DOUBLE)
                .build();

        CelRuntime.Program withinEpsilon = epsilonCel.createProgram(
                epsilonCel.compile("(a - b) < epsilon && (b - a) < epsilon").getAst()
        );

        double sum     = 0.1 + 0.2;          // not exactly 0.3
        double epsilon = 1e-9;

        Object result = withinEpsilon.eval(Map.of("a", sum, "b", 0.3, "epsilon", epsilon));
        System.out.printf("|%.20f - 0.3| < %e  →  %s%n", sum, epsilon, result);

        assertTrue((Boolean) result);
    }

    // ---- passing BigDecimal directly to eval ----

    @Test
    void bigDecimalPassedAsDouble_sameValue_equalsWorks() throws Exception {
        // Both vars declared as DOUBLE, same BigDecimal passed to both sides.
        // CEL sees two identical unknown objects and returns true — but this is
        // coincidental: it is NOT doing precise BigDecimal arithmetic.
        CelRuntime.Program eq = compile("a == b");
        BigDecimal val = new BigDecimal("1.12345678901234567890");

        Object result = eq.eval(Map.of("a", val, "b", val));
        assertTrue((Boolean) result);
    }

    @Test
    void bigDecimalPassedAsDouble_comparedWithLiteral_throwsAtRuntime() {
        // var declared as DOUBLE, BigDecimal passed, compared with a double literal.
        // CEL cannot find a matching overload for comparing BigDecimal with double.
        assertThrows(Exception.class, () -> {
            CelRuntime.Program gt = compile("a > 1.0");
            gt.eval(Map.of("a", new BigDecimal("1.5")));
        });
    }

    @Test
    void bigDecimalPassedAsDyn_equalityReturnsFalse() throws Exception {
        // CEL's DYN equality falls back to reference equality for unknown types.
        // Even two BigDecimals with identical values are NOT equal.
        Cel dynCel = CelFactory.standardCelBuilder()
                .addVar("a", SimpleType.DYN)
                .addVar("b", SimpleType.DYN)
                .build();
        CelRuntime.Program eq = dynCel.createProgram(dynCel.compile("a == b").getAst());

        BigDecimal v1 = new BigDecimal("1.12345678901234567890");
        BigDecimal v2 = new BigDecimal("1.12345678901234567890"); // same value, different instance

        // reference equality: v1 != v2 even though v1.equals(v2) is true in Java
        assertFalse((Boolean) eq.eval(Map.of("a", v1, "b", v2)));
    }

    // ---- BigDecimal.doubleValue() before passing to CEL ----

    @Test
    void doubleValue_representableValue_precisionPreserved() throws Exception {
        // 1.5 is exactly representable in IEEE 754 — no precision loss
        CelRuntime.Program gt = compile("a > 1.0");
        Object result = gt.eval(Map.of("a", new BigDecimal("1.5").doubleValue(), "b", 0.0));
        System.out.printf("BigDecimal(\"1.5\").doubleValue() > 1.0  →  %s%n", result);
        assertTrue((Boolean) result);
    }

    @Test
    void doubleValue_highPrecisionValue_losesPrecisionOnConversion() throws Exception {
        // 1.1234567890123456789 has more digits than double can represent
        BigDecimal precise  = new BigDecimal("1.1234567890123456789");
        BigDecimal slightlyDifferent = new BigDecimal("1.1234567890123456000");

        double d1 = precise.doubleValue();
        double d2 = slightlyDifferent.doubleValue();

        System.out.printf("precise.doubleValue()          = %.20f%n", d1);
        System.out.printf("slightlyDifferent.doubleValue() = %.20f%n", d2);
        System.out.printf("equal after conversion: %s%n", d1 == d2);

        // precision is lost during doubleValue() — CEL never sees the difference
        CelRuntime.Program eq = compile("a == b");
        Object result = eq.eval(Map.of("a", d1, "b", d2));
        assertTrue((Boolean) result);
    }

    @Test
    void doubleValue_comparedWithCelLiteral_mustMatch() throws Exception {
        // The CEL expression literal and the Java-side value must both be
        // converted from the same source to guarantee they match
        BigDecimal source = new BigDecimal("3.14");
        double javaValue  = source.doubleValue();

        CelRuntime.Program eq = CelFactory.standardCelBuilder()
                .addVar("a", SimpleType.DOUBLE)
                .build()
                .createProgram(
                        CelFactory.standardCelBuilder()
                                .addVar("a", SimpleType.DOUBLE)
                                .build()
                                .compile("a == 3.14")
                                .getAst()
                );

        Object result = eq.eval(Map.of("a", javaValue));
        System.out.printf("BigDecimal(\"3.14\").doubleValue() == CEL literal 3.14  →  %s%n", result);
        // 3.14 is not exactly representable, but both sides go through the same
        // IEEE 754 rounding, so they end up equal
        assertTrue((Boolean) result);
    }

    // ---- CEL literal precision ----

    @Test
    void celLiteral_doubleInExpression_hasDoublePrecision() throws Exception {
        // A literal like 3.0000000000001 in a CEL expression is stored as a double
        Cel literalCel = CelFactory.standardCelBuilder()
                .addVar("score", SimpleType.DOUBLE)
                .build();

        CelRuntime.Program p = literalCel.createProgram(
                literalCel.compile("score == 3.0000000000001").getAst()
        );

        double value = new BigDecimal("3.0000000000001").doubleValue();
        Object result = p.eval(Map.of("score", value));
        System.out.printf("BigDecimal(\"3.0000000000001\").doubleValue() == CEL literal 3.0000000000001  →  %s%n", result);

        // Both sides are IEEE 754 doubles — they match if the literal is representable
        assertTrue((Boolean) result);
    }
}
