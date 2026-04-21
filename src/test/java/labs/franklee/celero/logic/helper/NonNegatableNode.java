package labs.franklee.celero.logic.helper;

import labs.franklee.celero.logic.base.Node;
import labs.franklee.celero.logic.base.Relation;
import labs.franklee.celero.logic.base.Validation;

/**
 * A Node that intentionally does NOT implement Negatable.
 * Used to cover the defensive "child instanceof Negatable" false-branch in AND/OR/NOT.
 */
public class NonNegatableNode extends Node {

    @Override
    public Validation validate() {
        return new Validation(true, null);
    }

    @Override
    public Relation resolve() {
        return null;
    }
}
