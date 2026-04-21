package labs.franklee.celero.logic.path;

import labs.franklee.celero.logic.base.Condition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Path {
    private final List<Condition> conditions = new ArrayList<>();

    public Path addCondition(Condition condition) {
        this.conditions.add(condition);
        return this;
    }

    public Path addAllCondition(List<Condition> conditions) {
        this.conditions.addAll(conditions);
        return this;
    }

    public List<Condition> conditions() {
        return this.conditions;
    }

    public void sort() {
        if (!this.conditions.isEmpty()) {
            this.conditions.sort(Comparator.comparingInt(Condition::getPriority));
        }
    }
}
