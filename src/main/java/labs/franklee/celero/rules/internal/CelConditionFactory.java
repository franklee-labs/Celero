package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.CelCondition;

import java.util.Map;

public class CelConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new CelCondition(value(properties), priority(properties));
    }
}
