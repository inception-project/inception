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
package de.tudarmstadt.ukp.inception.annotation.layer.chain.export;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Arrays.fill;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.http.MediaType;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporter_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class ChainLayerToCsvExporter
    extends CrossDocumentExporter_ImplBase
{
    private final AnnotationSchemaService schemaService;

    public ChainLayerToCsvExporter(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService)
    {
        super(aDocumentService);
        schemaService = aSchemaService;
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return ChainLayerSupport.TYPE.equals(aLayer.getType());
    }

    @Override
    public void export(OutputStream aOut, AnnotationLayer aLayer, AnnotationFeature aFeature,
            Map<SourceDocument, List<AnnotationDocument>> allAnnDocs,
            List<AnnotationSet> aDataOwners)
        throws IOException
    {
        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();

        var adapter = (ChainAdapter) schemaService.getAdapter(aLayer);

        var headers = new ArrayList<String>();
        headers.addAll(asList("doc", "user", "chain", "index", "begin", "end", "text", "label"));
        if (adapter.isLinkedListBehavior()) {
            headers.add("link_label");
        }

        var rec = new Object[headers.size()];

        try (var writer = new OutputStreamWriter(aOut, UTF_8);
                var csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader(headers.toArray(String[]::new)).get())) {

            for (var doc : docs) {
                var annDocs = allAnnDocs.get(doc);
                try (var session = CasStorageSession.openNested()) {
                    for (var dataOwner : aDataOwners) {
                        var cas = loadCasOrInitialCas(doc, dataOwner.id(), annDocs);
                        if (cas.getTypeSystem().getType(adapter.getChainTypeName()) == null) {
                            // If the types are not defined, then we do not need to try and render
                            // them because the CAS does not contain any instances of them
                            continue;
                        }

                        var chainFirst = cas.getTypeSystem().getType(adapter.getChainTypeName())
                                .getFeatureByBaseName(adapter.getChainFirstFeatureName());
                        var spanLabelFeature = cas.getTypeSystem()
                                .getType(adapter.getAnnotationTypeName())
                                .getFeatureByBaseName(ChainAdapter.SPAN_LABEL_FEATURE);
                        var arcLabelFeature = cas.getTypeSystem()
                                .getType(adapter.getAnnotationTypeName())
                                .getFeatureByBaseName(ChainAdapter.ARC_LABEL_FEATURE);

                        var chainId = 1;
                        for (var chainFs : cas.select(adapter.getChainTypeName())) {
                            var linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
                            if (linkFs == null) {
                                continue; // Skip empty chains
                            }

                            fill(rec, null);

                            rec[0] = doc.getName();
                            rec[1] = dataOwner;
                            rec[2] = chainId;

                            // Iterate over the links of the chain
                            var linkIndex = 1;
                            while (linkFs != null) {
                                var linkNext = linkFs.getType()
                                        .getFeatureByBaseName(adapter.getLinkNextFeatureName());
                                var nextLinkFs = (AnnotationFS) linkFs.getFeatureValue(linkNext);

                                rec[3] = linkIndex;
                                rec[4] = linkFs.getBegin();
                                rec[5] = linkFs.getEnd();
                                rec[6] = linkFs.getCoveredText();

                                if (spanLabelFeature != null) {
                                    rec[7] = linkFs.getFeatureValueAsString(spanLabelFeature);
                                }
                                else {
                                    rec[7] = "";
                                }

                                if (adapter.isLinkedListBehavior()) {
                                    if (arcLabelFeature != null) {
                                        rec[8] = linkFs.getFeatureValueAsString(arcLabelFeature);
                                    }
                                    else {
                                        rec[8] = "";
                                    }
                                }

                                csvPrinter.printRecord(rec);

                                linkFs = nextLinkFs;
                                linkIndex++;
                            }

                            chainId++;
                        }

                        csvPrinter.flush();
                    }
                }
            }
        }
    }

    @Override
    public String getFileExtension()
    {
        return EXT_CSV;
    }

    @Override
    public MediaType getMediaType()
    {
        return MEDIA_TYPE_TEXT_CSV;
    }
}
