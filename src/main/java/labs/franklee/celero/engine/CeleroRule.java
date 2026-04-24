package labs.franklee.celero.engine;

import labs.franklee.celero.rules.Rule;

public class CeleroRule {

    private final Rule rule;

    private final Solutions solutions;

    public CeleroRule(Rule rule) {
        this.rule = rule;
        this.solutions = new Solutions(this.rule.getPathGroup());
    }

    Rule getRule() {
        return this.rule;
    }

    public Solutions getSolutions() {
        return this.solutions;
    }


    public String getId() {
        return this.rule.getId();
    }

    public String getName() {
        return this.rule.getName();
    }

    public String getDescription() {
        return this.rule.getDescription();
    }

    public CeleroRule setCacheable(boolean cacheable) {
        this.rule.setCacheable(cacheable);
        return this;
    }

    public boolean isCacheable() {
        return this.rule.isCacheable();
    }


}
