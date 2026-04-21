package labs.franklee.celero.logic.base;

public interface Negatable<T extends Node> {

    T negate() throws Exception;
}
