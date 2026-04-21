package labs.franklee.celero.logic.base;

public class Validation {

    private boolean valid;

    private String message;

    public final static Validation VALID = new Validation(true, null);

    public Validation(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
