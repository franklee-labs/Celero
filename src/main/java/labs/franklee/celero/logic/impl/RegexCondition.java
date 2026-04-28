package labs.franklee.celero.logic.impl;

import com.google.re2j.Pattern;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;

/**
 * Checks whether a String field fully matches a regular expression.
 *
 * <p>Uses RE2J ({@link com.google.re2j.Pattern}) — the same engine as CEL — to validate regexp. so behaviour is
 * consistent regardless of which layer evaluates the expression.
 * </ol>
 */
public class RegexCondition extends Condition {

    private final String field;
    private final String regex;

    private CelRuntime.Program program;

    public RegexCondition(String field, String regex) {
        super();
        this.field = field;
        this.regex = regex;
    }

    public RegexCondition(String field, String regex, int priority) {
        super(priority, false);
        this.field = field;
        this.regex = regex;
    }

    public RegexCondition(String field, String regex, int priority, boolean ignoreAbsence) {
        super(priority, ignoreAbsence);
        this.field = field;
        this.regex = regex;
    }

    @Override
    protected String generateName() {
        return String.format("%s.matches(r'%s')", this.field, this.regex);
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
        this.expression = String.format("%s.matches(r'%s')", this.field, this.regex);
        this.program = CelUtils.buildProgram(expression);

    }

    String getField() {
        return this.field;
    }

    String getRegex() {
        return this.regex;
    }
}
