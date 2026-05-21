package labs.franklee.celero.listener;

import labs.franklee.celero.engine.AdvancedCeleroEngine;
import labs.franklee.celero.logic.base.EvalResult;

/**
 * Callback invoked by {@link AdvancedCeleroEngine} immediately after each condition
 * is executed during rule evaluation.
 *
 * <p>Unlike {@link ConditionListener}, this variant receives a three-state
 * {@link EvalResult} ({@code TRUE}, {@code FALSE}, or {@code INDETERMINATE}),
 * allowing listeners to distinguish between a condition that failed and one whose
 * required input field was absent from the {@link labs.franklee.celero.engine.RuleContext}.
 *
 * <p>Register instances via
 * {@link AdvancedCeleroEngine#addConditionListener(AdvancedConditionListener)}.
 * Multiple listeners may be registered; they are called in ascending {@link #order()} value.
 *
 * <p>A condition that is served from the result cache does <em>not</em> trigger this
 * listener — it fires only when the condition expression is actually evaluated.
 *
 * <p>This is a {@link FunctionalInterface}; a lambda may be used for simple cases:
 * <pre>{@code
 * engine.addConditionListener(event -> {
 *     if (event.getResult().isIndeterminate()) {
 *         log.warn("Missing field for condition: {}", event.getConditionName());
 *     }
 * });
 * }</pre>
 *
 * <p>Exceptions thrown inside {@code onResult} are silently swallowed by the engine
 * so that a misbehaving listener cannot disrupt rule evaluation.
 */
@FunctionalInterface
public interface AdvancedConditionListener {

    /**
     * Called after a condition has been evaluated.
     *
     * @param event the three-state evaluation result and context; never {@code null}
     */
    void onResult(AdvancedConditionEvent event);

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
