package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.ExistsCondition;

import java.util.Map;

public class ExistsConditionFactory extends CommonConditionFactory {
    @Override
    public Condition create(Map<String, Object> properties) {
        return new ExistsCondition(field(properties), priority(properties));
    }
}
