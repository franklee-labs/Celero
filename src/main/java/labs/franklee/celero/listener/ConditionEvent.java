package labs.franklee.celero.listener;

import labs.franklee.celero.engine.DefaultCeleroEngine;
import labs.franklee.celero.engine.RuleContext;

/**
 * Carries the result of a single condition evaluation, delivered to
 * {@link ConditionListener#onResult(ConditionEvent)} by {@link DefaultCeleroEngine}.
 *
 * <p>The event is immutable. All fields are set at construction time and reflect
 * the state at the moment the condition was evaluated.
 */
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

    /**
     * Returns the id of the rule that owns this condition.
     *
     * @return rule id; never {@code null}
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * Returns the name of the rule that owns this condition.
     *
     * @return rule name; may be {@code null} if not set on the rule
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Returns the id of the condition that was evaluated.
     *
     * @return condition id; may be {@code null} for internally generated nodes
     */
    public String getConditionId() {
        return conditionId;
    }

    /**
     * Returns the name of the condition that was evaluated.
     *
     * @return condition name; may be {@code null} if not set
     */
    public String getConditionName() {
        return conditionName;
    }

    /**
     * Returns whether the condition evaluated to {@code true}.
     *
     * @return {@code true} if the condition matched; {@code false} otherwise
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
