package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.CompareCondition;
import labs.franklee.celero.rules.ConditionNode;

import java.util.Map;

public class CompareConditionFactory extends CommonConditionFactory {
    @Override
    public Condition create(ConditionNode conditionNode) {
        Map<String, Object> properties = conditionNode.getProperties();
        return new CompareCondition(field(properties), value(properties), valueType(properties),
                conditionNode.getSign(), priority(properties));
    }
}
