package labs.franklee.celero.listener;

@FunctionalInterface
public interface AdvancedConditionListener {

    void onResult(AdvancedConditionEvent event);

    default int order() {
        return 0;
    }
}
