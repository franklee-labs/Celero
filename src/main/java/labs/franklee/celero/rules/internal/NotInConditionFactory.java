package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.NotInCondition;

import java.util.Map;

public class NotInConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(Map<String, Object> properties) {
        return new NotInCondition(key(properties), parseList(properties), priority(properties));
    }
}
