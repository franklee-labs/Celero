package labs.franklee.celero.logic.impl;

import dev.cel.common.CelErrorCode;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelUnknownSet;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.EvalException;
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

//    public AbsentCondition(String field, int priority, boolean ignoreAbsence) {
//        super(priority, ignoreAbsence);
//        this.field = field;
//        this.expression = "!has(" + field + ")";
//    }

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
        return this.celEvaluateIgnoreAbsence(this.program, context);
    }

    private boolean celEvaluateIgnoreAbsence(CelRuntime.Program program, Context context) {
        try {
            Object result = program.eval(context.getEvalParam());
            if (result instanceof CelUnknownSet) {
                return true;
            }
            return result instanceof Boolean b && b;
        } catch (CelEvaluationException e) {
            if (e.getErrorCode() == CelErrorCode.ATTRIBUTE_NOT_FOUND) {
                return true;
            }
            throw new EvalException(e);
        }
    }

    @Override
    public Condition negate() throws Exception {
//        Condition condition = new ExistsCondition(this.field, this.getPriority(), this.isIgnoreAbsence());
        Condition condition = new ExistsCondition(this.field, this.getPriority());
        condition.build();
        return condition;
    }
}
