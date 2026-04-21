package labs.franklee.celero.engine;

import labs.franklee.celero.context.Context;

abstract class AbstractCeleroEngine {

    protected Context buildContext(RuleContext ruleContext, boolean enableConditionResultCache, boolean enableMissing) {
        Context.Builder builder = Context.Builder.createBuilder(ruleContext);
        if (enableConditionResultCache) {
            builder.enableConditionResultCache();
        }
        if (enableMissing) {
            builder.enableMissState();
        }
        return builder.build();
    }

}
