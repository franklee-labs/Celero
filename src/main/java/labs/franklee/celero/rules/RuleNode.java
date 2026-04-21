package labs.franklee.celero.rules;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.logic.base.Node;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RelationNode.class, name = RuleNode.TYPE_RELATION),
        @JsonSubTypes.Type(value = ConditionNode.class,  name = RuleNode.TYPE_CONDITION)
})
public abstract class RuleNode {

    final static String TYPE_RELATION = "relation";
    final static String TYPE_CONDITION = "condition";

    private String id;

    private String name;

    private String description;

    private String type;

    private String sign;

    public String getId() {
        return id;
    }

    public RuleNode setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public RuleNode setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public RuleNode setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getType() {
        return type;
    }

    public RuleNode setType(String type) {
        this.type = type;
        return this;
    }

    public String getSign() {
        return sign;
    }

    public RuleNode setSign(String sign) {
        this.sign = sign;
        return this;
    }

    public boolean isRelationNode() {
        return RuleNode.TYPE_RELATION.equalsIgnoreCase(this.getType());
    }

    public boolean isConditionNode() {
        return RuleNode.TYPE_CONDITION.equalsIgnoreCase(this.getType());
    }

    public abstract Node transform(RuleMeta meta) throws InvalidRuleNodeException;
}
