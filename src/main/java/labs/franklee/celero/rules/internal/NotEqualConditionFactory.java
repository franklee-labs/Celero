package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.NotEqualCondition;

import java.util.Map;

public class NotEqualConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new NotEqualCondition(key(properties), value(properties), valueType(properties), priority(properties));
    }
}
