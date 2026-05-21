package labs.franklee.celero.rules;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.rules.internal.*;

import java.util.*;

/**
 * Global registry that maps condition signs to their {@link ConditionFactory} implementations.
 *
 * <p>The following signs are built in and available to all rules:
 *
 * <table>
 *   <tr><th>Sign</th><th>Semantics</th></tr>
 *   <tr><td>EQ</td><td>field == value</td></tr>
 *   <tr><td>NEQ</td><td>field != value</td></tr>
 *   <tr><td>GT / GTE / LT / LTE</td><td>numeric comparison</td></tr>
 *   <tr><td>IN</td><td>field value is in a list</td></tr>
 *   <tr><td>NIN</td><td>field value is not in a list</td></tr>
 *   <tr><td>REGEXP</td><td>field matches a regular expression</td></tr>
 *   <tr><td>CEL</td><td>arbitrary CEL expression</td></tr>
 *   <tr><td>INTERSECT</td><td>two lists share at least one element</td></tr>
 *   <tr><td>DISJOINT</td><td>two lists share no elements</td></tr>
 *   <tr><td>EXISTS</td><td>field is present in the context</td></tr>
 *   <tr><td>ABSENT</td><td>field is absent from the context</td></tr>
 * </table>
 *
 * <p>Custom signs can be added globally via {@link #registerGlobalFactory} or scoped to
 * a specific rule via {@link #registerFactory}. Sign matching is case-insensitive.
 *
 * <p>All methods are static; this class is not instantiated.
 */
public class ConditionFactoryRegistry {

    private final static Map<String, ConditionFactory> globalFactories = new HashMap<>();
    private final static Map<String, Map<String, ConditionFactory>> ruleFactories = new HashMap<>();
    private final static Set<String> internalSigns = new HashSet<>();

    static {
        globalFactories.put("EQ",        new EqualConditionFactory());
        globalFactories.put("NEQ",       new NotEqualConditionFactory());
        globalFactories.put("GT",        new CompareConditionFactory());
        globalFactories.put("GTE",       new CompareConditionFactory());
        globalFactories.put("LT",        new CompareConditionFactory());
        globalFactories.put("LTE",       new CompareConditionFactory());
        globalFactories.put("IN",        new InConditionFactory());
        globalFactories.put("NIN",       new NotInConditionFactory());
        globalFactories.put("REGEXP",    new RegexConditionFactory());
        globalFactories.put("CEL",       new CelConditionFactory());
        globalFactories.put("INTERSECT", new IntersectConditionFactory());
        globalFactories.put("DISJOINT",  new DisjointConditionFactory());
        globalFactories.put("EXISTS",    new ExistsConditionFactory());
        globalFactories.put("ABSENT",    new AbsentConditionFactory());

        internalSigns.addAll(globalFactories.keySet());
        internalSigns.addAll(Set.of("AND", "OR", "NOT"));
    }

    /**
     * Registers a custom {@link ConditionFactory} under the given sign for all rules.
     *
     * <p>The sign is normalized to upper-case before registration. It must not clash
     * with any built-in sign (including relation signs {@code AND}, {@code OR},
     * {@code NOT}) and must not already be registered globally.
     *
     * <p>This method is not thread-safe. Register custom factories once at application
     * startup, before any rules are built or evaluated.
     *
     * @param sign    the condition sign string (e.g. {@code "MY_CUSTOM"}); must not be {@code null}
     * @param factory the factory that creates condition instances for this sign; must not be {@code null}
     * @throws InvalidRuleNodeException if {@code sign} is {@code null}, conflicts with a
     *                                  reserved sign, or is already registered globally
     */
    public static void registerGlobalFactory(String sign, ConditionFactory factory) throws InvalidRuleNodeException {
        if (null == sign) {
            throw new InvalidRuleNodeException("sign must not be null");
        }
        sign = sign.toUpperCase();
        if (internalSigns.contains(sign)) {
            throw new InvalidRuleNodeException("sign must not be [" + sign + "]");
        }
        if (globalFactories.containsKey(sign)) {
            throw new InvalidRuleNodeException("duplicated sign [" + sign + "]");
        }
        globalFactories.put(sign, factory);
    }

    /**
     * Registers a custom {@link ConditionFactory} under the given sign for a specific rule only.
     *
     * <p>A rule-scoped factory takes precedence over a global factory with the same sign
     * when the engine looks up the factory for that rule. This allows per-rule overrides
     * without affecting other rules.
     *
     * <p>The sign must not clash with any built-in or relation sign. It may duplicate a
     * globally registered sign (the rule-scoped one will shadow it).
     *
     * <p>This method is not thread-safe. Register custom factories once at startup.
     *
     * @param ruleId  the id of the rule this factory applies to; must not be {@code null}
     * @param sign    the condition sign string; must not be {@code null}
     * @param factory the factory that creates condition instances for this sign; must not be {@code null}
     * @throws InvalidRuleNodeException if {@code sign} is {@code null}, conflicts with a
     *                                  reserved sign, or is already registered for this rule
     */
    public static void registerFactory(String ruleId, String sign, ConditionFactory factory) throws InvalidRuleNodeException {
        if (null == sign) {
            throw new InvalidRuleNodeException("sign must not be null");
        }
        sign = sign.toUpperCase();
        if (internalSigns.contains(sign)) {
            throw new InvalidRuleNodeException("sign must not be [" + sign + "]");
        }
        Map<String, ConditionFactory> map = ruleFactories.getOrDefault(ruleId, new HashMap<>());
        if (map.containsKey(sign)) {
            throw new InvalidRuleNodeException("duplicated sign [" + sign + "] for rule [" + ruleId + "]");
        }
        map.put(sign, factory);
        ruleFactories.put(ruleId, map);
    }

    /**
     * Looks up the {@link ConditionFactory} for the given rule and sign.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Rule-scoped factory registered via {@link #registerFactory}</li>
     *   <li>Global factory registered via {@link #registerGlobalFactory} or built-in</li>
     * </ol>
     *
     * @param ruleId the id of the rule being built; used for rule-scoped lookup
     * @param sign   the condition sign string; case-insensitive
     * @return the matching {@link ConditionFactory}, or {@code null} if none is found
     */
    public static ConditionFactory getConditionFactory(String ruleId, String sign) {
        if (null == sign) {
            return null;
        }
        sign = sign.toUpperCase();
        Map<String, ConditionFactory> map = ruleFactories.getOrDefault(ruleId, new HashMap<>());
        ConditionFactory factory = map.getOrDefault(sign, null);
        if (null != factory) {
            return factory;
        }
        return globalFactories.getOrDefault(sign, null);
    }
}
