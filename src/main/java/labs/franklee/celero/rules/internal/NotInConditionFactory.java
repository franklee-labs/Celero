package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.NotInCondition;
import labs.franklee.celero.rules.ConditionNode;

import java.util.Map;

public class NotInConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(ConditionNode conditionNode) {
        Map<String, Object> properties = conditionNode.getProperties();
        return new NotInCondition(field(properties), value(properties), valueType(properties), priority(properties));
    }
}
