package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.rules.ConditionNode;

public interface ConditionFactory {

    Condition create(ConditionNode conditionNode);
}
