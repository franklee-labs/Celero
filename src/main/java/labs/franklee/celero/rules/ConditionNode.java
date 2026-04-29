package labs.franklee.celero.rules;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.Node;
import labs.franklee.celero.rules.internal.ConditionFactory;

import java.util.HashMap;
import java.util.Map;

public class ConditionNode extends RuleNode {

    private boolean cacheable;

    private final Map<String, Object> properties = new HashMap<>();

    public boolean isCacheable() {
        return cacheable;
    }

    public ConditionNode setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public ConditionNode setProperties(Map<String, Object> props) {
        this.properties.putAll(props);
        return this;
    }

    @Override
    public Node transform(RuleMeta meta) throws InvalidRuleNodeException {
        if (null == this.getId()) {
            throw new InvalidRuleNodeException("condition node id must not be null");
        }
        String ruleId = meta.getId();
        ConditionFactory factory = ConditionFactoryRegistry.getConditionFactory(ruleId, this.getSign());
        if (null == factory) {
            throw new InvalidRuleNodeException("can not find factory for sign [" + this.getSign() + "]");
        }
        Condition condition = factory.create(this.properties);
        try {
            condition.setId(this.getId());
            condition.setName(this.getName());
            condition.setRuleId(meta.getId()).setRuleName(meta.getName());
            condition.setCacheable(this.isCacheable());
            condition.build();
            return condition;
        } catch (InvalidRuleNodeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRuleNodeException(e);
        }
    }
}
