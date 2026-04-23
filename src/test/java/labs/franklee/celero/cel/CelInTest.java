package labs.franklee.celero.cel;

import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CelInTest {

    @Test
    public void valueInt_in_DynList_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", 1, "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    public void valueString_in_DynList_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", "celero", "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    public void valueDouble_in_DynList_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", Double.MAX_VALUE, "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    public void valueBool_in_DynList_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", true, "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    public void valueLong_in_DynList_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", Long.MIN_VALUE, "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && b);
    }

    /* test when using SimpleType.DYN for list */
    @Test
    public void valueLong_in_Dyn_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", SimpleType.DYN)
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", Long.MIN_VALUE, "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    public void value_notIn_DynList_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("list", ListType.create(SimpleType.DYN))
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in list").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", "1", "list", List.of(1, "celero", Double.MAX_VALUE, true, Long.MIN_VALUE)));
        assertTrue(result instanceof Boolean b && !b);
    }

    /* tests when using map */

    @Test
    public void value_in_Dyn_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("obj", SimpleType.DYN)
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in obj").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", "k1", "obj", Map.of("k1", "v1", "k2", 2)));
        assertTrue(result instanceof Boolean b && b);
    }

    @Test
    public void value_notIn_Dyn_returnTrue() throws Throwable {
        Cel cel = CelFactory.standardCelBuilder()
                .addVar("val", SimpleType.DYN)
                .addVar("obj", SimpleType.DYN)
                .build();
        CelAbstractSyntaxTree ast = cel.compile("val in obj").getAst();
        CelRuntime.Program program = cel.createProgram(ast);
        Object result = program.eval(Map.of("val", "k3", "obj", Map.of("k1", "v1", "k2", 2)));
        assertTrue(result instanceof Boolean b && !b);
    }

}
