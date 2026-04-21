package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;

import java.util.Map;

public interface ConditionFactory {

    Condition create(Map<String, Object> properties);
}
