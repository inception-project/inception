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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

// This support is not a component because we do not want it to be auto-scanned. It uses the 
// legacy TSV3 reader/writer code, not the new re-implementation.
//@Component
/**
 * @deprecated Superseded by {@link WebAnnoTsv3FormatSupport}
 */
@Deprecated
public class WebAnnoTsv3LegacyFormatSupport
    implements FormatSupport
{
    public static final String ID = "ctsv3";
    public static final String NAME = "WebAnno TSV3 (WebAnno v3; deprecated implementation)";

    private final AnnotationSchemaService annotationService;

    @Autowired
    public WebAnnoTsv3LegacyFormatSupport(AnnotationSchemaService aAnnotationService)
    {
        super();
        annotationService = aAnnotationService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }

    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public CollectionReaderDescription getReaderDescription(TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(WebannoTsv3Reader.class, aTSD);
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        List<AnnotationLayer> layers = annotationService.listAnnotationLayer(aProject);

        List<String> slotFeatures = new ArrayList<>();
        List<String> slotTargets = new ArrayList<>();
        List<String> linkTypes = new ArrayList<>();

        Set<String> spanLayers = new HashSet<>();
        Set<String> slotLayers = new HashSet<>();
        for (AnnotationLayer layer : layers) {

            if (layer.getType().contentEquals(WebAnnoConst.SPAN_TYPE)) {
                // TSV will not use this
                if (!annotationExists(aCAS, layer.getName())) {
                    continue;
                }
                boolean isslotLayer = false;
                for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                    if (MultiValueMode.ARRAY.equals(f.getMultiValueMode())
                            && LinkMode.WITH_ROLE.equals(f.getLinkMode())) {
                        isslotLayer = true;
                        slotFeatures.add(layer.getName() + ":" + f.getName());
                        slotTargets.add(f.getType());
                        linkTypes.add(f.getLinkTypeName());
                    }
                }

                if (isslotLayer) {
                    slotLayers.add(layer.getName());
                }
                else {
                    spanLayers.add(layer.getName());
                }
            }
        }
        spanLayers.addAll(slotLayers);
        List<String> chainLayers = new ArrayList<>();
        for (AnnotationLayer layer : layers) {
            if (layer.getType().contentEquals(WebAnnoConst.CHAIN_TYPE)) {
                if (!chainAnnotationExists(aCAS, layer.getName() + "Chain")) {
                    continue;
                }
                chainLayers.add(layer.getName());
            }
        }

        List<String> relationLayers = new ArrayList<>();
        for (AnnotationLayer layer : layers) {
            if (layer.getType().contentEquals(WebAnnoConst.RELATION_TYPE)) {
                // TSV will not use this
                if (!annotationExists(aCAS, layer.getName())) {
                    continue;
                }
                relationLayers.add(layer.getName());
            }
        }

        // @formatter:off
        return createEngineDescription(WebannoTsv3Writer.class, aTSD,
                "spanLayers", spanLayers, 
                "slotFeatures", slotFeatures, 
                "slotTargets", slotTargets, 
                "linkTypes", linkTypes, 
                "chainLayers", chainLayers,
                "relationLayers", relationLayers);
        // @formatter:on
    }

    private boolean annotationExists(CAS aCas, String aType)
    {

        Type type = aCas.getTypeSystem().getType(aType);
        if (CasUtil.select(aCas, type).size() == 0) {
            return false;
        }
        return true;
    }

    private boolean chainAnnotationExists(CAS aCas, String aType)
    {

        Type type = aCas.getTypeSystem().getType(aType);
        if (CasUtil.selectFS(aCas, type).size() == 0) {
            return false;
        }
        return true;
    }
}
