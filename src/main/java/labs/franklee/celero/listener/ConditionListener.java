package labs.franklee.celero.listener;

import labs.franklee.celero.engine.DefaultCeleroEngine;

/**
 * Callback invoked by {@link DefaultCeleroEngine} immediately after each condition
 * is executed during rule evaluation.
 *
 * <p>Register instances via
 * {@link DefaultCeleroEngine#addConditionListener(ConditionListener)}.
 * Multiple listeners may be registered; they are called in ascending {@link #order()} value.
 *
 * <p>A condition that is served from the result cache does <em>not</em> trigger this
 * listener — it fires only when the condition expression is actually evaluated.
 *
 * <p>This is a {@link FunctionalInterface}; a lambda may be used for simple cases:
 * <pre>{@code
 * engine.addConditionListener(event -> {
 *     if (!event.isMatched()) {
 *         log.warn("Condition failed: {}", event.getConditionName());
 *     }
 * });
 * }</pre>
 *
 * <p>Exceptions thrown inside {@code onResult} are silently swallowed by the engine
 * so that a misbehaving listener cannot disrupt rule evaluation.
 */
@FunctionalInterface
public interface ConditionListener {

    /**
     * Called after a condition has been evaluated.
     *
     * @param event the evaluation result and context; never {@code null}
     */
    void onResult(ConditionEvent event);

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
