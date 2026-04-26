package labs.franklee.celero.logic.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.cel.bundle.Cel;
import dev.cel.common.types.CelType;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.InvalidConditionException;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;
import labs.franklee.celero.logic.base.ValueType;

import java.util.*;

/**
 * Logical inverse of {@link IntersectCondition}
 */
public class DisjointCondition extends Condition {

    private final String field1;
    private String valueTypeStr1;
    private ValueType valueType1;
    private final String field2;
    private String valueTypeStr2;
    private ValueType valueType2;
    private Map<String, Object> builtinParams;
    private CelRuntime.Program program;

    public DisjointCondition(String field1, String valueType1, String field2, String valueType2) {
        this.field1 = field1;
        this.field2 = field2;
        this.setValueType(valueType1, valueType2);
    }

    public DisjointCondition(String field1, String valueType1, String field2, String valueType2, int priority) {
        super(priority, false);
        this.field1 = field1;
        this.field2 = field2;
        this.setValueType(valueType1, valueType2);
    }

    public DisjointCondition(String field1, String valueType1, String field2, String valueType2, int priority, boolean ignoreAbsence) {
        super(priority, ignoreAbsence);
        this.field1 = field1;
        this.field2 = field2;
        this.setValueType(valueType1, valueType2);
    }

    private void setValueType(String valueType1, String valueType2) {
        try {
            this.valueTypeStr1 = valueType1;
            this.valueTypeStr2 = valueType2;
            this.valueType1 = ValueType.fromString(valueType1);
            this.valueType2 = ValueType.fromString(valueType2);
        } catch (IllegalArgumentException e) {
            throw new InvalidConditionException("invalid ValueType. support " + Arrays.toString(ValueType.values()) + ", got [" + valueType1 + "," + valueType2 + "]");
        }
    }

    @Override
    protected String generateName() {
        return "list(" + this.field1 + ") and list(" + this.field2 + ") are disjoint";
    }

    @Override
    public Validation validate() {
        if (!ValueType.List.equals(this.valueType1) && !ValueType.Expression.equals(this.valueType1)) {
            return new Validation(false, "invalid ValueType. support [List, Expression], got [" + valueType1 + "," + valueType2 + "]");
        }
        if (!ValueType.List.equals(this.valueType2) && !ValueType.Expression.equals(this.valueType2)) {
            return new Validation(false, "invalid ValueType. support [List, Expression], got [" + valueType1 + "," + valueType2 + "]");
        }
        return Validation.VALID;
    }

    @Override
    public void compile() throws Exception {
        Map<String, Object> builtinMap = new HashMap<>();
        String left = this.field1;
        Map<String, CelType> builtinParamTypes = new HashMap<>();
        if (ValueType.List.equals(this.valueType1)) {
            List<Object> list1 = mapper.readValue(this.field1, new TypeReference<>() {});
            String k = Constant.BUILTIN_KEY + "list_001";
            List<Object> normalized = list1.stream().map(CelUtils::toCelValue).toList();
            builtinMap.put(k, normalized);
            left = k;
            builtinParamTypes.put(k, ListType.create(SimpleType.DYN));
        }
        String right = this.field2;
        if (ValueType.List.equals(this.valueType2)) {
            List<Object> list2 = mapper.readValue(this.field2, new TypeReference<>() {});
            String k = Constant.BUILTIN_KEY + "list_002";
            List<Object> normalized = list2.stream().map(CelUtils::toCelValue).toList();
            builtinMap.put(k, normalized);
            right = k;
            builtinParamTypes.put(k, ListType.create(SimpleType.DYN));
        }
        if (!builtinMap.isEmpty()) {
            this.builtinParams = builtinMap;
        }
        this.expression = String.format("!( %s.exists(x, %s.exists(y, y == x)) )", left, right);
        Set<String> varNames = CelUtils.extractTopVarNames(this.expression);
        Cel cel = CelUtils.buildCelWithVars(varNames, builtinParamTypes);
        this.program = CelUtils.buildProgram(this.expression, cel);
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
    public Condition negate() throws Exception {
        Condition condition = new IntersectCondition(this.field1, this.valueTypeStr1, this.field2, this.valueTypeStr2, this.getPriority(), this.isIgnoreAbsence());
        condition.build();
        return condition;
    }
}
