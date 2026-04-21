package labs.franklee.celero.listener;

@FunctionalInterface
public interface ConditionListener {

    void onResult(ConditionEvent event);

    default int order() {
        return 0;
    }
}
