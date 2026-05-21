package labs.franklee.celero.engine;

import labs.franklee.celero.logic.path.PathGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only structural view of all resolved evaluation paths for a {@link CeleroRule}.
 *
 * <p>The engine expands a rule's logical expression into a set of flat condition sequences
 * (paths). Each path is exposed here as a {@link Solution}. Inspecting {@code Solutions}
 * is useful for understanding the rule layout — for example, to display "what does a user
 * need to satisfy?" — without executing an evaluation.
 *
 * <p>Instances are obtained via {@link CeleroRule#getSolutions()}.
 */
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

    /**
     * Returns the total number of resolved evaluation paths (solutions).
     *
     * @return number of solutions; always &gt;= 1 for a valid rule
     */
    public int getSolutionCount() {
        return this.solutions.size();
    }

    /**
     * Returns the {@link Solution} at the given index.
     *
     * @param i zero-based index
     * @return the solution, or {@code null} if {@code i} is out of bounds
     */
    public Solution getSolutionAt(int i) {
        if (i < 0 || i >= this.solutions.size()) {
            return null;
        }
        return this.solutions.get(i);
    }

    /**
     * A single resolved evaluation path — an ordered sequence of conditions that must
     * all be satisfied for the rule to match via this path.
     */
    public static class Solution {
        private final List<Condition> conditions;

        Solution(List<Condition> conditions) {
            this.conditions = conditions;
        }

        /**
         * Returns the number of conditions in this solution path.
         *
         * @return condition count; always &gt;= 1
         */
        public int getConditionCount() {
            return this.conditions.size();
        }

        /**
         * Returns the {@link Condition} at the given index within this solution.
         *
         * @param i zero-based index
         * @return the condition, or {@code null} if {@code i} is out of bounds
         */
        public Condition getConditionAt(int i) {
            if (i < 0 || i >= this.conditions.size()) {
                return null;
            }
            return this.conditions.get(i);
        }
    }

    /**
     * A lightweight descriptor for a condition within a {@link Solution}.
     *
     * <p>Carries only the identity information ({@code id} and {@code name});
     * it does not expose evaluation logic.
     */
    public static class Condition {
        private String id;
        private String name;

        Condition(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * Returns the condition id as declared in the rule definition.
         *
         * @return condition id; may be {@code null} for internally generated nodes
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the human-readable condition name as declared in the rule definition.
         *
         * @return condition name; may be {@code null} if not set
         */
        public String getName() {
            return name;
        }
    }
}
