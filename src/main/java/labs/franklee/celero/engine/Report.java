package labs.franklee.celero.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates the execution trace of a single rule evaluation.
 *
 * <p>A {@code Report} is created by the engine when
 * {@link RuleContext#setEnableReports(boolean) setEnableReports(true)} is set on the
 * {@link RuleContext}. One {@link Route} is appended per evaluation path the engine
 * attempts, in the order they were tried.
 *
 * <p>Retrieve a rule's report via {@link RuleContext#getReports()}.
 */
public class Report {

    private final List<Route> routes = new ArrayList<>();

    void append(Route route) {
        this.routes.add(route);
    }

    /**
     * Returns all routes recorded during this rule's evaluation, in evaluation order.
     *
     * <p>Each {@link Route} captures which conditions matched, did not match, were
     * absent, or were skipped for one path attempt.
     *
     * @return list of routes; empty if the rule has no paths (should not occur for
     *         a valid rule) or if reports were not enabled
     */
    public List<Route> getRoutes() {
        return routes;
    }
}
