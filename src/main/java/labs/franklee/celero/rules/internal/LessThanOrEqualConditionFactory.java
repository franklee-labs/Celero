package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.LessThanOrEqualCondition;

import java.util.Map;

public class LessThanOrEqualConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new LessThanOrEqualCondition(field(properties), value(properties), valueType(properties), priority(properties), ignoreAbsence(properties));
    }
}
