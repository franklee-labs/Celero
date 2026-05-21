package labs.franklee.celero.engine;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.listener.ConditionEvent;
import labs.franklee.celero.listener.ConditionListener;
import labs.franklee.celero.listener.RuleEvent;
import labs.franklee.celero.listener.RuleListener;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.logic.path.Path;
import labs.franklee.celero.rules.RuleBuilder;

import java.util.*;

/**
 * A rule engine that evaluates rules to a binary {@code true/false} result.
 *
 * <p>Missing input fields are treated as {@code false}. If you need to distinguish
 * between "condition failed" and "field was absent", use {@link AdvancedCeleroEngine}
 * instead.
 *
 * <p>A single engine instance may be shared across threads. Each evaluation call must
 * use its own {@link RuleContext}.
 */
public class DefaultCeleroEngine extends AbstractCeleroEngine {

    private final boolean enableMissingState = false;

    public DefaultCeleroEngine() {
    }

    private final List<ConditionListener> conditionListeners = new ArrayList<>();

    /**
     * Registers a {@link ConditionListener} that is notified after each condition execution.
     *
     * <p>Listeners are invoked in ascending {@link ConditionListener#order()} value.
     * If a condition result is served from cache, the listener is <em>not</em> called again —
     * it fires only when the condition is actually executed.
     *
     * <p>This method is not thread-safe; register all listeners before sharing the engine
     * across threads.
     *
     * @param listener the listener to register; must not be {@code null}
     */
    public void addConditionListener(ConditionListener listener) {
        conditionListeners.add(listener);
        conditionListeners.sort(Comparator.comparingInt(ConditionListener::order));
    }

    private final List<RuleListener> ruleListeners = new ArrayList<>();

    /**
     * Registers a {@link RuleListener} that is notified after each rule evaluation completes.
     *
     * <p>Listeners are invoked in ascending {@link RuleListener#order()} value.
     *
     * <p>This method is not thread-safe; register all listeners before sharing the engine
     * across threads.
     *
     * @param listener the listener to register; must not be {@code null}
     */
    public void addRuleListener(RuleListener listener) {
        ruleListeners.add(listener);
        ruleListeners.sort(Comparator.comparingInt(RuleListener::order));
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
            boolean result = evaluate(rule, ruleContext);
            RuleEvent event = new RuleEvent(rule.getId(), rule.getName(), result, ruleContext);
            this.callRuleListeners(event);
        }
    }

    /**
     * Evaluates a single rule against the given context.
     *
     * <p>This method is thread-safe. Multiple threads may call it concurrently with the
     * same {@link CeleroRule}, as long as each call uses its own {@link RuleContext}.
     *
     * <p>Note: this overload does <em>not</em> fire registered {@link RuleListener}s.
     * Use {@link #evaluate(List, RuleContext)} if you need rule-level listener callbacks.
     *
     * @param rule        the rule to evaluate; must have been built via {@link RuleBuilder}
     * @param ruleContext the input parameters for this evaluation; create with {@link RuleContext#of}
     * @return {@code true} if a fully matched path was found; {@code false} otherwise
     */
    public boolean evaluate(CeleroRule rule, RuleContext ruleContext) {
        Context context = buildContext(ruleContext, rule.isCacheable(), enableMissingState);
        for (Path path : rule.getRule().getPathGroup().paths()) {
            if (execute(rule, path, context)) return true;
        }
        return false;
    }

    private boolean execute(CeleroRule rule, Path path, Context context) {
        Set<Integer> matchedIdx = new HashSet<>();
        Set<Integer> unMatchedIdx = new HashSet<>();
        int skippedAt = path.conditions().size();
        for (int i = 0; i < path.conditions().size(); i++) {
            EvalResult result = null;
            Condition condition = path.conditions().get(i);
            if (context.isEnableConditionResultCache()) {
                result = context.getConditionEvalResult(condition.getInternalUniqueId());
            }
            if (null == result) {
                result = condition.execute(context);
                ConditionEvent event = new ConditionEvent(condition.getRuleId(), condition.getRuleName(),
                        condition.getId(), condition.getName(),
                        result.isTrue(), context.getRuleContext());
                this.callConditionListeners(event);
                if (!result.isTrue()) {
                    if (context.isEnableReport()) {
                        unMatchedIdx.add(i);
                        skippedAt = i+1;
                        Route route = createRoute(matchedIdx, unMatchedIdx, skippedAt, path.conditions());
                        context.getRuleContext().appendRoute(rule, route);
                    }
                    return false;
                } else {
                    matchedIdx.add(i);
                }
            } else if (!result.isTrue()) {
                if (context.isEnableReport()) {
                    unMatchedIdx.add(i);
                    skippedAt = i+1;
                    Route route = createRoute(matchedIdx, unMatchedIdx, skippedAt, path.conditions());
                    context.getRuleContext().appendRoute(rule, route);
                }
                return false;
            } else {
                matchedIdx.add(i);
            }
        }
        if (context.isEnableReport()) {
            Route route = createRoute(matchedIdx, unMatchedIdx, skippedAt, path.conditions());
            context.getRuleContext().appendRoute(rule, route);
        }
        return true;
    }

    private Route createRoute(Set<Integer> matchedIdx, Set<Integer> unMatchedIdx, int skippedAt, List<Condition> conditions) {
        Route route = new Route();
        Set<Route.Item> matched = new HashSet<>();
        Set<Route.Item> unMatched = new HashSet<>();
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
                }
            }
        }
        route.setMatched(matched);
        route.setUnmatched(unMatched);
        route.setSkipped(skipped);
        return route;
    }

    private void callConditionListeners(ConditionEvent event) {
        this.conditionListeners.forEach(listener -> {
            try {
                listener.onResult(event);
            } catch (Throwable _) {
            }
        });
    }

    private void callRuleListeners(RuleEvent event) {
        this.ruleListeners.forEach(listener -> {
            try {
                listener.onRuleResult(event);
            } catch (Throwable _) {
            }
        });
    }
}
