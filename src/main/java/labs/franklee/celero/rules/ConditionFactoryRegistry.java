package labs.franklee.celero.rules;

import labs.franklee.celero.exceptions.InvalidRuleNodeException;
import labs.franklee.celero.rules.internal.*;

import java.util.*;

public class ConditionFactoryRegistry {

    private final static Map<String, ConditionFactory> globalFactories = new HashMap<>();
    private final static Map<String, Map<String, ConditionFactory>> ruleFactories = new HashMap<>();
    private final static Set<String> internalSigns = new HashSet<>();

    static {
        globalFactories.put("EQ",      new EqualConditionFactory());
        globalFactories.put("NEQ",     new NotEqualConditionFactory());
        globalFactories.put("GT",      new GreaterThanConditionFactory());
        globalFactories.put("GTE",     new GreaterThanOrEqualConditionFactory());
        globalFactories.put("LT",      new LessThanConditionFactory());
        globalFactories.put("LTE",     new LessThanOrEqualConditionFactory());
        globalFactories.put("IN",      new InConditionFactory());
        globalFactories.put("NIN",  new NotInConditionFactory());
        globalFactories.put("REGEX",   new RegexConditionFactory());
        globalFactories.put("CEL",     new CelConditionFactory());
        globalFactories.put("INTERSECT", new IntersectConditionFactory());
        globalFactories.put("DISJOINT", new DisjointConditionFactory());
        globalFactories.put("EXISTS", new ExistsConditionFactory());
        globalFactories.put("ABSENT", new AbsentConditionFactory());

        internalSigns.addAll(globalFactories.keySet());
        internalSigns.addAll(Set.of("AND", "OR", "NOT"));
    }

    public static void registerGlobalFactory(String sign, ConditionFactory factory) throws InvalidRuleNodeException {
        if (null == sign) {
            throw new InvalidRuleNodeException("sign must not be null");
        }
        sign = sign.toUpperCase();
        if (internalSigns.contains(sign)) {
            throw new InvalidRuleNodeException("sign must not be [" + sign + "]");
        }
        if (globalFactories.containsKey(sign)) { // duplicate registered
            throw new InvalidRuleNodeException("duplicated sign [" + sign + "]");
        }
        globalFactories.put(sign, factory);
    }

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
