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
package de.tudarmstadt.ukp.inception.curation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMergeOperationResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

@Component(CurationEditorExtension.EXTENSION_ID)
public class CurationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    public static final String EXTENSION_ID = "curationEditorExtension";

    // actions from the ui when selecting span or arc annotation
    private static final String ACTION_SELECT_ARC = "arcOpenDialog";
    private static final String ACTION_SELECT_SPAN = "spanOpenDialog";

    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired DocumentService documentService;
    private @Autowired CurationRenderer curationRenderer;

    @Override
    public String getBeanName()
    {
        return EXTENSION_ID;
    }

    @Override
    public void handleAction(AnnotationActionHandler aPanel, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aParamId, String aAction)
        throws AnnotationException, IOException
    {
        // only process actions relevant to curation
        if (!aParamId.getExtensionId().equals(EXTENSION_ID)) {
            return;
        }

        if (!aAction.equals(ACTION_SELECT_ARC) && !aAction.equals(ACTION_SELECT_SPAN)) {
            return;
        }

        // Annotation has been selected for gold
        CurationVID extendedVID = CurationVID.parse(aParamId.getExtensionPayload());
        if (extendedVID != null) {
            saveAnnotation(aAction, aPanel, aState, aTarget, aCas, extendedVID);
        }
    }

    /**
     * Save annotation identified by aVID from user CAS to given curator's CAS
     */
    private void saveAnnotation(String aAction, AnnotationActionHandler aPanel,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aTargetCas,
            CurationVID aCurationVid)
        throws IOException, AnnotationException
    {
        // get user CAS and annotation (to be merged into curator's)
        SourceDocument doc = aState.getDocument();
        String srcUser = aCurationVid.getUsername();

        if (!documentService.existsAnnotationDocument(doc, srcUser)) {
            log.error(String.format("Source CAS of %s for curation not found", srcUser));
            return;
        }

        VID vid = VID.parse(aCurationVid.getExtensionPayload());

        CAS srcCas = documentService.readAnnotationCas(doc, srcUser);
        AnnotationFS sourceAnnotation = selectAnnotationByAddr(srcCas, vid.getId());
        AnnotationLayer layer = annotationService.findLayer(aState.getProject(), sourceAnnotation);

        // merge into curator's CAS depending on annotation type (span or arc)
        CasMerge casMerge = new CasMerge(annotationService);
        CasMergeOperationResult mergeResult;
        if (ACTION_SELECT_SPAN.equals(aAction.toString())) {
            mergeResult = casMerge.mergeSpanAnnotation(doc, srcUser, layer, aTargetCas,
                    sourceAnnotation, layer.isAllowStacking());

            // open created/updates FS in annotation detail editorpanel
            AnnotationFS mergedAnno = selectAnnotationByAddr(aTargetCas,
                    mergeResult.getResultFSAddress());
            aState.getSelection().selectSpan(mergedAnno);
        }
        else if (ACTION_SELECT_ARC.equals(aAction.toString())) {
            // this is a slot arc
            if (vid.isSlotSet()) {
                TypeAdapter adapter = annotationService.getAdapter(layer);
                AnnotationFeature feature = adapter.listFeatures().stream().sequential()
                        .skip(vid.getAttribute()).findFirst().get();

                mergeResult = casMerge.mergeSlotFeature(doc, srcUser, layer, aTargetCas,
                        sourceAnnotation, feature.getName(), vid.getSlot());

                // open created/updates FS in annotation detail editorpanel
                AnnotationFS mergedAnno = selectAnnotationByAddr(aTargetCas,
                        mergeResult.getResultFSAddress());
                aState.getSelection().selectSpan(mergedAnno);
            }
            // normal relation annotation arc is clicked
            else {
                mergeResult = casMerge.mergeRelationAnnotation(doc, srcUser, layer, aTargetCas,
                        sourceAnnotation, layer.isAllowStacking());

                // open created/updates FS in annotation detail editorpanel
                AnnotationFS mergedAnno = selectAnnotationByAddr(aTargetCas,
                        mergeResult.getResultFSAddress());
                aState.getSelection().selectArc(mergedAnno);
            }
        }

        aPanel.actionSelect(aTarget);
        aPanel.actionCreateOrUpdate(aTarget, aTargetCas); // should also update timestamps
    }

    @Override
    public void render(CAS aCas, AnnotatorState aState, VDocument aVdoc, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        curationRenderer.render(aCas, aState, aVdoc, aWindowBeginOffset, aWindowEndOffset);
    }
}
