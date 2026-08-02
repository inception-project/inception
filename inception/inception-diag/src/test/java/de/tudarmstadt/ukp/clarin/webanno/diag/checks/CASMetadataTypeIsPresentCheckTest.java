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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

class CASMetadataTypeIsPresentCheckTest
{
    CASMetadataTypeIsPresentCheck sut;
    SourceDocument document;
    String dataOwner;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new CASMetadataTypeIsPresentCheck();
        document = SourceDocument.builder().build();
    }

    @Test
    void thatMissingCasMetadataTypeIsReported() throws Exception
    {
        // A CAS serialized with a type system predating the introduction of CASMetadata does not
        // declare the type at all.
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var cas = CasCreationUtils.createCas(tsd, null, null);

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, cas, messages);

        assertThat(result).isTrue();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(messages.get(0).getMessage()).contains("needs upgrade to support CASMetadata");
    }

    @Test
    void thatOutdatedCasMetadataTypeIsReported() throws Exception
    {
        // An older type system may declare a CASMetadata type that predates the addition of some
        // features - most importantly lastChangedOnDisk, without which concurrent modification
        // detection silently stops working.
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var type = tsd.addType(CASMetadata._TypeName, "", CAS.TYPE_NAME_ANNOTATION);
        type.addFeature(CASMetadata._FeatName_username, "", CAS.TYPE_NAME_STRING);

        var cas = CasCreationUtils.createCas(tsd, null, null);
        var casMetadataType = cas.getTypeSystem().getType(CASMetadata._TypeName);
        cas.addFsToIndexes(cas.createAnnotation(casMetadataType, 0, 0));

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, cas, messages);

        assertThat(result).isTrue();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(messages.get(0).getMessage()) //
                .contains("needs upgrade to bring CASMetadata up-to-date") //
                .contains(CASMetadata._FeatName_lastChangedOnDisk) //
                .contains(CASMetadata._FeatName_projectId) //
                .doesNotContain("[" + CASMetadata._FeatName_username + "]");
    }

    @Test
    void thatMissingCasMetadataInstanceIsReported() throws Exception
    {
        // The type is up-to-date, but the CAS carries no CASMetadata instance.
        var cas = makeCasWithCurrentTypeSystem();

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, cas, messages);

        assertThat(result).isTrue();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(messages.get(0).getMessage()).contains("contains no CASMetadata");
    }

    @Test
    void thatUpToDateCasMetadataIsNotReported() throws Exception
    {
        var cas = makeCasWithCurrentTypeSystem();
        var casMetadataType = cas.getTypeSystem().getType(CASMetadata._TypeName);
        cas.addFsToIndexes(cas.createAnnotation(casMetadataType, 0, 0));

        var messages = new ArrayList<LogMessage>();

        var result = sut.check(document, dataOwner, cas, messages);

        assertThat(result).isTrue();
        assertThat(messages).isEmpty();
    }

    private CAS makeCasWithCurrentTypeSystem() throws Exception
    {
        var tsd = createTypeSystemDescription(
                "de/tudarmstadt/ukp/clarin/webanno/api/type/webanno-internal");
        return CasCreationUtils.createCas(tsd, null, null);
    }
}
