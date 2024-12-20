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

import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

@Component
public class AgreementMeasureSupportRegistryImpl
    implements AgreementMeasureSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<AgreementMeasureSupport<?, ?, ?>> agreementMeasuresProxy;

    private List<AgreementMeasureSupport<?, ?, ?>> agreementMeasures;

    public AgreementMeasureSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<AgreementMeasureSupport<?, ?, ?>> aFeatureSupports)
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
        List<AgreementMeasureSupport<?, ?, ?>> fsp = new ArrayList<>();

        if (agreementMeasuresProxy != null) {
            fsp.addAll(agreementMeasuresProxy);
            AnnotationAwareOrderComparator.sort(fsp);

            for (AgreementMeasureSupport fs : fsp) {
                log.debug("Found agreement measure support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] agreement measure supports", fsp.size());

        agreementMeasures = unmodifiableList(fsp);
    }

    @Override
    public List<AgreementMeasureSupport<?, ?, ?>> getAgreementMeasureSupports()
    {
        return agreementMeasures;
    }

    @Override
    public AgreementMeasureSupport getAgreementMeasureSupport(String aId)
    {
        return getAgreementMeasureSupports().stream() //
                .filter(fs -> fs.getId().equals(aId)) //
                .findFirst() //
                .orElse(null);
    }

    @Override
    public List<AgreementMeasureSupport<?, ?, ?>> getAgreementMeasureSupports(
            AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return agreementMeasures.stream() //
                .filter(factory -> factory.accepts(aLayer, aFeature)) //
                .sorted(comparing(AgreementMeasureSupport::getName)) //
                .collect(toList());
    }

    @Override
    public AgreementMeasure getMeasure(AnnotationLayer aLayer, AnnotationFeature aFeature,
            String aMeasure, DefaultAgreementTraits traits)
    {
        var ams = getAgreementMeasureSupport(aMeasure);
        var measure = ams.createMeasure(aLayer, aFeature, traits);
        return measure;
    }
}
