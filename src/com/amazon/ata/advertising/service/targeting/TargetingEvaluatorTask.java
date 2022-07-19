package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.List;

public class TargetingEvaluatorTask implements Runnable{

    private TargetingEvaluator evaluator;
    private TargetingPredicate targetingPredicate;
    private RequestContext requestContext;

    public TargetingEvaluatorTask(TargetingEvaluator evaluator, TargetingPredicate targetingPredicate, RequestContext requestContext) {
        this.evaluator = evaluator;
        this.targetingPredicate = targetingPredicate;
        this.requestContext = requestContext;
    }

    @Override
    public void run() {
        TargetingPredicateResult result = targetingPredicate.evaluate(requestContext);
        if (result != null) {
            evaluator.addToList(result);
        }

    }
}
