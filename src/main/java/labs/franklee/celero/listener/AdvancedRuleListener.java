package labs.franklee.celero.listener;

@FunctionalInterface
public interface AdvancedRuleListener {

    void onRuleResult(AdvancedRuleEvent event);

    default int order() {
        return 0;
    }
}
