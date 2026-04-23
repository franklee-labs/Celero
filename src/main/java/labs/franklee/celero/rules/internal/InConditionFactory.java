package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.InCondition;

import java.util.Map;

public class InConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new InCondition(field(properties), value(properties), valueType(properties), priority(properties));
    }
}
