/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation.merge.strategy;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.curation.merge.CasMerge;

public class ThresholdBasedMergeStrategy
    implements MergeStrategy
{
    private static final Logger LOG = LoggerFactory.getLogger(CasMerge.class);

    /**
     * Number of users who have to annotate an item for it to be considered. If less than the given
     * number of users have voted, the item is considered <i>Incomplete</i>.
     */
    private final int userThreshold;

    /**
     * Percentage of annotations the majority vote has to have over the second-most voted. If it is
     * set to 0, only items with a tie are considered <i>Disputed</i>. If this is set to 1, any item
     * which has not been voted for unanimously, is considered <i>Disputed</i>. A value of 0.5
     * indicates that an item is <i>Disputed</i> if one the most vote has not at least twice as many
     * votes as the second-most voted.<br />
     * Confidence is computed as abs(correct-wrong)/max(correct,wrong).
     */
    private final double confidenceThreshold;

    private final int topRanks;

    private ThresholdBasedMergeStrategy(Builder builder)
    {
        this.userThreshold = builder.userThreshold;
        this.confidenceThreshold = builder.confidenceThreshold;
        this.topRanks = builder.topRanks;
    }

    @Override
    public List<Configuration> chooseConfigurationsToMerge(DiffResult aDiff, ConfigurationSet aCfgs,
            AnnotationLayer aLayer)
    {
        var topRanksToConsider = aLayer.isAllowStacking() ? topRanks : 1;
        if (topRanksToConsider == 0) {
            topRanksToConsider = Integer.MAX_VALUE;
        }

        var cfgsAboveUserThreshold = aCfgs.getConfigurations().stream() //
                .filter(cfg -> cfg.getCasGroupIds().size() >= userThreshold) //
                .sorted(comparing((Configuration cfg) -> cfg.getCasGroupIds().size()).reversed()) //
                .toList();

        if (cfgsAboveUserThreshold.isEmpty()) {
            LOG.trace(" `-> Not merging as no configuration meets the user threshold [{}] ({})",
                    userThreshold, getClass().getSimpleName());
            return emptyList();
        }

        var totalAnnotators = cfgsAboveUserThreshold.stream() //
                .flatMap(cfg -> cfg.getCasGroupIds().stream()) //
                .distinct() //
                .count();

        var totalVotes = cfgsAboveUserThreshold.stream() //
                .mapToDouble(cfg -> cfg.getCasGroupIds().size()) //
                .sum();

        var cutOffVotesByConfidence = confidenceThreshold * totalVotes;
        var cutOffVotesByRank = cfgsAboveUserThreshold.get(
                (topRanksToConsider - 1) < cfgsAboveUserThreshold.size() ? (topRanksToConsider - 1)
                        : cfgsAboveUserThreshold.size() - 1)
                .getCasGroupIds().size();

        var result = cfgsAboveUserThreshold.stream() //
                .filter(cfg -> cfg.getCasGroupIds().size() >= cutOffVotesByConfidence)
                .filter(cfg -> cfg.getCasGroupIds().size() >= cutOffVotesByRank) //
                .collect(toList());

        if (topRanksToConsider == 1 && result.size() > 1) {
            // If we request only one result but there is more than one, then it is a tie. If only
            // a single result is requested, then ties are considered a dispute.
            return emptyList();
        }

        return result;

        // Configuration best = cfgsAboveUserThreshold.get(0);
        // Optional<Configuration> secondBest = Optional.empty();
        // if (cfgsAboveUserThreshold.size() > 1) {
        // secondBest = Optional.of(cfgsAboveUserThreshold.get(1));
        // }
        //
        // double bestVoteCount = best.getCasGroupIds().size();
        // double secondBestVoteCount = secondBest.map(cfg ->
        // cfg.getCasGroupIds().size()).orElse(0);
        // double confidence = ((bestVoteCount - secondBestVoteCount) / bestVoteCount);
        //
        // if (confidence > 0.0 && confidence >= confidenceThreshold) {
        // return asList(best);
        // }
        //
        // // DISPUTED
        // LOG.trace(" `-> Not merging as confidence [{}] is zero or does not meet the threshold
        // [{}]",
        // confidence, confidenceThreshold);
        // return emptyList();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE) //
                .append("userThreshold", userThreshold)//
                .append("confidenceThreshold", confidenceThreshold).toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private int userThreshold;
        private double confidenceThreshold;
        private int topRanks;

        private Builder()
        {
        }

        public Builder withUserThreshold(int aUserThreshold)
        {
            userThreshold = aUserThreshold;
            return this;
        }

        public Builder withConfidenceThreshold(double aConfidenceThreshold)
        {
            confidenceThreshold = aConfidenceThreshold;
            return this;
        }

        public Builder withTopRanks(int aTopRanks)
        {
            topRanks = aTopRanks;
            return this;
        }

        public ThresholdBasedMergeStrategy build()
        {
            return new ThresholdBasedMergeStrategy(this);
        }
    }
}
