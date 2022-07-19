package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;
    private  List<TargetingPredicateResult> results ;

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
//        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();
//        boolean allTruePredicates = true;
//        for (TargetingPredicate predicate : targetingPredicates) {
//            TargetingPredicateResult predicateResult = predicate.evaluate(requestContext);
//            if (!predicateResult.isTrue()) {
//                allTruePredicates = false;
//                break;
//            }
//        }
//
//        return allTruePredicates ? TargetingPredicateResult.TRUE :
//                                   TargetingPredicateResult.FALSE;
//    }

        if (targetingGroup.getTargetingPredicates() == null) return TargetingPredicateResult.TRUE;

        ExecutorService executorService = Executors.newCachedThreadPool();
        results = Collections.synchronizedList(new ArrayList<>());

//        TargetingPredicateResult result = targetingGroup.getTargetingPredicates().stream()
//                .map(predicate -> predicate.evaluate(requestContext))
//                .filter(x -> !x.isTrue())
//                .findFirst().orElse(TargetingPredicateResult.TRUE);
//        return !result.isTrue() ? TargetingPredicateResult.FALSE : TargetingPredicateResult.TRUE;


        targetingGroup.getTargetingPredicates().stream()
                .forEach(predicate -> executorService.submit(new TargetingEvaluatorTask(this, predicate, requestContext)));

        // make thread sleep or can potentially return an empty results array.
        try {
            Thread.sleep((long) 2.5);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        executorService.shutdown();

        if (results != null && results.isEmpty()) {
            return TargetingPredicateResult.FALSE;
        }
        return results.get(0);

    }

    public synchronized void addToList(TargetingPredicateResult result) {

        List<TargetingPredicateResult> predicateList = Collections.synchronizedList(new ArrayList<>(results));
        predicateList.add(result);
        results = predicateList;
    }

}
