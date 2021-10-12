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

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;

public class MergeIncompleteStrategy
    implements MergeStrategy
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BEAN_NAME = "incompleteAgreementNonStacked";

    @Override
    public Optional<Configuration> chooseConfigurationToMerge(DiffResult aDiff,
            ConfigurationSet aCfgs)
    {
        boolean stacked = aCfgs.getConfigurations().stream() //
                .filter(Configuration::isStacked) //
                .findAny().isPresent();

        if (stacked) {
            LOG.trace(" `-> Not merging stacked annotation");
            return Optional.empty();
        }

        if (!aDiff.isAgreement(aCfgs)) {
            LOG.trace(" `-> Not merging annotation with disagreement");
            return Optional.empty();
        }

        return Optional.of(aCfgs.getConfigurations().get(0));
    }
}
