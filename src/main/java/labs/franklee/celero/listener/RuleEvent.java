package labs.franklee.celero.listener;

import labs.franklee.celero.engine.RuleContext;

public class RuleEvent {

    private final String ruleId;
    private final String ruleName;
    private final boolean matched;
    private final RuleContext context;

    public RuleEvent(String ruleId, String ruleName, boolean matched, RuleContext context) {
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

    public boolean isMatched() {
        return matched;
    }

    public RuleContext getContext() {
        return context;
    }
}
