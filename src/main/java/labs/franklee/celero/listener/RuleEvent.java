package labs.franklee.celero.listener;

import labs.franklee.celero.engine.DefaultCeleroEngine;
import labs.franklee.celero.engine.RuleContext;

/**
 * Carries the result of a single rule evaluation, delivered to
 * {@link RuleListener#onRuleResult(RuleEvent)} by {@link DefaultCeleroEngine}.
 *
 * <p>The event is immutable. All fields are set at construction time and reflect
 * the state at the moment the rule finished evaluating.
 */
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

    /**
     * Returns the id of the rule that was evaluated.
     *
     * @return rule id; never {@code null}
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * Returns the name of the rule that was evaluated.
     *
     * @return rule name; may be {@code null} if not set on the rule
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Returns whether the rule evaluation produced a match.
     *
     * @return {@code true} if at least one path was fully satisfied; {@code false} otherwise
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * Returns the {@link RuleContext} associated with this evaluation.
     *
     * <p>Listeners may call {@link RuleContext#getAttribute} and
     * {@link RuleContext#setAttribute} to share state across listener invocations
     * within the same evaluation call.
     *
     * @return the current rule context; never {@code null}
     */
    public RuleContext getContext() {
        return context;
    }
}
