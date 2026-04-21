package labs.franklee.celero.logic.base;

public final class EvalResult {

    public static final EvalResult TRUE  = new EvalResult(State.TRUE);
    public static final EvalResult FALSE = new EvalResult(State.FALSE);
    public static final EvalResult INDETERMINATE = new EvalResult(State.INDETERMINATE);

    private EvalResult(State state) {
        this.state = state;
    }

    public enum State { TRUE, FALSE, INDETERMINATE}

    private final State state;

    public boolean isTrue()    { return state == State.TRUE; }
    public boolean isFalse()   { return state == State.FALSE; }
    public boolean isIndeterminate() { return state == State.INDETERMINATE; }
}