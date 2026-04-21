package labs.franklee.celero.rules;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.logic.base.Node;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.Validation;
import labs.franklee.celero.logic.path.PathGroup;

public class Rule {

    /**
     * unique id
     */
    private final String id;

    /**
     * rule's name, no unique constraint
     */
    private final String name;

    /**
     * description of this rule
     */
    private final String description;

    /**
     * enable condition-result-cache whether.
     * if false, condition result will not be cached even though {@link ConditionNode.cacheable} is true.
     * if true, condition result can be cached when {@link ConditionNode.cacheable} is set to true.
     */
    private volatile boolean cacheable;

    /**
     * root of rule tree(JSON rule configuration)
     */
    private final RelationNode ruleRoot;

    /**
     * root of logic tree
     */
    private Node root;

    /**
     * paths to match this rule
     */
    private PathGroup pathGroup;

    public Rule(String id, String name, String description, boolean cacheable, RelationNode ruleRoot) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cacheable = cacheable;
        this.ruleRoot = ruleRoot;
    }

    void build() throws Throwable {
        RuleMeta meta = new RuleMeta(this.id, this.name, this.description);
        this.root = ruleRoot.transform(meta);
        Validation b = this.root.validateAll();
        if (!b.isValid()) {
            throw new InvalidRuleNodeException(b.getMessage());
        }
        Relation relation = this.root.resolve();
        this.pathGroup = relation.getPathGroup();
        this.pathGroup.sort();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Rule setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public PathGroup getPathGroup() {
        return pathGroup;
    }
}
