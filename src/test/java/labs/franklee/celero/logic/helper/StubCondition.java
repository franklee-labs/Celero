package labs.franklee.celero.logic.helper;

import labs.franklee.celero.context.Context;
import labs.franklee.celero.logic.base.Condition;

/**
 * A concrete Condition for use in unit tests.
 * Stores a name for assertion purposes; negate() returns a new StubCondition prefixed with "!".
 */
public class StubCondition extends Condition {

    private final String name;

    public StubCondition(String name) {
        this.name = name;
    }

    public StubCondition(String name, int priority) {
        super(priority);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Condition negate() {
        return new StubCondition("!" + name);
    }

    @Override
    public boolean evaluate(Context context) {
        return false;
    }
}
