package labs.franklee.celero.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Carries the input parameters and mutable state for a single rule-evaluation call.
 *
 * <p>A {@code RuleContext} is created per evaluation via {@link #of(Map)} and must
 * <em>not</em> be shared across concurrent evaluations. The input {@code params} map
 * is made immutable on construction; all other state (attributes, reports) is mutable
 * and scoped to the lifetime of one evaluation.
 */
public class RuleContext {

    private final Map<String, Object> params;
    private final Map<String, Object> attributes = new HashMap<>();
    private boolean enableReports = false;
    private final Map<CeleroRule, Report> reports = new HashMap<>();

    private RuleContext(Map<String, Object> params) {
        this.params = params == null ? Collections.emptyMap() : Collections.unmodifiableMap(params);
    }

    /**
     * Creates a new {@code RuleContext} with the given input parameters.
     *
     * <p>Parameter keys must not start with {@code _} (that prefix is reserved for
     * internal built-in values). The map is copied into an unmodifiable view.
     *
     * @param params the named values that conditions will read during evaluation;
     *               may be empty but must not be {@code null}
     * @return a new {@code RuleContext}
     */
    public static RuleContext of(Map<String, Object> params) {
        return new RuleContext(params);
    }

    /**
     * Returns the immutable input parameters supplied at construction time.
     *
     * @return unmodifiable parameter map; never {@code null}
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Returns whether per-rule execution reports are being collected.
     *
     * @return {@code true} if report collection is enabled
     */
    public boolean isEnableReports() {
        return enableReports;
    }

    /**
     * Enables or disables per-rule execution reports.
     *
     * <p>When enabled, the engine records which conditions matched, did not match,
     * were absent, or were skipped for every path it evaluates. Retrieve the results
     * via {@link #getReports()} after evaluation.
     *
     * <p>Report collection has a small overhead; disable it in hot paths where
     * diagnostic detail is not needed.
     *
     * @param enableReports {@code true} to enable, {@code false} to disable
     * @return {@code this}, for method chaining
     */
    public RuleContext setEnableReports(boolean enableReports) {
        this.enableReports = enableReports;
        return this;
    }

    void appendRoute(CeleroRule rule, Route route) {
        Report report = this.reports.getOrDefault(rule, new Report());
        report.append(route);
        this.reports.put(rule, report);
    }

    /**
     * Returns the execution report for each rule evaluated with this context.
     *
     * <p>The map is only populated when {@link #setEnableReports(boolean) setEnableReports(true)}
     * was called before evaluation. Each entry maps a {@link CeleroRule} to its
     * {@link Report}, which contains one {@link Route} per path the engine attempted.
     *
     * @return map from rule to its report; empty if reports were not enabled
     */
    public Map<CeleroRule, Report> getReports() {
        return reports;
    }

    /**
     * Returns the attribute stored under {@code key}, or {@code null} if absent.
     *
     * <p>Attributes are a general-purpose side-channel for passing data between
     * listeners or accumulating state during an evaluation. They are independent
     * of the condition input {@link #getParams() params}.
     *
     * @param key the attribute key
     * @return the stored value, or {@code null}
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Stores a value under {@code key} for the lifetime of this evaluation.
     *
     * <p>Typical use: a lower-priority listener writes intermediate data that a
     * higher-priority listener reads, or a listener accumulates results that the
     * caller reads after {@code engine.evaluate(...)} returns.
     *
     * @param key   the attribute key; must not be {@code null}
     * @param value the value to store; may be {@code null} to clear an entry
     * @return {@code this}, for method chaining
     */
    public RuleContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Returns a read-only view of all attributes currently stored in this context.
     *
     * @return unmodifiable attribute map; never {@code null}
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
