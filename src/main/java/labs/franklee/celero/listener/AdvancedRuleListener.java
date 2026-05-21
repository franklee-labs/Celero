package labs.franklee.celero.listener;

import labs.franklee.celero.engine.AdvancedCeleroEngine;
import labs.franklee.celero.logic.base.EvalResult;

/**
 * Callback invoked by {@link AdvancedCeleroEngine} immediately after each rule
 * evaluation completes.
 *
 * <p>Unlike {@link RuleListener}, this variant receives a three-state
 * {@link EvalResult} ({@code TRUE}, {@code FALSE}, or {@code INDETERMINATE}),
 * allowing listeners to react specifically to cases where the outcome could not
 * be determined due to absent input fields.
 *
 * <p>Register instances via
 * {@link AdvancedCeleroEngine#addRuleListener(AdvancedRuleListener)}.
 * Multiple listeners may be registered; they are called in ascending {@link #order()} value.
 *
 * <p>This listener is only fired when rules are evaluated via
 * {@link AdvancedCeleroEngine#evaluate(java.util.List, labs.franklee.celero.engine.RuleContext)}.
 * Calling the single-rule overload
 * {@link AdvancedCeleroEngine#evaluate(labs.franklee.celero.engine.CeleroRule, labs.franklee.celero.engine.RuleContext)}
 * does not trigger rule listeners.
 *
 * <p>This is a {@link FunctionalInterface}; a lambda may be used for simple cases:
 * <pre>{@code
 * engine.addRuleListener(event -> {
 *     if (event.isMatched().isIndeterminate()) {
 *         log.warn("Rule {} could not be decided — missing fields", event.getRuleId());
 *     }
 * });
 * }</pre>
 *
 * <p>Exceptions thrown inside {@code onRuleResult} are silently swallowed by the engine
 * so that a misbehaving listener cannot disrupt evaluation of subsequent rules.
 */
@FunctionalInterface
public interface AdvancedRuleListener {

    /**
     * Called after a rule has been fully evaluated.
     *
     * @param event the three-state evaluation result and context; never {@code null}
     */
    void onRuleResult(AdvancedRuleEvent event);

    /**
     * Returns the invocation priority of this listener relative to others registered
     * on the same engine. Listeners are called in <em>ascending</em> order — a lower
     * value fires first.
     *
     * <p>Defaults to {@code 0} when not overridden.
     *
     * @return the priority value; lower means earlier
     */
    default int order() {
        return 0;
    }
}
