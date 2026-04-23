package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.GreaterThanOrEqualCondition;

import java.util.Map;

public class GreaterThanOrEqualConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new GreaterThanOrEqualCondition(field(properties), value(properties), valueType(properties), priority(properties));
    }
}
