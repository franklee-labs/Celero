package labs.franklee.celero.listener;

import labs.franklee.celero.engine.RuleContext;

public class ConditionEvent {

    private final String ruleId;
    private final String ruleName;
    private final String conditionId;
    private final String conditionName;
    private final boolean matched;
    private final RuleContext context;

    public ConditionEvent(String ruleId, String ruleName, String conditionId, String conditionName, boolean matched, RuleContext context) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.conditionId = conditionId;
        this.conditionName = conditionName;
        this.matched = matched;
        this.context = context;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getConditionId() {
        return conditionId;
    }

    public String getConditionName() {
        return conditionName;
    }

    public boolean isMatched() {
        return matched;
    }

    public RuleContext getContext() {
        return context;
    }
}
