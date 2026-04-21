package labs.franklee.celero.listener;

import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.EvalResult;

public class AdvancedRuleEvent {

    private final String ruleId;
    private final String ruleName;
    private final EvalResult matched;
    private final RuleContext context;

    public AdvancedRuleEvent(String ruleId, String ruleName, EvalResult matched, RuleContext context) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.matched = matched;
        this.context = context;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public EvalResult isMatched() {
        return matched;
    }

    public RuleContext getContext() {
        return context;
    }
}
