package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.LessThanCondition;

import java.util.Map;

public class LessThanConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new LessThanCondition(key(properties), value(properties), valueType(properties), priority(properties));
    }
}
