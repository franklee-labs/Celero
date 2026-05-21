package labs.franklee.celero.listener;

import labs.franklee.celero.engine.AdvancedCeleroEngine;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.EvalResult;

/**
 * Carries the three-state result of a single condition evaluation, delivered to
 * {@link AdvancedConditionListener#onResult(AdvancedConditionEvent)} by
 * {@link AdvancedCeleroEngine}.
 *
 * <p>The result is one of:
 * <ul>
 *   <li>{@link EvalResult#TRUE}          — condition matched</li>
 *   <li>{@link EvalResult#FALSE}         — condition did not match</li>
 *   <li>{@link EvalResult#INDETERMINATE} — a required input field was absent</li>
 * </ul>
 *
 * <p>The event is immutable. All fields are set at construction time and reflect
 * the state at the moment the condition was evaluated.
 */
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
     * Returns the three-state evaluation result of this condition.
     *
     * @return {@link EvalResult#TRUE} if matched, {@link EvalResult#FALSE} if not matched,
     *         or {@link EvalResult#INDETERMINATE} if a required field was absent
     */
    public EvalResult getResult() {
        return result;
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
