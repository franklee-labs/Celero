package labs.franklee.celero.engine;

import labs.franklee.celero.logic.path.PathGroup;

import java.util.ArrayList;
import java.util.List;

public class Solutions {

    private final List<Solution> solutions;

    Solutions(PathGroup pathGroup) {
        this.solutions = new ArrayList<>(pathGroup.paths().size());
        pathGroup.paths().forEach(path -> {
            List<Condition> conditions = new ArrayList<>(path.conditions().size());
            path.conditions().forEach(cond -> {
                conditions.add(new Condition(cond.getId(), cond.getName()));
            });
            this.solutions.add(new Solution(conditions));
        });
    }

    public int getSolutionCount() {
        return this.solutions.size();
    }

    public Solution getSolutionAt(int i) {
        if (i < 0 || i >= this.solutions.size()) {
            return null;
        }
        return this.solutions.get(i);
    }


    public static class Solution {
        private final List<Condition> conditions;

        Solution(List<Condition> conditions) {
            this.conditions = conditions;
        }

        public int getConditionCount() {
            return this.conditions.size();
        }

        public Condition getConditionAt(int i) {
            if (i < 0 || i >= this.conditions.size()) {
                return null;
            }
            return this.conditions.get(i);
        }
    }

    public static class Condition {
        private String id;
        private String name;

        Condition(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
