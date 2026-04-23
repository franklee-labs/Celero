package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.NotInCondition;

import java.util.Map;

public class NotInConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new NotInCondition(field(properties), value(properties), valueType(properties), priority(properties));
    }
}
