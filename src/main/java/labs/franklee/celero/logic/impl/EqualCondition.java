package labs.franklee.celero.logic.impl;

import dev.cel.bundle.Cel;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;
import labs.franklee.celero.logic.base.ValueType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;


public class EqualCondition extends Condition {

    private final static String EQUAL = " == ";

    private final String field;

    private final ValueType valueType;

    private final String value;

    private Set<String> expressionValueVars;

    private CelRuntime.Program program;

    private Map<String, Object> builtinParams;

    public EqualCondition(String field, String value, ValueType valueType) {
        super();
        this.field = field;
        this.value = value;
        this.valueType = valueType;
    }

    public EqualCondition(String field, String value, ValueType valueType, int priority) {
        super(priority, false);
        this.field = field;
        this.value = value;
        this.valueType = valueType;
    }

    public EqualCondition(String field, String value, ValueType valueType, int priority, boolean ignoreAbsence) {
        super(priority, ignoreAbsence);
        this.field = field;
        this.value = value;
        this.valueType = valueType;
    }

    @Override
    protected String generateName() {
        if (ValueType.String.equals(this.valueType)) {
            return String.format("%s %s str(%s)", this.field, EQUAL, this.value);
        }
        if (ValueType.Number.equals(this.valueType)) {
            return String.format("%s %s num(%s)", this.field, EQUAL, this.value);
        }
        if (ValueType.Expression.equals(this.valueType)) {
            return String.format("%s %s %s", this.field, EQUAL, this.value);
        }
        if (ValueType.Boolean.equals(this.valueType)) {
            return String.format("%s %s bool(%s)", this.field, EQUAL, this.value);
        }
        return String.format("%s %s %s", this.field, EQUAL, this.value);
    }

    @Override
    public Validation validate() {
        if (this.valueType == ValueType.Number) {
            try {
                BigDecimal bd = new BigDecimal(this.value);
                return Validation.VALID;
            } catch (Throwable e) {
                return new Validation(false, this.getName() + " " + e.getMessage());
            }
        }
        return super.validate();
    }

    @Override
    public Condition negate() throws Exception {
        Condition condition = new NotEqualCondition(this.field, this.value, this.valueType, this.getPriority(), this.isIgnoreAbsence());
        condition.build();
        return condition;
    }

    @Override
    public void beforeEvaluate(Context context) {
        context.buildEvalParams(this.builtinParams);
    }

    @Override
    public boolean evaluate(Context context) throws MissingParameterException {
        return this.celEvaluate(this.program, context);
    }

    private void buildCelProgram(Map<String, CelType> celTypes) throws Exception {
        this.expressionValueVars = CelUtils.extractTopVarNames(this.expression);
        Cel cel = CelUtils.buildCelWithVars(this.expressionValueVars, celTypes);
        this.program = CelUtils.buildProgram(this.expression, cel);
    }

    @Override
    public void compile() throws Exception {
        if (this.valueType == ValueType.Expression) {
            this.expression = this.field + EQUAL + this.value;
            this.buildCelProgram(null);
        } else if (this.valueType == ValueType.String) {
            String valKey = Constant.BUILTIN_KEY + "STR_001";
            this.expression = this.field + EQUAL + valKey;
            this.buildCelProgram(Map.of(valKey, SimpleType.STRING));
            this.builtinParams = Map.of(valKey, this.value);
        } else if (this.valueType == ValueType.Boolean) {
            boolean b = Boolean.parseBoolean(this.value);
            if (b) {
                this.expression = this.field + EQUAL + "true";
            } else {
                this.expression = this.field + EQUAL + "false";
            }
            this.buildCelProgram(null);
        } else if (this.valueType == ValueType.Number) {
            String valKey = Constant.BUILTIN_KEY + "NUM_001";
            this.expression = this.field + EQUAL + valKey;
            BigDecimal bd = new BigDecimal(this.value);
            BigDecimal striped = bd.stripTrailingZeros();
            Map<String, CelType> celType;
            if (striped.scale() <= 0) {
                // whole number → long, exact
                celType = Map.of(valKey, SimpleType.INT);
                this.builtinParams = Map.of(valKey, striped.longValueExact());
            } else {
                // decimal → double, IEEE 754 precision
                celType = Map.of(valKey, SimpleType.DOUBLE);
                this.builtinParams = Map.of(valKey, striped.doubleValue());
            }
            this.buildCelProgram(celType);
        } else {
            throw new UnsupportedOperationException("Unsupported ValueType");
        }
    }

}
