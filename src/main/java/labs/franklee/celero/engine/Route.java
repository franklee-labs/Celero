package labs.franklee.celero.engine;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Records the condition evaluation outcome for one path (solution) attempt.
 *
 * <p>When a rule is evaluated with reports enabled, the engine appends one
 * {@code Route} to the rule's {@link Report} for every path it tries. Each condition
 * in that path is classified into exactly one of four buckets:
 *
 * <ul>
 *   <li><b>matched</b>   — condition evaluated to {@code true}</li>
 *   <li><b>unmatched</b> — condition evaluated to {@code false}; this path failed here</li>
 *   <li><b>absent</b>    — required field was missing ({@code INDETERMINATE});
 *                          only populated by {@link AdvancedCeleroEngine}</li>
 *   <li><b>skipped</b>   — not evaluated because an earlier condition already failed</li>
 * </ul>
 */
public class Route {

    private Set<Item> matched = new HashSet<>();
    private Set<Item> unmatched = new HashSet<>();
    private Set<Item> absent = new HashSet<>();
    private Set<Item> skipped = new HashSet<>();

    void setMatched(Set<Item> items) {
        this.matched = items;
    }

    void setUnmatched(Set<Item> items) {
        this.unmatched = items;
    }

    void setAbsent(Set<Item> items) {
        this.absent = items;
    }

    void setSkipped(Set<Item> items) {
        this.skipped = items;
    }

    /**
     * Returns the conditions that evaluated to {@code true} on this path.
     *
     * @return set of matched condition descriptors; never {@code null}
     */
    public Set<Item> getMatched() {
        return matched;
    }

    /**
     * Returns the conditions that evaluated to {@code false} on this path.
     *
     * <p>At most one condition will be present here for any given path attempt,
     * because the engine short-circuits on the first failure.
     *
     * @return set of unmatched condition descriptors; never {@code null}
     */
    public Set<Item> getUnmatched() {
        return unmatched;
    }

    /**
     * Returns the conditions whose required input field was absent from the
     * {@link RuleContext} ({@code INDETERMINATE} result).
     *
     * <p>This set is only populated when using {@link AdvancedCeleroEngine}.
     * {@link DefaultCeleroEngine} treats absent fields as {@code false} and places
     * them in {@link #getUnmatched()} instead.
     *
     * @return set of absent condition descriptors; never {@code null}
     */
    public Set<Item> getAbsent() {
        return absent;
    }

    /**
     * Returns the conditions that were not evaluated because an earlier condition
     * on this path already failed, making further evaluation unnecessary.
     *
     * @return set of skipped condition descriptors; never {@code null}
     */
    public Set<Item> getSkipped() {
        return skipped;
    }

    /**
     * A lightweight descriptor identifying a condition within a {@link Route}.
     */
    public static class Item {
        private final String conditionId;
        private final String conditionName;

        public Item(String conditionId, String conditionName) {
            this.conditionId = conditionId;
            this.conditionName = conditionName;
        }

        /**
         * Returns the condition id as declared in the rule definition.
         *
         * @return condition id; may be {@code null} for internally generated nodes
         */
        public String getConditionId() {
            return conditionId;
        }

        /**
         * Returns the human-readable condition name as declared in the rule definition.
         *
         * @return condition name; may be {@code null} if not set
         */
        public String getConditionName() {
            return conditionName;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return Objects.equals(conditionId, item.conditionId) && Objects.equals(conditionName, item.conditionName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conditionId, conditionName);
        }
    }
}
