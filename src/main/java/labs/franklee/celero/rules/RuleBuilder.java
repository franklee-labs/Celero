package labs.franklee.celero.rules;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.exceptions.InvalidRuleNodeException;

import java.util.List;

/**
 * Fluent builder for constructing a compiled {@link CeleroRule}.
 *
 * <p>Rules can be defined either programmatically via the builder methods or
 * loaded from a JSON string via {@link #fromJson(String, String)}.
 *
 * <p>Typical usage:
 * <pre>{@code
 * CeleroRule rule = RuleBuilder.fromJson("rule-id", jsonString)
 *         .name("My Rule")
 *         .description("...")
 *         .cacheable(true)
 *         .build();
 * }</pre>
 *
 * <p>A builder instance is single-use and not thread-safe. The produced
 * {@link CeleroRule} is immutable and safe to share across threads.
 */
public class RuleBuilder {

    private final static ObjectMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    private String id;
    private String name;
    private String description;
    private boolean cacheable;
    private RelationNode root;

    private RuleBuilder() {}

    /**
     * Creates a new, empty {@code RuleBuilder}.
     *
     * <p>Use this when constructing the rule tree programmatically via
     * {@link #root(RuleNode)}. To load a rule from JSON, prefer
     * {@link #fromJson(String, String)}.
     *
     * @return a new builder instance
     */
    public static RuleBuilder create() {
        return new RuleBuilder();
    }

    /**
     * Creates a {@code RuleBuilder} pre-populated from a JSON rule definition.
     *
     * <p>The JSON root can be either a relation node or a single condition node.
     * A bare condition node is automatically wrapped in an {@code AND} relation.
     *
     * <p>Example — AND relation with a single GT condition:
     * <pre>{@code
     * {
     *   "type": "relation",
     *   "sign": "AND",
     *   "children": [
     *     {
     *       "id": "c1",
     *       "type": "condition",
     *       "sign": "GT",
     *       "properties": { "field": "age", "value": "18", "valueType": "Number" }
     *     }
     *   ]
     * }
     * }</pre>
     *
     * <p>Unknown JSON fields are silently ignored.
     *
     * @param id   the unique rule identifier; must not be {@code null}
     * @param json the rule definition in JSON format; must not be {@code null}
     * @return a builder with {@code id} and {@code root} pre-set
     * @throws Exception if the JSON cannot be parsed or contains an invalid structure
     */
    public static RuleBuilder fromJson(String id, String json) throws Exception {
        RuleNode node = MAPPER.readValue(json, RuleNode.class);
        return new RuleBuilder()
                .id(id)
                .root(node);
    }

    /**
     * Sets the unique identifier for this rule.
     *
     * <p>Required — {@link #build()} throws {@link IllegalStateException} if not set.
     *
     * @param id unique rule id; must not be {@code null}
     * @return {@code this}
     */
    public RuleBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the human-readable name for this rule.
     *
     * @param name rule name; may be {@code null}
     * @return {@code this}
     */
    public RuleBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the description for this rule.
     *
     * @param description rule description; may be {@code null}
     * @return {@code this}
     */
    public RuleBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Enables or disables condition-result caching for this rule.
     *
     * <p>Caching avoids re-evaluating the same condition when the rule's logical
     * expression expands into multiple paths that share conditions (e.g.
     * {@code A AND (B OR C)} splits into {@code A&B} and {@code A&C}; with caching
     * enabled, {@code A} is evaluated only once).
     *
     * <p>Caching is effective only for conditions that also have {@code cacheable=true}
     * set individually in their condition node definition.
     *
     * @param cacheable {@code true} to enable caching; defaults to {@code false}
     * @return {@code this}
     */
    public RuleBuilder cacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    /**
     * Sets the root node of the rule tree.
     *
     * <p>Required — {@link #build()} throws {@link IllegalStateException} if not set.
     * If a {@link ConditionNode} is passed instead of a {@link RelationNode}, it is
     * automatically wrapped in an {@code AND} relation.
     *
     * @param node the root rule node; must not be {@code null}
     * @return {@code this}
     */
    public RuleBuilder root(RuleNode node) {
        this.root = wrapIfNeeded(node);
        return this;
    }

    /**
     * Compiles the rule and returns an immutable {@link CeleroRule} ready for evaluation.
     *
     * <p>This method validates the rule structure, resolves all logical paths, and
     * compiles each condition's expression. It is the most expensive step in rule
     * setup and should be called once at startup, not on every evaluation.
     *
     * @return a compiled, thread-safe {@link CeleroRule}
     * @throws IllegalStateException    if {@code id} or {@code root} was not set
     * @throws InvalidRuleNodeException if the rule tree contains invalid or
     *                                  unsupported condition definitions
     */
    public CeleroRule build() throws Throwable {
        if (this.root == null) {
            throw new IllegalStateException("root must not be null");
        }
        if (null == this.id) {
            throw new IllegalStateException("rule id must not be null");
        }
        Rule rule = new Rule(this.id, this.name, this.description, this.cacheable, this.root);
        try {
            rule.build();
        } catch (Throwable t) {
            throw new InvalidRuleNodeException(t);
        }
        return new CeleroRule(rule);
    }

    private static RelationNode wrapIfNeeded(RuleNode node) {
        if (node instanceof RelationNode relationNode) {
            return relationNode;
        }
        RelationNode wrapper = new RelationNode();
        wrapper.setSign("AND");
        wrapper.setChildren(List.of(node));
        return wrapper;
    }
}
