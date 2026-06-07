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
package de.tudarmstadt.ukp.clarin.webanno.api.type;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

public class CasMetadataSupportTest
{
    @Test
    public void thatIdentityIsReadFromFullCasMetadata() throws Exception
    {
        var tsd = createTypeSystemDescription(
                "de/tudarmstadt/ukp/clarin/webanno/api/type/webanno-internal");
        var cas = CasCreationUtils.createCas(tsd, null, null);

        var cmd = new CASMetadata(cas.getJCas());
        cmd.setProjectName("project");
        cmd.setSourceDocumentName("document");
        cmd.addToIndexes();

        assertThat(CasMetadataSupport.getProjectName(cas)).isEqualTo("project");
        assertThat(CasMetadataSupport.getSourceDocumentName(cas)).isEqualTo("document");
    }

    @Test
    public void thatReadingToleratesOlderCasMetadataWithoutIdentityFeatures() throws Exception
    {
        // An older type system may declare the CASMetadata type without the identity features.
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var type = tsd.addType(CASMetadata.class.getName(), "", CAS.TYPE_NAME_ANNOTATION);
        type.addFeature("username", "", CAS.TYPE_NAME_STRING);

        var cas = CasCreationUtils.createCas(tsd, null, null);
        var casMetadataType = cas.getTypeSystem().getType(CASMetadata.class.getName());
        var cmd = cas.createAnnotation(casMetadataType, 0, 0);
        cas.addFsToIndexes(cmd);

        // Reading via the generated accessor would throw because the feature is missing ...
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> cas.select(CASMetadata.class).get(0).getProjectName());

        // ... but the support helper tolerates it and leaves the identity unset.
        assertThat(CasMetadataSupport.getProjectName(cas)).isNull();
        assertThat(CasMetadataSupport.getSourceDocumentName(cas)).isNull();
    }

    @Test
    public void thatReadingToleratesMissingCasMetadataType() throws Exception
    {
        var cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);

        assertThat(CasMetadataSupport.getProjectName(cas)).isNull();
        assertThat(CasMetadataSupport.getSourceDocumentName(cas)).isNull();
    }
}
