package labs.franklee.celero.engine;

import labs.franklee.celero.rules.Rule;

import java.util.*;

public class RuleContext {

    private final Map<String, Object> params;
    private final Map<String, Object> attributes = new HashMap<>();
    private boolean enableReports = false;
    private final Map<Rule, Report> reports = new HashMap<>();

    private RuleContext(Map<String, Object> params) {
        this.params = params == null ? Collections.emptyMap() : Collections.unmodifiableMap(params);
    }

    public static RuleContext of(Map<String, Object> params) {
        return new RuleContext(params);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public boolean isEnableReports() {
        return enableReports;
    }

    public RuleContext setEnableReports(boolean enableReports) {
        this.enableReports = enableReports;
        return this;
    }

    void appendRoute(Rule rule, Route route) {
        Report report = this.reports.getOrDefault(rule, new Report());
        report.append(route);
        this.reports.put(rule, report);
    }

    public Map<Rule, Report> getReports() {
        return reports;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public RuleContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
