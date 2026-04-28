package labs.franklee.celero.logic.impl;

import com.google.re2j.Pattern;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;

/**
 * Logical inverse of {@link RegexCondition}: passes when the field does NOT match the pattern.
 */
public class NegateRegexCondition extends Condition {

    private final RegexCondition origin;
    private final String field;
    private final String regex;

    private CelRuntime.Program program;

    NegateRegexCondition(RegexCondition origin) {
        super(origin.getPriority(), origin.isIgnoreAbsence());
        this.setName("[negated]" + origin.getName());
        this.origin = origin;
        this.field = origin.getField();
        this.regex = origin.getRegex();
    }

    @Override
    protected String generateName() {
        return String.format("!( %s.matches(r'%s') )", this.field, this.regex);
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
        return origin;
    }

    @Override
    public void beforeEvaluate(Context context) {
        context.buildEvalParams(null);
    }

    @Override
    public boolean evaluate(Context context) throws MissingParameterException {
        return this.celEvaluate(this.program, context);
    }

    @Override
    public void compile() throws Exception {
        this.expression = String.format("!( %s.matches(r'%s') )", this.field, this.regex);
        this.program = CelUtils.buildProgram(expression);
    }

    String getRegex() {
        return this.regex;
    }
}
