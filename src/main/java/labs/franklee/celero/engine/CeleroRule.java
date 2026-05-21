package labs.franklee.celero.engine;

import labs.franklee.celero.rules.Rule;
import labs.franklee.celero.rules.RuleBuilder;

/**
 * A compiled, immutable rule ready for evaluation.
 *
 * <p>Instances are created exclusively via {@link RuleBuilder#build()}.
 * Once built, a {@code CeleroRule} is safe to share across threads and
 * to evaluate concurrently, as long as each evaluation uses its own
 * {@link RuleContext}.
 */
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

    /**
     * Returns a read-only structural view of all resolved evaluation paths for this rule.
     *
     * <p>Each {@link Solutions.Solution} corresponds to one path the engine will try in
     * order. Inspecting solutions is useful for understanding the rule structure without
     * running an evaluation.
     *
     * @return the {@link Solutions} for this rule; never {@code null}
     */
    public Solutions getSolutions() {
        return this.solutions;
    }

    /**
     * Returns the unique identifier of this rule, as supplied to {@link RuleBuilder#id}.
     *
     * @return rule id; never {@code null}
     */
    public String getId() {
        return this.rule.getId();
    }

    /**
     * Returns the human-readable name of this rule, as supplied to {@link RuleBuilder#name}.
     *
     * @return rule name; may be {@code null} if not set
     */
    public String getName() {
        return this.rule.getName();
    }

    /**
     * Returns the description of this rule, as supplied to {@link RuleBuilder#description}.
     *
     * @return rule description; may be {@code null} if not set
     */
    public String getDescription() {
        return this.rule.getDescription();
    }

    /**
     * Enables or disables condition-result caching for this rule at runtime.
     *
     * <p>This is a volatile write and takes effect on the next evaluation call.
     * Caching only applies to conditions that also have {@code cacheable=true} set
     * individually in the rule definition.
     *
     * @param cacheable {@code true} to enable caching, {@code false} to disable
     * @return {@code this}, for method chaining
     */
    public CeleroRule setCacheable(boolean cacheable) {
        this.rule.setCacheable(cacheable);
        return this;
    }

    /**
     * Returns whether condition-result caching is currently enabled for this rule.
     *
     * @return {@code true} if caching is enabled
     */
    public boolean isCacheable() {
        return this.rule.isCacheable();
    }
}
