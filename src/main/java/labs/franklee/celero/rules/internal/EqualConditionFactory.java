package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.EqualCondition;
import labs.franklee.celero.rules.ConditionNode;

import java.util.Map;

public class EqualConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(ConditionNode conditionNode) {
        Map<String, Object> properties = conditionNode.getProperties();
        return new EqualCondition(field(properties), value(properties), valueType(properties), priority(properties));
    }
}
