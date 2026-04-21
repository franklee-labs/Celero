package labs.franklee.celero.context;

import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.exceptions.InvalidParameterException;
import labs.franklee.celero.logic.base.EvalResult;

import java.util.HashMap;
import java.util.Map;

public class Context {

    public static class Builder {
        private final RuleContext ruleContext;
        private boolean enableMissState = false;
        private boolean conditionResultCacheable = false; // whether this rule enables condition-result cache
        private boolean enableReport = false;

        private Builder(RuleContext ruleContext) {
            this.ruleContext = ruleContext;
        }

        public static Builder createBuilder(RuleContext ruleContext) {
            Builder b = new Builder(ruleContext);
            if (ruleContext.isEnableReports()) {
                b.enableReport();
            }
            return b;
        }

        public Builder enableMissState() {
            this.enableMissState = true;
            return this;
        }

        public Builder enableConditionResultCache() {
            this.conditionResultCacheable = true;
            return this;
        }

        public Builder enableReport() {
            this.enableReport = true;
            return this;
        }

        public Context build() {
            Context context = new Context(this.ruleContext);
            context.enableMissing = this.enableMissState;
            context.enableConditionResultCache = this.conditionResultCacheable;
            context.enableReport = this.enableReport;
            return context;
        }
    }

    private final RuleContext ruleContext;

    private final Map<String, Object> params;

    private Map<String, Object> evalParams;

    private boolean enableMissing;

    private boolean enableConditionResultCache;  // whether this rule enables condition-result cache

    private boolean enableReport;

    private Context(RuleContext ruleContext) {
        this.ruleContext = ruleContext;
        ruleContext.getParams().keySet().forEach(k -> {
            if (k.startsWith("_")) {
                throw new InvalidParameterException("param keys can not start with _ [key=" + k + "]");
            }
        });
        this.params = ruleContext.getParams();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void buildEvalParams(Map<String, Object> builtinParam) {
        Map<String, Object> map = new HashMap<>(this.params);
        if (null != builtinParam) {
            builtinParam.keySet().forEach(k -> {
                if (null == k) {
                    throw new InvalidParameterException("builtin keys must not be null");
                }
                if (!k.startsWith("_")) {
                    throw new InvalidParameterException("builtin keys must start with _ [key=" + k + "]");
                }
            });
            map.putAll(builtinParam);
        }
        this.evalParams = map;
    }

    public RuleContext getRuleContext() {
        return ruleContext;
    }

    public Map<String, Object> getEvalParam() {
        return this.evalParams;
    }

    public boolean isEnableMissing() {
        return enableMissing;
    }

    public boolean isEnableConditionResultCache() {
        return enableConditionResultCache;
    }

    public boolean isEnableReport() {
        return enableReport;
    }

    private final Map<String, EvalResult> conditionEvalResult = new HashMap<>();

    public void setConditionEvalResult(String conditionUniqueId, EvalResult result) {
        this.conditionEvalResult.put(conditionUniqueId, result);
    }

    public EvalResult getConditionEvalResult(String conditionUniqueId) {
        return this.conditionEvalResult.getOrDefault(conditionUniqueId, null);
    }
}
