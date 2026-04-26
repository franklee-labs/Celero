package labs.franklee.celero.logic.impl;

import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;

public class CelCondition extends Condition {

    private CelRuntime.Program program;

    public CelCondition(String expression) {
        super();
        this.expression = expression;
    }

    public CelCondition(String expression, int priority, boolean ignoreAbsence) {
        super(priority, ignoreAbsence);
        this.expression = expression;
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
    public Condition negate() throws Exception {
        Condition condition = new NegateCelCondition(this);
        condition.build();
        return condition;
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
