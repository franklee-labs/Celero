package labs.franklee.celero.listener;

@FunctionalInterface
public interface RuleListener {

    void onRuleResult(RuleEvent event);

    default int order() {
        return 0;
    }
}
