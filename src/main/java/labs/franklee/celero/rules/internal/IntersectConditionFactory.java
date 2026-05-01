package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.impl.IntersectCondition;
import labs.franklee.celero.rules.ConditionNode;

import java.util.Map;

public class IntersectConditionFactory extends CommonConditionFactory {

    @Override
    public Condition create(ConditionNode conditionNode) {
        Map<String, Object> properties = conditionNode.getProperties();
        String field1 = getValueAs(properties, "field1", String.class);
        String valueTypeStr1 = getValueAs(properties, "valueType1", String.class);
        String field2 = getValueAs(properties, "field2", String.class);
        String valueTypeStr2 = getValueAs(properties, "valueType2", String.class);
        return new IntersectCondition(field1, valueTypeStr1, field2, valueTypeStr2, priority(properties));
    }
}
