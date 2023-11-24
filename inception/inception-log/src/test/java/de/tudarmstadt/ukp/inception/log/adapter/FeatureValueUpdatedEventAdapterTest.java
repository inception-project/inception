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
package de.tudarmstadt.ukp.inception.log.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toPrettyJsonString;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.log.exporter.ExportedLoggedEvent;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

public class FeatureValueUpdatedEventAdapterTest
{
    @Test
    public void thatJsonRepresentationIsComplete() throws Exception
    {
        TypeSystemDescription tsd = new TypeSystemDescription_impl();
        TypeDescription typeDesc = tsd.addType("customType", "desc", TYPE_NAME_ANNOTATION);
        FeatureDescription featDesc = typeDesc.addFeature("value", "desc", TYPE_NAME_STRING);

        Project project = new Project("test-project");
        project.setId(1l);
        SourceDocument doc = new SourceDocument("document name", project, "format");
        doc.setId(2l);
        AnnotationLayer layer = new AnnotationLayer(typeDesc.getName(), typeDesc.getDescription(),
                SPAN_TYPE, project, false, TOKENS, STACKING_ONLY);
        AnnotationFeature feat = new AnnotationFeature(project, layer, featDesc.getName(),
                featDesc.getDescription(), TYPE_NAME_STRING);

        CAS cas = CasFactory.createCas(tsd);
        AnnotationFS fs = cas.createAnnotation(cas.getTypeSystem().getType(typeDesc.getName()), 0,
                10);
        cas.addFsToIndexes(fs);

        FeatureValueUpdatedEvent event = new FeatureValueUpdatedEvent(getClass(), doc, "user",
                layer, fs, feat, "new-value", "old-value");

        FeatureValueUpdatedEventAdapter sut = new FeatureValueUpdatedEventAdapter();

        LoggedEvent loggedEvent = sut.toLoggedEvent(event);
        loggedEvent.setId(1l);
        loggedEvent.setCreated(Date.from(Instant.ofEpochMilli(1234567l)));

        ExportedLoggedEvent exportedEvent = ExportedLoggedEvent.fromLoggedEvent(doc.getName(),
                loggedEvent);

        // @formatter:off
        assertThat(toPrettyJsonString(exportedEvent)).isEqualTo("{\n"
                + "  \"annotator\" : \"user\",\n"
                + "  \"created\" : 1234567,\n"
                + "  \"details\" : {\"feature\":\"value\",\"value\":\"new-value\",\"previousValue\":\"old-value\",\"annotation\":{\"addr\":2,\"begin\":0,\"end\":10,\"type\":\"customType\"}},\n"
                + "  \"document_name\" : \"document name\",\n"
                + "  \"event\" : \"FeatureValueUpdatedEvent\",\n"
                + "  \"id\" : 1,\n"
                + "  \"user\" : \"<SYSTEM>\"\n"
                + "}");
        // @formatter:on
    }
}
