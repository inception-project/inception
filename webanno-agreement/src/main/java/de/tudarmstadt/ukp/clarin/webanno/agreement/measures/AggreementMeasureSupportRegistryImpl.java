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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

@Component
public class AggreementMeasureSupportRegistryImpl
    implements AggreementMeasureSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<AggreementMeasureSupport> agreementMeasuresProxy;

    private List<AggreementMeasureSupport> agreementMeasures;

    public AggreementMeasureSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<AggreementMeasureSupport> aFeatureSupports)
    {
        agreementMeasuresProxy = aFeatureSupports;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    public void init()
    {
        List<AggreementMeasureSupport> fsp = new ArrayList<>();

        if (agreementMeasuresProxy != null) {
            fsp.addAll(agreementMeasuresProxy);
            AnnotationAwareOrderComparator.sort(fsp);

            for (AggreementMeasureSupport fs : fsp) {
                log.info("Found agreement measure support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        agreementMeasures = Collections.unmodifiableList(fsp);
    }

    @Override
    public List<AggreementMeasureSupport> getAgreementMeasureSupports()
    {
        return agreementMeasures;
    }

    @Override
    public AggreementMeasureSupport getAgreementMeasureSupport(String aId)
    {
        return getAgreementMeasureSupports().stream().filter(fs -> fs.getId().equals(aId))
                .findFirst().orElse(null);
    }

    @Override
    public List<AggreementMeasureSupport> getAgreementMeasureSupports(AnnotationFeature aFeature)
    {
        return agreementMeasures.stream().filter(factory -> factory.accepts(aFeature))
                .sorted(Comparator.comparing(AggreementMeasureSupport::getName))
                .collect(Collectors.toList());
    }
}
