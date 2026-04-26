package labs.franklee.celero.logic.impl;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;

public class ExistsCondition extends Condition {

    private final String field;

    public ExistsCondition(String field) {
        this.field = field;
    }

    public ExistsCondition(int priority, boolean ignoreAbsence, String field) {
        super(priority, ignoreAbsence);
        this.field = field;
    }

    @Override
    protected String generateName() {
        return "has(" + this.field + ")";
    }

    @Override
    public void compile() throws Exception {

    }

    @Override
    public void beforeEvaluate(Context context) {

    }

    @Override
    public boolean evaluate(Context context) throws MissingParameterException {
        return false;
    }

    @Override
    public Condition negate() throws Exception {
        return null;
    }
}
