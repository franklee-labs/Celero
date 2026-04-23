package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.EqualCondition;

import java.util.Map;

public class EqualConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new EqualCondition(field(properties), value(properties), valueType(properties), priority(properties));
    }
}
