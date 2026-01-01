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
package de.tudarmstadt.ukp.inception.export;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
class LayerImportExportUtilsTest
{
    private @Mock AnnotationSchemaService annotationService;

    private Project project;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp()
    {
        project = Project.builder() //
                .withId(1L) //
                .withName("Test Project") //
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void thatExportLayersToJsonExportsAllLayers() throws Exception
    {
        var layer1 = createSpanLayer(1, "Layer1", "Layer 1");
        var layer2 = createSpanLayer(2, "Layer2", "Layer 2");
        var layer3 = createSpanLayer(3, "Layer3", "Layer 3");

        when(annotationService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(List.of());

        var json = LayerImportExportUtils.exportLayersToJson(annotationService,
                asList(layer1, layer2, layer3));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        assertThat(exportedLayers).hasSize(3);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("Layer1", "Layer2", "Layer3");
    }

    @Test
    void thatExportLayersToJsonIncludesAttachLayers() throws Exception
    {
        var attachLayer = createSpanLayer(1, "AttachLayer", "Attach Layer");
        var relationLayer = createRelationLayer(2, "RelationLayer", "Relation Layer", attachLayer);

        when(annotationService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(List.of());

        var json = LayerImportExportUtils.exportLayersToJson(annotationService,
                asList(relationLayer));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        assertThat(exportedLayers).hasSize(2);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("RelationLayer", "AttachLayer");

        var exportedRelationLayer = findLayerByName(exportedLayers, "RelationLayer");
        assertThat(exportedRelationLayer.getAttachType()).isNotNull();
        assertThat(exportedRelationLayer.getAttachType().getName()).isEqualTo("AttachLayer");
    }

    @Test
    void thatExportLayersToJsonDeduplicatesAttachLayers() throws Exception
    {
        var attachLayer = createSpanLayer(1, "AttachLayer", "Attach Layer");
        var relationLayer1 = createRelationLayer(2, "RelationLayer1", "Relation Layer 1",
                attachLayer);
        var relationLayer2 = createRelationLayer(3, "RelationLayer2", "Relation Layer 2",
                attachLayer);

        when(annotationService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(List.of());

        var json = LayerImportExportUtils.exportLayersToJson(annotationService,
                asList(relationLayer1, relationLayer2));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        // Should export 3 layers total: 2 relation layers + 1 shared attach layer
        assertThat(exportedLayers).hasSize(3);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("RelationLayer1", "RelationLayer2", "AttachLayer");
    }

    @Test
    void thatExportLayersToJsonIncludesLinkFeatureTargetLayers() throws Exception
    {
        var targetLayer = createSpanLayer(1, "TargetLayer", "Target Layer");
        var sourceLayer = createSpanLayer(1, "SourceLayer", "Source Layer");
        var linkFeature = createLinkFeature(2, "linkFeature", "Link Feature", targetLayer);

        when(annotationService.listAnnotationFeature(sourceLayer)).thenReturn(asList(linkFeature));
        when(annotationService.listAnnotationFeature(targetLayer)).thenReturn(List.of());
        when(annotationService.findLayer(project, "TargetLayer")).thenReturn(targetLayer);

        var json = LayerImportExportUtils.exportLayersToJson(annotationService,
                asList(sourceLayer));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        assertThat(exportedLayers).hasSize(2);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("SourceLayer", "TargetLayer");
    }

    @Test
    void thatExportLayersToJsonDeduplicatesLinkFeatureTargetLayers() throws Exception
    {
        var targetLayer = createSpanLayer(1, "TargetLayer", "Target Layer");
        var sourceLayer1 = createSpanLayer(2, "SourceLayer1", "Source Layer 1");
        var sourceLayer2 = createSpanLayer(3, "SourceLayer2", "Source Layer 2");
        var linkFeature1 = createLinkFeature(4, "linkFeature1", "Link Feature 1", targetLayer);
        var linkFeature2 = createLinkFeature(5, "linkFeature2", "Link Feature 2", targetLayer);

        when(annotationService.listAnnotationFeature(sourceLayer1))
                .thenReturn(asList(linkFeature1));
        when(annotationService.listAnnotationFeature(sourceLayer2))
                .thenReturn(asList(linkFeature2));
        when(annotationService.listAnnotationFeature(targetLayer)).thenReturn(List.of());
        when(annotationService.findLayer(project, "TargetLayer")).thenReturn(targetLayer);

        var json = LayerImportExportUtils.exportLayersToJson(annotationService,
                asList(sourceLayer1, sourceLayer2));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        // Should export 3 layers total: 2 source layers + 1 shared target layer
        assertThat(exportedLayers).hasSize(3);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("SourceLayer1", "SourceLayer2", "TargetLayer");
    }

    @Test
    void thatExportLayersToJsonHandlesComplexDependencies() throws Exception
    {
        // Setup: Layer1 -> (link) -> Layer2 -> (attach) -> Layer3
        var layer3 = createSpanLayer(1, "Layer3", "Layer 3");
        var layer2 = createRelationLayer(2, "Layer2", "Layer 2", layer3);
        var layer1 = createSpanLayer(3, "Layer1", "Layer 1");
        var linkFeature = createLinkFeature(4, "linkToLayer2", "Link to Layer 2", layer2);

        when(annotationService.listAnnotationFeature(layer1)).thenReturn(asList(linkFeature));
        when(annotationService.listAnnotationFeature(layer2)).thenReturn(List.of());
        when(annotationService.listAnnotationFeature(layer3)).thenReturn(List.of());
        when(annotationService.findLayer(project, "Layer2")).thenReturn(layer2);

        var json = LayerImportExportUtils.exportLayersToJson(annotationService, asList(layer1));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        // Should export all 3 layers following the dependency chain
        assertThat(exportedLayers).hasSize(3);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("Layer1", "Layer2", "Layer3");
    }

    @Test
    void thatExportLayersToJsonSkipsGenericAnnotationType() throws Exception
    {
        var layer = createSpanLayer(1, "Layer1", "Layer 1");
        var linkFeature = createLinkFeature(2, "genericLink", "Generic Link", null);
        linkFeature.setType(CAS.TYPE_NAME_ANNOTATION); // Generic type

        when(annotationService.listAnnotationFeature(layer)).thenReturn(asList(linkFeature));

        var json = LayerImportExportUtils.exportLayersToJson(annotationService, asList(layer));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        // Should only export Layer1, not try to export the generic annotation type
        assertThat(exportedLayers).hasSize(1);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactly("Layer1");
    }

    @Test
    void thatExportLayersToJsonHandlesNonExistentLinkTargetLayer() throws Exception
    {
        var layer = createSpanLayer(1, "Layer1", "Layer 1");
        var linkFeature = createLinkFeature(2, "brokenLink", "Broken Link", null);
        linkFeature.setType("NonExistentLayer");

        when(annotationService.listAnnotationFeature(layer)).thenReturn(asList(linkFeature));
        when(annotationService.findLayer(project, "NonExistentLayer"))
                .thenThrow(new RuntimeException("Layer not found"));

        // Should not throw exception, just log warning
        var json = LayerImportExportUtils.exportLayersToJson(annotationService, asList(layer));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        assertThat(exportedLayers).hasSize(1);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactly("Layer1");
    }

    @Test
    void thatExportLayersToJsonHandlesEmptyList() throws Exception
    {
        var json = LayerImportExportUtils.exportLayersToJson(annotationService, List.of());

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        assertThat(exportedLayers).isEmpty();
    }

    @Test
    void thatExportLayersToJsonHandlesBuiltInLayers() throws Exception
    {
        var builtInLayer = createSpanLayer(1, "Token", "Token");
        builtInLayer.setBuiltIn(true);
        var customLayer = createSpanLayer(2, "CustomLayer", "Custom Layer");

        when(annotationService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(List.of());

        var json = LayerImportExportUtils.exportLayersToJson(annotationService,
                asList(builtInLayer, customLayer));

        var exportedLayers = objectMapper.readValue(json, ExportedAnnotationLayer[].class);
        assertThat(exportedLayers).hasSize(2);
        assertThat(exportedLayers).extracting(ExportedAnnotationLayer::getName)
                .containsExactlyInAnyOrder("Token", "CustomLayer");

        var exportedBuiltIn = findLayerByName(exportedLayers, "Token");
        assertThat(exportedBuiltIn.isBuiltIn()).isTrue();
    }

    private AnnotationLayer createSpanLayer(long aId, String aName, String aUiName)
    {
        var layer = AnnotationLayer.builder() //
                .withId(aId) //
                .withName(aName) //
                .withUiName(aUiName) //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(project) //
                .withAnchoringMode(TOKENS) //
                .withOverlapMode(NO_OVERLAP) //
                .build();
        return layer;
    }

    private AnnotationLayer createRelationLayer(long aId, String aName, String aUiName,
            AnnotationLayer aAttachLayer)
    {
        var layer = AnnotationLayer.builder() //
                .withId(aId) //
                .withName(aName) //
                .withUiName(aUiName) //
                .withType(RelationLayerSupport.TYPE) //
                .withProject(project) //
                .withAnchoringMode(TOKENS) //
                .withOverlapMode(NO_OVERLAP) //
                .withAttachType(aAttachLayer) //
                .build();
        return layer;
    }

    private AnnotationFeature createLinkFeature(long aId, String aName, String aUiName,
            AnnotationLayer aTargetLayer)
    {
        var feature = AnnotationFeature.builder() //
                .withId(aId) //
                .withName(aName) //
                .withUiName(aUiName) //
                .withType(aTargetLayer != null ? aTargetLayer.getName() : "SomeType") //
                .withProject(project) //
                .withLinkMode(LinkMode.WITH_ROLE) //
                .build();
        return feature;
    }

    private ExportedAnnotationLayer findLayerByName(ExportedAnnotationLayer[] aLayers, String aName)
    {
        for (var layer : aLayers) {
            if (layer.getName().equals(aName)) {
                return layer;
            }
        }
        return null;
    }
}
