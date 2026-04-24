package labs.franklee.celero.rules;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.logic.base.Node;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.impl.AND;
import labs.franklee.celero.logic.impl.NOT;
import labs.franklee.celero.logic.impl.OR;

import java.util.ArrayList;
import java.util.List;

public class RelationNode extends RuleNode {

    private List<RuleNode> children = new ArrayList<>();

    public RelationNode() {
        this.setType("relation");
    }

    public List<RuleNode> getChildren() {
        return children;
    }

    public RuleNode setChildren(List<RuleNode> children) {
        this.children = children;
        return this;
    }

    public Node transform(RuleMeta meta) throws InvalidRuleNodeException {
        Node node = createRelation(this.getSign());
        node.setRuleId(meta.getId());
        List<Node> tmp = new ArrayList<>(this.getChildren().size());
        for (RuleNode child : this.getChildren()) {
            Node n = child.transform(meta);
            tmp.add(n);
        }
        node.setChildren(tmp);
        return node;
    }

    public Relation createRelation(String sign) throws InvalidRuleNodeException {
        Relation node;
        if ("and".equalsIgnoreCase(sign)) {
            node = new AND();
        } else if ("or".equalsIgnoreCase(sign)) {
            node = new OR();
        } else if ("not".equalsIgnoreCase(sign)) {
            node = new NOT();
        } else {
            throw new InvalidRuleNodeException("unsupported relation sign:" + sign + " support sign [AND/OR/NOT]");
        }
        node.setId(this.getId());
        node.setName(this.getName());
        return node;
    }
}
