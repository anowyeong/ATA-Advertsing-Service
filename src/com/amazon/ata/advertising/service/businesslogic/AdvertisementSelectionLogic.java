package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.dao.TargetingGroupDao;
import com.amazon.ata.advertising.service.model.*;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.w3c.dom.css.CSSImportRule;

import java.lang.annotation.Target;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    public static final boolean IMPLEMENTED_STREAMS = true;


    private TargetingEvaluator targetingEvaluator;

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;
    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */
    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {
        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();

        if (StringUtils.isEmpty(marketplaceId)) {
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
        } else {

            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
            List<AdvertisementContent> resultContent = new ArrayList<>();

            RequestContext requestContext = new RequestContext(customerId, marketplaceId);
            targetingEvaluator = new TargetingEvaluator(requestContext);

            List<TargetingGroup> targetingGroups;

            // Convert this to using stream below
//            if (contents != null && contents.size() > 0) { x
//                for (AdvertisementContent content : contents) { x
//                    targetingGroups = targetingGroupDao.get(content.getContentId()); x
//
//                    if (targetingGroups != null) { x
//                        for (TargetingGroup group : targetingGroups) {
//                            if (targetingEvaluator.evaluate(group) == TargetingPredicateResult.TRUE) {
//                                resultContent.add(content);
//                            }
//                        }
//                    }
//                }
//            }
//            if (contents != null && contents.size() > 0 ) {
//                resultContent = contents.stream()
//                        .filter(Objects::nonNull)
//                        .filter(content -> {
//                            return targetingGroupDao.get(content.getContentId()).stream()
//                                    .filter(Objects::nonNull)
//                                    .anyMatch(group -> targetingEvaluator.evaluate(group) == TargetingPredicateResult.TRUE);
//                        })
//                        .collect(Collectors.toList());
//            }
//
//            if (requestContext != null && resultContent.size() > 0) {
//                AdvertisementContent advertisementContent = resultContent.get(random.nextInt(resultContent.size()));
//                generatedAdvertisement = new GeneratedAdvertisement(advertisementContent);
//
//            }

            TreeMap<TargetingGroup, AdvertisementContent> treeMap = new TreeMap<>(new SortByClickRate());

            if (contents != null && contents.size() > 0) {
                for (AdvertisementContent content : contents) {
                    targetingGroups = targetingGroupDao.get(content.getContentId());

                    if (targetingGroups != null) {
                        for (TargetingGroup group : targetingGroups) {
                            if (targetingEvaluator.evaluate(group) == TargetingPredicateResult.TRUE) {
                                treeMap.put(group, content);
                                break;
                            }
                        }
                    }
                }
            }

            if (requestContext != null && !treeMap.isEmpty()) {
                AdvertisementContent advertisementContent = treeMap.lastEntry().getValue();
                generatedAdvertisement = new GeneratedAdvertisement(advertisementContent);
            }
        }
            return generatedAdvertisement;
    }

    class SortByClickRate implements Comparator<TargetingGroup> {

        @Override
        public int compare(TargetingGroup targetingGroup, TargetingGroup t1) {
            if (targetingGroup.getClickThroughRate() < t1.getClickThroughRate()) {
                return -1;
            } else if (targetingGroup.getClickThroughRate() > t1.getClickThroughRate()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
