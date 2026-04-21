package labs.franklee.celero.exceptions;

public class InvalidRuleNodeException extends Exception {

    public InvalidRuleNodeException(String message) {
        super(message);
    }

    public InvalidRuleNodeException(Throwable e) {
        super(e);
    }
}
