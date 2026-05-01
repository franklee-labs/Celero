package labs.franklee.celero.logic.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cel.common.CelErrorCode;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelUnknownSet;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.EvalException;
import labs.franklee.celero.exceptions.InvalidConditionException;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.impl.AND;
import labs.franklee.celero.logic.path.Path;

import java.util.UUID;

public abstract class Condition extends Node implements Negatable<Condition> {

    protected static final ObjectMapper mapper = new ObjectMapper();

    private final String internalUniqueId;

    private boolean cacheable;

    // if set true, return false when param is absent
    private boolean ignoreAbsence;

    // greater value has lower priority
    private final int priority;

    protected String expression = "";

    public Condition() {
        this.type = NodeType.Condition;
        this.priority = Priority.DEFAULT;
        this.ignoreAbsence = false;
        this.internalUniqueId = UUID.randomUUID().toString();
    }

    public Condition(int priority, boolean ignoreAbsence) {
        this.type = NodeType.Condition;
        this.priority = priority;
        this.ignoreAbsence = ignoreAbsence;
        this.internalUniqueId = UUID.randomUUID().toString();
    }

    protected abstract String generateName();

    @Override
    public void setName(String name) {
        super.setName(name);
        if (null == this.getName() || "".equalsIgnoreCase(this.getName().trim())) {
            this.name = generateName();
        }
    }

    public String getInternalUniqueId() {
        return internalUniqueId;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    public boolean isCacheable() {
        return this.cacheable;
    }

    public boolean isIgnoreAbsence() {
        return ignoreAbsence;
    }

    public void setIgnoreAbsence(boolean ignoreAbsence) {
        this.ignoreAbsence = ignoreAbsence;
    }

    public final int getPriority() {
        return this.priority;
    }

    public Validation validate() {
        return Validation.VALID;
    }

    public abstract void compile() throws Exception;

    public String getExpression() {
        return this.expression;
    }

    public final void build() throws Exception {
        if (!this.validate().isValid()) {
            throw new InvalidConditionException("invalid condition id=[" + getId() + "]" + ", name=[" + getName() + "]");
        }
        this.compile();
    }

    @Override
    public final Relation resolve() {
        return new AND(new Path().addCondition(this));
    }

    public final EvalResult execute(Context context) {
        this.beforeEvaluate(context);
        EvalResult result = null;
        try {
            result = this.evaluate(context) ? EvalResult.TRUE : EvalResult.FALSE;
        } catch (MissingParameterException e) {
            result = !this.isIgnoreAbsence() && context.isEnableMissing() ? EvalResult.INDETERMINATE : EvalResult.FALSE;
        } finally {
            if (context.isEnableConditionResultCache() && this.isCacheable()) {
                context.setConditionEvalResult(this.getInternalUniqueId(), result);
            }
        }
        return result;
    }

    public abstract void beforeEvaluate(Context context);

    public abstract boolean evaluate(Context context) throws MissingParameterException;

    protected final boolean celEvaluate(CelRuntime.Program program, Context context) throws MissingParameterException {
        try {
            Object result = program.eval(context.getEvalParam());
            if (result instanceof CelUnknownSet) {
                throw new MissingParameterException("unknown field value in expression: " + this.getExpression());
            }
            return result instanceof Boolean b && b;
        } catch (CelEvaluationException e) {
            if (e.getErrorCode() == CelErrorCode.ATTRIBUTE_NOT_FOUND) {
                throw new MissingParameterException("missing field in expression: " + this.getExpression(), e);
            }
            throw new EvalException(e);
        }
    }
}
