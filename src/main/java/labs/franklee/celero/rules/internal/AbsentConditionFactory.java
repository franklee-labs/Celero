package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.AbsentCondition;

import java.util.Map;

public class AbsentConditionFactory extends CommonConditionFactory {
    @Override
    public Condition create(Map<String, Object> properties) {
        return new AbsentCondition(field(properties), priority(properties));
    }
}
