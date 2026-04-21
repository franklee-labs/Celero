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

public class GreaterThanCondition extends Condition {

    private static final String GT = " > ";

    private final String key;
    private final ValueType valueType;
    private final String value;

    private Set<String> expressionValueVars;
    private CelRuntime.Program program;
    private Map<String, Object> builtinParams;

    public GreaterThanCondition(String key, String value, ValueType valueType) {
        super();
        this.key = key;
        this.value = value;
        this.valueType = valueType;
    }

    public GreaterThanCondition(String key, String value, ValueType valueType, int priority) {
        super(priority);
        this.key = key;
        this.value = value;
        this.valueType = valueType;
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
        } else if (this.valueType == ValueType.Expression) {
            return Validation.VALID;
        }
        return new Validation(false, this.getName() + " unsupported value type [" + this.valueType + "]");
    }

    @Override
    public Condition negate() throws Exception {
        Condition condition = new LessThanOrEqualCondition(this.key, this.value, this.valueType, this.getPriority());
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
            this.expression = this.key + GT + this.value;
            this.buildCelProgram(null);
        } else if (this.valueType == ValueType.Number) {
            String valKey = Constant.BUILTIN_KEY + "NUM_001";
            this.expression = this.key + GT + valKey;
            BigDecimal bd = new BigDecimal(this.value).stripTrailingZeros();
            Map<String, CelType> celType;
            if (bd.scale() <= 0) {
                celType = Map.of(valKey, SimpleType.INT);
                this.builtinParams = Map.of(valKey, bd.longValueExact());
            } else {
                celType = Map.of(valKey, SimpleType.DOUBLE);
                this.builtinParams = Map.of(valKey, bd.doubleValue());
            }
            this.buildCelProgram(celType);
        } else {
            throw new UnsupportedOperationException("GreaterThanCondition only supports Number and Expression");
        }
    }
}
