package labs.franklee.celero.engine;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.listener.AdvancedConditionEvent;
import labs.franklee.celero.listener.AdvancedConditionListener;
import labs.franklee.celero.listener.AdvancedRuleEvent;
import labs.franklee.celero.listener.AdvancedRuleListener;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.logic.path.Path;
import labs.franklee.celero.rules.RuleBuilder;

import java.util.*;

/**
 * A rule engine that evaluates rules to a three-state {@link EvalResult}:
 * {@code TRUE}, {@code FALSE}, or {@code INDETERMINATE}.
 *
 * <p>Unlike {@link DefaultCeleroEngine}, this engine does not automatically treat a
 * missing input field as {@code false}. When a condition references a field that is
 * absent from the {@link RuleContext}, the condition yields {@code INDETERMINATE},
 * allowing the caller to defer or handle the ambiguous case explicitly.
 *
 * <p>A single engine instance may be shared across threads. Each evaluation call must
 * use its own {@link RuleContext}.
 */
public class AdvancedCeleroEngine extends AbstractCeleroEngine {

    private final boolean enableMissingState = true;

    public AdvancedCeleroEngine() {
    }

    private final List<AdvancedConditionListener> conditionListeners = new ArrayList<>();

    /**
     * Registers an {@link AdvancedConditionListener} that is notified after each condition
     * execution with a three-state {@link EvalResult}.
     *
     * <p>Listeners are invoked in ascending {@link AdvancedConditionListener#order()} value.
     * If a condition result is served from cache, the listener is <em>not</em> called again —
     * it fires only when the condition is actually executed.
     *
     * <p>This method is not thread-safe; register all listeners before sharing the engine
     * across threads.
     *
     * @param listener the listener to register; must not be {@code null}
     */
    public void addConditionListener(AdvancedConditionListener listener) {
        conditionListeners.add(listener);
        conditionListeners.sort(Comparator.comparingInt(AdvancedConditionListener::order));
    }

    private final List<AdvancedRuleListener> ruleListeners = new ArrayList<>();

    /**
     * Registers an {@link AdvancedRuleListener} that is notified after each rule evaluation
     * completes with a three-state {@link EvalResult}.
     *
     * <p>Listeners are invoked in ascending {@link AdvancedRuleListener#order()} value.
     *
     * <p>This method is not thread-safe; register all listeners before sharing the engine
     * across threads.
     *
     * @param listener the listener to register; must not be {@code null}
     */
    public void addRuleListener(AdvancedRuleListener listener) {
        ruleListeners.add(listener);
        ruleListeners.sort(Comparator.comparingInt(AdvancedRuleListener::order));
    }

    /**
     * Evaluates a list of rules against the given context, firing registered listeners
     * after each rule completes.
     *
     * <p>This method is thread-safe. Multiple threads may call it concurrently with the
     * same rule list, as long as each call uses its own {@link RuleContext}.
     *
     * @param rules       the rules to evaluate; must have been built via {@link RuleBuilder}
     * @param ruleContext the input parameters for this evaluation; create with {@link RuleContext#of}
     */
    public void evaluate(List<CeleroRule> rules, RuleContext ruleContext) {
        for (CeleroRule rule : rules) {
            EvalResult result = evaluate(rule, ruleContext);
            AdvancedRuleEvent event = new AdvancedRuleEvent(rule.getId(), rule.getName(), result, ruleContext);
            this.callRuleListeners(event);
        }
    }

    /**
     * Evaluates a single rule against the given context.
     *
     * <p>This method is thread-safe. Multiple threads may call it concurrently with the
     * same {@link CeleroRule}, as long as each call uses its own {@link RuleContext}.
     *
     * <p>Note: this overload does <em>not</em> fire registered {@link AdvancedRuleListener}s.
     * Use {@link #evaluate(List, RuleContext)} if you need rule-level listener callbacks.
     *
     * @param rule        the rule to evaluate; must have been built via {@link RuleBuilder}
     * @param ruleContext the input parameters for this evaluation; create with {@link RuleContext#of}
     * @return {@link EvalResult#TRUE}          if a fully matched path was found
     *         {@link EvalResult#FALSE}         if every path had at least one unmatched condition
     *         {@link EvalResult#INDETERMINATE} if no path was false, but at least one condition
     *                                          could not be evaluated due to an absent field
     */
    public EvalResult evaluate(CeleroRule rule, RuleContext ruleContext) {
        Context context = buildContext(ruleContext, rule.isCacheable(), enableMissingState);
        boolean miss = false;
        for (Path path : rule.getRule().getPathGroup().paths()) {
            EvalResult result = execute(rule, path, context);
            if (result.isTrue()) {
                return result;
            }
            if (result.isIndeterminate()) {
                miss = true;
            }
        }
        return miss ? EvalResult.INDETERMINATE : EvalResult.FALSE;
    }

