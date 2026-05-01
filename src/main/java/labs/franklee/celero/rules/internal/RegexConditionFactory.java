package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.RegexCondition;
import labs.franklee.celero.rules.ConditionNode;

import java.util.Map;

public class RegexConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(ConditionNode conditionNode) {
        Map<String, Object> properties = conditionNode.getProperties();
        return new RegexCondition(field(properties), value(properties), priority(properties));
    }
}
