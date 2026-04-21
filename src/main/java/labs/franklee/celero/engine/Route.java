package labs.franklee.celero.engine;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    public Set<Item> getMatched() {
        return matched;
    }

    public Set<Item> getUnmatched() {
        return unmatched;
    }

    public Set<Item> getAbsent() {
        return absent;
    }

    public Set<Item> getSkipped() {
        return skipped;
    }

    public static class Item {
        private String conditionId;
        private String conditionName;

        public Item(String conditionId, String conditionName) {
            this.conditionId = conditionId;
            this.conditionName = conditionName;
        }

        public String getConditionId() {
            return conditionId;
        }

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