    private EvalResult execute(CeleroRule rule, Path path, Context context) {
        boolean absent = false;
        Set<Integer> matchedIdx = new HashSet<>();
        Set<Integer> unMatchedIdx = new HashSet<>();
        Set<Integer> absentIdx = new HashSet<>();
        int skippedAt = path.conditions().size();
        for (int i = 0; i < path.conditions().size(); i++) {
            EvalResult result = null;
            Condition condition = path.conditions().get(i);
            if (context.isEnableConditionResultCache()) {
                result = context.getConditionEvalResult(condition.getInternalUniqueId());
            }
            if (null == result) {
                result = condition.execute(context);
                AdvancedConditionEvent event = new AdvancedConditionEvent(condition.getRuleId(), condition.getRuleName(),
                        condition.getId(), condition.getName(),
                        result, context.getRuleContext());
                this.callConditionListeners(event);
                if (result.isFalse()) {
                    unMatchedIdx.add(i);
                    skippedAt = i+1;
                    if (context.isEnableReport()) {
                        Route route = createRoute(matchedIdx, unMatchedIdx, absentIdx, skippedAt, path.conditions());
                        context.getRuleContext().appendRoute(rule, route);
                    }
                    return EvalResult.FALSE;
                } else if (result.isIndeterminate()) {
                    absentIdx.add(i);
                    absent = true;
                } else {
                    matchedIdx.add(i);
                }
            } else if (result.isFalse()) {
                unMatchedIdx.add(i);
                skippedAt = i+1;
                if (context.isEnableReport()) {
                    Route route = createRoute(matchedIdx, unMatchedIdx, absentIdx, skippedAt, path.conditions());
                    context.getRuleContext().appendRoute(rule, route);
                }
                return EvalResult.FALSE;
            } else if (result.isIndeterminate()) {
                absentIdx.add(i);
                absent = true;
            } else {
                matchedIdx.add(i);
            }
        }
        if (context.isEnableReport()) {
            Route route = createRoute(matchedIdx, unMatchedIdx, absentIdx, skippedAt, path.conditions());
            context.getRuleContext().appendRoute(rule, route);
        }
        return absent ? EvalResult.INDETERMINATE : EvalResult.TRUE;
    }

    private Route createRoute(Set<Integer> matchedIdx, Set<Integer> unMatchedIdx, Set<Integer> absentIdx, int skippedAt, List<Condition> conditions) {
        Route route = new Route();
        Set<Route.Item> matched = new HashSet<>();
        Set<Route.Item> unMatched = new HashSet<>();
        Set<Route.Item> absent = new HashSet<>();
        Set<Route.Item> skipped = new HashSet<>();
        for (int i = 0; i < conditions.size(); i++) {
            Condition condition = conditions.get(i);
            if (i >= skippedAt) {
                skipped.add(new Route.Item(condition.getId(), condition.getName()));
            } else {
                if (matchedIdx.contains(i)) {
                    matched.add(new Route.Item(condition.getId(), condition.getName()));
                } else if (unMatchedIdx.contains(i)) {
                    unMatched.add(new Route.Item(condition.getId(), condition.getName()));
                } else if (absentIdx.contains(i)) {
                    absent.add(new Route.Item(condition.getId(), condition.getName()));
                }
            }
        }
        route.setMatched(matched);
        route.setUnmatched(unMatched);
        route.setAbsent(absent);
        route.setSkipped(skipped);
        return route;
    }

    private void callConditionListeners(AdvancedConditionEvent event) {
        this.conditionListeners.forEach(listener -> {
            try {
                listener.onResult(event);
            } catch (Throwable _) {
            }
        });
    }

    private void callRuleListeners(AdvancedRuleEvent event) {
        this.ruleListeners.forEach(listener -> {
            try {
                listener.onRuleResult(event);
            } catch (Throwable _) {
            }
        });
    }
}
