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
package de.tudarmstadt.ukp.clarin.webanno.agreement.config;

import org.springframework.context.annotation.Bean;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.cohenkappa.CohenKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.fleisskappa.FleissKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha.KrippendorffAlphaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing.KrippendorffAlphaUnitizingAgreementMeasureSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class AgreementAutoConfiguration
{
    @Bean
    public CohenKappaAgreementMeasureSupport cohenKappaAgreementMeasureSupport(
            AnnotationSchemaService aAnnotationService)
    {
        return new CohenKappaAgreementMeasureSupport(aAnnotationService);
    }

    @Bean
    public FleissKappaAgreementMeasureSupport fleissKappaAgreementMeasureSupport(
            AnnotationSchemaService aAnnotationService)
    {
        return new FleissKappaAgreementMeasureSupport(aAnnotationService);
    }

    @Bean
    public KrippendorffAlphaAgreementMeasureSupport KrippendorffAlphaAgreementMeasureSupport(
            AnnotationSchemaService aAnnotationService)
    {
        return new KrippendorffAlphaAgreementMeasureSupport(aAnnotationService);
    }

    @Bean
    public KrippendorffAlphaUnitizingAgreementMeasureSupport krippendorffAlphaUnitizingAgreementMeasureSupport()
    {
        return new KrippendorffAlphaUnitizingAgreementMeasureSupport();
    }
}
