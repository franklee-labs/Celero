package labs.franklee.celero.engine;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.listener.ConditionEvent;
import labs.franklee.celero.listener.ConditionListener;
import labs.franklee.celero.listener.RuleEvent;
import labs.franklee.celero.listener.RuleListener;
import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.logic.base.EvalResult;
import labs.franklee.celero.logic.path.Path;

import java.util.*;

public class DefaultCeleroEngine extends AbstractCeleroEngine {

    private final boolean enableMissingState = false;

    public DefaultCeleroEngine() {
    }

    private final List<ConditionListener> conditionListeners = new ArrayList<>();

    /**
     * Add condition listener.
     * ConditionListener will be called immediately after executing a condition.
     * Cached condition will not be called twice, so ConditionListener will not be called twice.
     * @param listener A listener implemented {@link ConditionListener}
     */
    public void addConditionListener(ConditionListener listener) {
        conditionListeners.add(listener);
        conditionListeners.sort(Comparator.comparingInt(ConditionListener::order));
    }

    private final List<RuleListener> ruleListeners = new ArrayList<>();

    /**
     * Add rule listener.
     * RuleListener will be called immediately after executing a rule.
     * @param listener A listener implemented {@link RuleListener}
     */
    public void addRuleListener(RuleListener listener) {
        ruleListeners.add(listener);
        ruleListeners.sort(Comparator.comparingInt(RuleListener::order));
    }

    /**
     * Thread-safe execution of rules
     * @param rules List of {@link CeleroRule}
     * @param ruleContext an instance of {@link RuleContext}. RuleContext.of(map)
     */
    public void evaluate(List<CeleroRule> rules, RuleContext ruleContext) {
        for (CeleroRule rule : rules) {
            boolean result = evaluate(rule, ruleContext);
            RuleEvent event = new RuleEvent(rule.getId(), rule.getName(), result, ruleContext);
            this.callRuleListeners(event);
        }
    }

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
