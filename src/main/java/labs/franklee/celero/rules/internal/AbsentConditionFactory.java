package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.AbsentCondition;
import labs.franklee.celero.rules.ConditionNode;

import java.util.Map;

public class AbsentConditionFactory extends CommonConditionFactory {
    @Override
    public Condition create(ConditionNode conditionNode) {
        Map<String, Object> properties = conditionNode.getProperties();
        return new AbsentCondition(field(properties), priority(properties));
    }
}
