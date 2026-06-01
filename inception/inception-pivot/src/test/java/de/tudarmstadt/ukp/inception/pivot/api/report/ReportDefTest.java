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
package de.tudarmstadt.ukp.inception.pivot.api.report;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ReportDefTest
{
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void roundTripsFullyPopulatedReport() throws Exception
    {
        var original = new ReportDef();
        original.setAggregator(new AggregatorDef("count"));
        original.setRowExtractors(asList( //
                new ExtractorDef("documentName", "Token", null), //
                new ExtractorDef("featureValue", "Token", "pos")));
        original.setColExtractors(asList( //
                new ExtractorDef("annotator", "Token", null)));
        original.setCellExtractors(asList( //
                new ExtractorDef("featureValue", "Token", "lemma")));
        original.getFilter().setAnnotators(asList("alice", "bob"));
        original.getFilter().setDocuments(asList("doc1.txt", "doc2.txt"));
        original.getFilter().setStates(asList(IN_PROGRESS, FINISHED));

        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, ReportDef.class);

        assertThat(restored).isEqualTo(original);
        assertThat(restored.getSchemaVersion()).isEqualTo(ReportDef.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void omitsEmptyCollectionsAndNulls() throws Exception
    {
        var def = new ReportDef();
        def.setAggregator(new AggregatorDef("count"));

        var json = mapper.writeValueAsString(def);

        assertThat(json) //
                .contains("\"aggregator\":{\"id\":\"count\"}") //
                .contains("\"schemaVersion\":1") //
                .doesNotContain("rowExtractors") //
                .doesNotContain("colExtractors") //
                .doesNotContain("cellExtractors") //
                .doesNotContain("annotators") //
                .doesNotContain("documents") //
                .doesNotContain("states");
    }

    @Test
    void tolerateUnknownProperties() throws Exception
    {
        var json = """
                {
                  "schemaVersion": 1,
                  "aggregator": { "id": "count", "someFutureField": "ignored" },
                  "someFutureField": "ignored",
                  "filter": { "unknownFilter": [] }
                }
                """;

        var restored = mapper.readValue(json, ReportDef.class);

        assertThat(restored.getAggregator()).isEqualTo(new AggregatorDef("count"));
        assertThat(restored.getFilter()).isNotNull();
    }

    @Test
    void layerExtractorDefHasNullFeature() throws Exception
    {
        var ref = new ExtractorDef("documentName", "Token", null);

        var json = mapper.writeValueAsString(ref);
        var restored = mapper.readValue(json, ExtractorDef.class);

        assertThat(json).doesNotContain("feature");
        assertThat(restored).isEqualTo(ref);
        assertThat(restored.getFeature()).isNull();
    }
}
