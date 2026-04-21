package labs.franklee.celero.logic.impl;

import dev.cel.bundle.Cel;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelRuntime;
import labs.franklee.celero.context.Context;
import labs.franklee.celero.exceptions.MissingParameterException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates whether a field value is NOT contained in a given list.
 * Negation of {@link InCondition} — see its documentation for value format rules.
 */
public class NotInCondition extends Condition {

    private static final String LIST_KEY = Constant.BUILTIN_KEY + "LIST_001";

    private static final String IN = " in ";

    private final String key;
    private final List<Object> values;

    private Set<String> expressionValueVars;
    private CelRuntime.Program program;
    private Map<String, Object> builtinParams;

    public NotInCondition(String key, List<Object> values) {
        super();
        this.key = key;
        this.values = values;
    }

    public NotInCondition(String key, List<Object> values, int priority) {
        super(priority);
        this.key = key;
        this.values = values;
    }

    @Override
    public Condition negate() throws Exception {
        Condition condition = new InCondition(this.key, this.values, this.getPriority());
        condition.build();
        return condition;
    }

    @Override
    public Validation validate() {
        return this.values != null && !this.values.isEmpty() ?
                Validation.VALID :
                new Validation(false, this.getName() + " values must not be empty");
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
        this.expression = "!(" + this.key + IN + LIST_KEY + ")";
        List<Object> normalized = this.values.stream().map(CelUtils::toCelValue).toList();
        this.builtinParams = Map.of(LIST_KEY, normalized);
        this.expressionValueVars = CelUtils.extractTopVarNames(this.expression);
        Cel cel = CelUtils.buildCelWithVars(
                this.expressionValueVars,
                Map.of(LIST_KEY, ListType.create(SimpleType.DYN))
        );
        this.program = CelUtils.buildProgram(this.expression, cel);
    }
}
