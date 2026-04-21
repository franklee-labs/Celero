package labs.franklee.celero.logic.impl;

import labs.franklee.celero.logic.base.*;

import java.util.Collections;

public class NOT extends Relation {

    public NOT() {
        super();
    }

    @Override
    public Validation validate() {
        boolean b = this.getChildren().size() == 1;
        return b ? new Validation(true, null) :
                new Validation(false, "Not node can only hold exactly one node.");
    }

    @Override
    public RelationType relation() {
        return RelationType.Not;
    }

    @Override
    public Relation resolve() throws Exception {
        Node child = this.getChildren().getFirst();
        if (child instanceof Negatable) {
            return ((Negatable<?>) child).negate().resolve();
        }
        throw new UnsupportedOperationException("Negatable child node is required. " + child.getClass().getName() + " is not negatable!");
    }

    @Override
    public Relation negate() throws Exception {
        Node node = this.getChildren().getFirst();
        if (node instanceof Relation) {
            return (Relation) node;
        }
        Relation and = new AND();
        and.setChildren(Collections.singletonList(node));
        return and;
    }

}