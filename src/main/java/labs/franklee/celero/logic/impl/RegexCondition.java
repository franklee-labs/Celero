package labs.franklee.celero.logic.impl;

import com.google.re2j.Pattern;
import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;

import java.util.Set;

/**
 * Checks whether a String field fully matches a regular expression.
 *
 * <p>Uses RE2J ({@link com.google.re2j.Pattern}) — the same engine as CEL — so behaviour is
 * consistent regardless of which layer evaluates the expression. The pattern is compiled once in
 * {@link #compile()} and captured in a custom CEL function, so no regex compilation happens
 * during rule evaluation.
 *
 * <p>Usage:
 * <pre>
 *   new RegexCondition("email", "[\\w.+-]+@[\\w-]+\\.[\\w.]+")
 *   new RegexCondition("phone", "1[3-9]\\d{9}")
 * </pre>
 *
 * <p>Extension guide – implementing a Condition with a CEL custom function:
 * <ol>
 *   <li>{@link #validate()} – verify the configuration before any compilation work.
 *   <li>{@link #compile()} – pre-compile resources and register them as a custom CEL function via
 *       {@link CelFunctionDecl} + {@link CelFunctionBinding}. The lambda captures the compiled
 *       resource, so evaluation never triggers recompilation.
 *   <li>{@link #beforeEvaluate(Context)} – merge builtinParams into the eval map (null when none needed).
 *   <li>{@link #evaluate(Context)} – run the pre-compiled program; identical pattern to all other conditions.
 *   <li>{@link #negate()} – return the logical inverse via {@code condition.build()}.
 * </ol>
 */
public class RegexCondition extends Condition {

    private static final String FUNC_NAME = "regex_matches";
    private static final String OVERLOAD_ID = "regex_matches_string";

    private final String field;
    private final String regex;

    private CelRuntime.Program program;

    public RegexCondition(String field, String regex) {
        super();
        this.field = field;
        this.regex = regex;
        if (null == this.getName() || "".equalsIgnoreCase(this.getName().trim())) {
            this.setName(generateName());
        }
    }

    public RegexCondition(String field, String regex, int priority) {
        super(priority);
        this.field = field;
        this.regex = regex;
        if (null == this.getName() || "".equalsIgnoreCase(this.getName().trim())) {
            this.setName(generateName());
        }
    }

    @Override
    protected String generateName() {
        return FUNC_NAME + "(" + this.field + ")";
    }

    @Override
    public Validation validate() {
        try {
            Pattern.compile(this.regex);
            return Validation.VALID;
        } catch (Throwable e) {
            return new Validation(false, this.getName() + " " + e.getMessage());
        }
    }

    @Override
    public Condition negate() throws Exception {
        Condition condition = new NegateRegexCondition(this);
        condition.build();
        return condition;
    }

    @Override
    public void beforeEvaluate(Context context) {
        // no builtin params to inject; still required to initialise evalParams
        context.buildEvalParams(null);
    }

    @Override
    public boolean evaluate(Context context) throws MissingParameterException {
        return this.celEvaluate(this.program, context);
    }

    @Override
    public void compile() throws Exception {
        Pattern pattern = Pattern.compile(this.regex);

        this.expression = FUNC_NAME + "(" + this.field + ")";
        Set<String> varNames = CelUtils.extractTopVarNames(expression);

        var builder = CelFactory.standardCelBuilder()
                .addFunctionDeclarations(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNC_NAME,
                                CelOverloadDecl.newGlobalOverload(OVERLOAD_ID, SimpleType.BOOL, SimpleType.DYN)))
                .addFunctionBindings(
                        CelFunctionBinding.from(OVERLOAD_ID, Object.class,
                                val -> val instanceof String s ? pattern.matches(s) : Boolean.FALSE));
        varNames.forEach(v -> builder.addVar(v, SimpleType.DYN));

        Cel cel = builder.build();
        CelAbstractSyntaxTree ast = cel.compile(expression).getAst();
        this.program = cel.createProgram(ast);
    }

    String getField() {
        return this.field;
    }

    String getRegex() {
        return this.regex;
    }
}
