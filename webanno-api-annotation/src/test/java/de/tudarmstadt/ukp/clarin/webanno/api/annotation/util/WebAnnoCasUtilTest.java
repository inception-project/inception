/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOCUMENT_ANNOTATION;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class WebAnnoCasUtilTest
{
    @Test
    public void thatCreateDocumentMetadataUpgradesExistingDocumentAnnotation() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription();
        
        CAS cas = getRealCas(createCas(tsd));
        
        assertThat(cas.select(DocumentAnnotation.class).asList())
                .as("CAS has no DocumentAnnotation")
                .isEmpty();
        
        cas.setDocumentLanguage("en");
        
        assertThat(cas.select(DocumentAnnotation.class).asList())
                .as("CAS initialized with DocumentAnnotation")
                .extracting(fs -> fs.getType().getName())
                .containsExactly(TYPE_NAME_DOCUMENT_ANNOTATION);
        assertThat(cas.select(DocumentAnnotation.class).asList())
                .as("Language has been set")
                .extracting(DocumentAnnotation::getLanguage)
                .containsExactly("en");

        WebAnnoCasUtil.createDocumentMetadata(cas);

        assertThat(cas.select(DocumentAnnotation.class).asList())
                .as("DocumentAnnotation has been upgraded to DocumentMetaData")
                .extracting(fs -> fs.getType().getName())
                .containsExactly(DocumentMetaData.class.getName());
        assertThat(cas.select(DocumentAnnotation.class).asList())
                .as("Language survived upgrade")
                .extracting(DocumentAnnotation::getLanguage)
                .containsExactly("en");
    }
}
