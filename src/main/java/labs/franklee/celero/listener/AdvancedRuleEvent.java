package labs.franklee.celero.listener;

import labs.franklee.celero.engine.AdvancedCeleroEngine;
import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.logic.base.EvalResult;

/**
 * Carries the three-state result of a single rule evaluation, delivered to
 * {@link AdvancedRuleListener#onRuleResult(AdvancedRuleEvent)} by
 * {@link AdvancedCeleroEngine}.
 *
 * <p>The result is one of:
 * <ul>
 *   <li>{@link EvalResult#TRUE}          — at least one path was fully satisfied</li>
 *   <li>{@link EvalResult#FALSE}         — every path had at least one unmatched condition</li>
 *   <li>{@link EvalResult#INDETERMINATE} — no path was false, but at least one condition
 *                                          could not be evaluated due to an absent field</li>
 * </ul>
 *
 * <p>The event is immutable. All fields are set at construction time and reflect
 * the state at the moment the rule finished evaluating.
 */
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
     * Returns the three-state evaluation result of this rule.
     *
     * @return {@link EvalResult#TRUE} if matched, {@link EvalResult#FALSE} if not matched,
     *         or {@link EvalResult#INDETERMINATE} if the outcome could not be determined
     */
    public EvalResult isMatched() {
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
