package labs.franklee.celero.logic.base;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {
    protected String id;
    protected NodeType type;
    private List<Node> children = new ArrayList<>();
    protected String name = "Node";
    private String ruleId = null;
    private String ruleName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NodeType getType() {
        return type;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleId() {
        return ruleId;
    }

    public Node setRuleId(String ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public String getRuleName() {
        return ruleName;
    }

    public Node setRuleName(String ruleName) {
        this.ruleName = ruleName;
        return this;
    }

    public Validation validateAll() {
        Validation b = this.validate();
        if (!b.isValid()) {
            return b;
        }
        if (this.getChildren() != null && !this.getChildren().isEmpty()) {
            for (Node child : this.getChildren()) {
                b = child.validateAll();
                if (!b.isValid()) {
                    return b;
                }
            }
        }
        return b;
    }

    public abstract Validation validate();

    public abstract Relation resolve() throws Exception;

}
