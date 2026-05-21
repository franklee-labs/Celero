package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Condition;
import labs.franklee.celero.rules.ConditionFactoryRegistry;
import labs.franklee.celero.rules.ConditionNode;

/**
 * SPI for creating {@link Condition} instances from a parsed {@link ConditionNode}.
 *
 * <p>Implement this interface to add support for a custom condition type and register
 * it via {@link ConditionFactoryRegistry#registerGlobalFactory} (available to all rules)
 * or {@link ConditionFactoryRegistry#registerFactory} (scoped to a specific rule).
 *
 * <p>Example — a factory for a custom "STARTS_WITH" condition:
 * <pre>{@code
 * public class StartsWithConditionFactory implements ConditionFactory {
 *     @Override
 *     public Condition create(ConditionNode node) {
 *         String field = (String) node.getProperties().get("field");
 *         String prefix = (String) node.getProperties().get("value");
 *         return new StartsWithCondition(field, prefix);
 *     }
 * }
 *
 * // Register once at startup:
 * ConditionFactoryRegistry.registerGlobalFactory("STARTS_WITH", new StartsWithConditionFactory());
 * }</pre>
 */
public interface ConditionFactory {

    /**
     * Creates a {@link Condition} from the given node descriptor.
     *
     * <p>The factory is responsible for reading any required parameters from
     * {@link ConditionNode#getProperties()} and constructing the appropriate
     * {@link Condition} implementation. The engine will call {@link Condition#build()}
     * on the returned instance immediately after this method returns.
     *
     * @param conditionNode the parsed node carrying the condition's sign, id, name,
     *                      and properties; never {@code null}
     * @return a new, un-built {@link Condition} instance; never {@code null}
     */
    Condition create(ConditionNode conditionNode);
}
