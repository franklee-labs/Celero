package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.GreaterThanCondition;

import java.util.Map;

public class GreaterThanConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new GreaterThanCondition(key(properties), value(properties), valueType(properties), priority(properties));
    }
}
