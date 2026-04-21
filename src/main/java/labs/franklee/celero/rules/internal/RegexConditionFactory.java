package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.RegexCondition;

import java.util.Map;

public class RegexConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new RegexCondition(key(properties), value(properties), priority(properties));
    }
}
