package labs.franklee.celero.logic.impl;

import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;

/**
 * Logical inverse of {@link CelCondition}
 */
public class NegateCelCondition extends Condition {

    private final CelCondition origin;
    private CelRuntime.Program program;

    NegateCelCondition(CelCondition origin) {
        super(origin.getPriority());
        this.setName("[negated]" + origin.getName());
        this.origin = origin;
        this.expression = "!(" + origin.getExpression() + ")";
        if (null == this.getName() || "".equalsIgnoreCase(this.getName().trim())) {
            this.setName(generateName());
        }
    }

    private String generateName() {
        return this.expression;
    }

    @Override
    public Condition negate() {
        return origin;
    }

    @Override
    public void compile() throws Exception {
        this.program = CelUtils.buildProgram(this.expression);
    }

    @Override
    public void beforeEvaluate(Context context) {
        context.buildEvalParams(null);
    }

    @Override
    public boolean evaluate(Context context) throws MissingParameterException {
        return this.celEvaluate(this.program, context);
    }
}
