package labs.franklee.celero.logic.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cel.bundle.Cel;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.InvalidConditionException;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;
import labs.franklee.celero.logic.base.ValueType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates whether a field value is NOT contained in a given list.
 * Negation of {@link InCondition} — see its documentation for value format rules.
 */
public class NotInCondition extends Condition {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String LIST_KEY = Constant.BUILTIN_KEY + "LIST_001";

    private static final String IN = " in ";

    private final String field;
    private final String value;
    private List<Object> listValues;
    private final ValueType valueType;

    private Set<String> expressionValueVars;
    private CelRuntime.Program program;
    private Map<String, Object> builtinParams;

    public NotInCondition(String field, String value, ValueType valueType) {
        super();
        this.field = field;
        this.value = value;
        this.valueType = valueType;
        if (null == this.value) {
            throw new InvalidConditionException("value must be a JSON array. got null");
        }
        if (ValueType.List.equals(this.valueType)) {
            try {
                this.listValues = mapper.readValue(this.value, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new InvalidConditionException("value must be a JSON array. value=[" + this.value + "]");
            }
        }
        if (null == this.getName() || "".equalsIgnoreCase(this.getName().trim())) {
            this.setName(generateName());
        }
    }

    public NotInCondition(String field, String value, ValueType valueType, int priority) {
        super(priority);
        this.field = field;
        this.value = value;
        this.valueType = valueType;
        if (null == this.value) {
            throw new InvalidConditionException("value must be a JSON array. got null");
        }
        if (ValueType.List.equals(this.valueType)) {
            try {
                this.listValues = mapper.readValue(this.value, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new InvalidConditionException("value must be a JSON array. value=[" + this.value + "]");
            }
        }
        if (null == this.getName() || "".equalsIgnoreCase(this.getName().trim())) {
            this.setName(generateName());
        }
    }

    @Override
    protected String generateName() {
        return String.format("!( %s %s %s )", this.field, IN, this.value);
    }

    @Override
    public Condition negate() throws Exception {
        Condition condition = new InCondition(this.field, this.value, this.valueType, this.getPriority());
        condition.build();
        return condition;
    }

    @Override
    public Validation validate() {
        if (ValueType.List.equals(this.valueType)) {
            if (this.listValues.isEmpty()) {
                return new Validation(false, " value is null or empty");
            }
            return Validation.VALID;
        } else if (ValueType.Expression.equals(this.valueType)) {
            return super.validate();
        } else {
            return new Validation(false, " unsupported ValueType, support[List, Expression]");
        }
    }

    @Override
    public void beforeEvaluate(Context context) {
        context.buildEvalParams(this.builtinParams);
    }

    @Override
    public boolean evaluate(Context context) throws MissingParameterException {
        return this.celEvaluate(this.program, context);
    }

    @Override
    public void compile() throws Exception {
        if (ValueType.List.equals(this.valueType)) {
            this.expression = "!(" + this.field + IN + LIST_KEY + ")";
            List<Object> normalized = this.listValues.stream().map(CelUtils::toCelValue).toList();
            this.builtinParams = Map.of(LIST_KEY, normalized);
            this.expressionValueVars = CelUtils.extractTopVarNames(this.expression);
            Cel cel = CelUtils.buildCelWithVars(
                    this.expressionValueVars,
                    Map.of(LIST_KEY, ListType.create(SimpleType.DYN))
            );
            this.program = CelUtils.buildProgram(this.expression, cel);
        } else {
            this.expression = "!(" + this.field + IN + this.value + ")";
            this.program = CelUtils.buildProgram(this.expression);
        }
    }
}
