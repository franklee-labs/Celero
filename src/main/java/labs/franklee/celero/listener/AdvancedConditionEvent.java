package labs.franklee.celero.listener;

import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.EvalResult;

public class AdvancedConditionEvent {

    private final String ruleId;
    private final String ruleName;
    private final String conditionId;
    private final String conditionName;
    private final EvalResult result;
    private final RuleContext context;

    public AdvancedConditionEvent(String ruleId, String ruleName, String conditionId, String conditionName, EvalResult result, RuleContext context) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.conditionId = conditionId;
        this.conditionName = conditionName;
        this.result = result;
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

    public EvalResult getResult() {
        return result;
    }

    public RuleContext getContext() {
        return context;
    }
}
