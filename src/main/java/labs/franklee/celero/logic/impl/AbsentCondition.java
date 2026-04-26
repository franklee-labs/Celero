package labs.franklee.celero.logic.impl;

import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;

/**
 * Logical inverse of {@link ExistsCondition}
 */
public class AbsentCondition extends Condition {
    private final String field;
    private final String expression;
    private CelRuntime.Program program;

    public AbsentCondition(String field) {
        this.field = field;
        this.expression = "!has(" + field + ")";
    }

    public AbsentCondition(String field, int priority) {
        super(priority, false);
        this.field = field;
        this.expression = "!has(" + field + ")";
    }

    public AbsentCondition(String field, int priority, boolean ignoreAbsence) {
        super(priority, ignoreAbsence);
        this.field = field;
        this.expression = "!has(" + field + ")";
    }

    @Override
    protected String generateName() {
        return this.expression;
    }

    @Override
    public void compile() throws Exception {
        this.program = CelUtils.buildProgram(expression);
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
    public Condition negate() throws Exception {
        Condition condition = new ExistsCondition(this.field, this.getPriority(), this.isIgnoreAbsence());
        condition.build();
        return condition;
    }
}
