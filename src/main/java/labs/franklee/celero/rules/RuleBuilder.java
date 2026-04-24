package labs.franklee.celero.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import labs.franklee.celero.engine.CeleroRule;
import labs.franklee.celero.exceptions.InvalidRuleNodeException;

import java.util.List;

public class RuleBuilder {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    private String id;
    private String name;
    private String description;
    private boolean cacheable;
    private RelationNode root;

    private RuleBuilder() {}

    public static RuleBuilder create() {
        return new RuleBuilder();
    }

    /**
     * JSON format — root can be a relation node or a single condition node:
     * <pre>
     * {
     *   "id": "rule-001",
     *   "name": "...",
     *   "description": "...",
     *   "root": { "type": "relation"|"condition", "sign": "...", ... }
     * }
     * </pre>
     * When root is a condition node it is automatically wrapped in an AND relation.
     */
    public static RuleBuilder fromJson(String id, String json) throws Exception {
        RuleNode node = MAPPER.readValue(json, RuleNode.class);
        return new RuleBuilder()
                .id(id)
                .root(node);
    }

    public RuleBuilder id(String id) {
        this.id = id;
        return this;
    }

    public RuleBuilder name(String name) {
        this.name = name;
        return this;
    }

    public RuleBuilder description(String description) {
        this.description = description;
        return this;
    }

    public RuleBuilder cacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    public RuleBuilder root(RuleNode node) {
        this.root = wrapIfNeeded(node);
        return this;
    }

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
