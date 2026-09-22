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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode.NEVER;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import jakarta.persistence.NoResultException;

public abstract class AnnotationPageBase
    extends ProjectPageBase
{
    private static final long serialVersionUID = -1133219266479577443L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PAGE_PARAM_DOCUMENT = "d";
    public static final String PAGE_PARAM_DATA_OWNER = "u";
    public static final String PAGE_PARAM_FOCUS = "f";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    protected AnnotationPageBase(PageParameters aParameters)
    {
        super(aParameters);
    }

    protected SourceDocument getDocumentFromParameters(Project aProject,
            StringValue aDocumentParameter)
    {
        if (aDocumentParameter.isEmpty()) {
            return null;
        }

        return getDocumentFromParameters(aProject, aDocumentParameter.toString());
    }

    /**
     * @param aProject
     *            the project to resolve in.
     * @param aDocumentParameter
     *            a single document id or document name, already split out of the URL parameter.
     * @return the document, or {@code null} if the parameter names none.
     */
    protected SourceDocument getDocumentFromParameters(Project aProject, String aDocumentParameter)
    {
        if (aDocumentParameter == null || aDocumentParameter.isBlank()) {
            return null;
        }

        try {
            try {
                long documentId = Long.parseLong(aDocumentParameter.trim());
                return documentService.getSourceDocument(aProject.getId(), documentId);
            }
            catch (NumberFormatException e) {
                // If it is not a number, try interpreting it as a name
            }

            return documentService.getSourceDocument(aProject, aDocumentParameter);
        }
        catch (NoResultException e) {
            failWithUnopenableDocument("Document [" + aDocumentParameter
                    + "] does not exist in project [" + aProject.getName() + "]");
        }
        return null;
    }

    protected void failWithUnopenableDocument(String aDetails)
    {
        failWithDocumentNotFound(aDetails);
    }

    protected void failWithDocumentNotFound(String aDetails)
    {
        if (userRepository.isCurrentUserAdmin()) {
            getSession().error(aDetails);
        }
        else {
            getSession().error(
                    "Requested document does not exist or you have no permissions to access it.");
        }
        backToProjectPage();
    }

    protected void handleException(AjaxRequestTarget aTarget, Exception aException)
    {
        if (aException instanceof ReplaceHandlerException replaceHandlerException) {
            // Let Wicket redirects still work
            throw replaceHandlerException;
        }

        LoggerFactory.getLogger(getClass()).error("Error: " + aException.getMessage(), aException);
        error("Error: " + aException.getMessage());
        if (aTarget != null) {
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    /**
     * @return the editor manager of this page.
     */
    public abstract DocumentEditorManager getDocumentEditorManager();

    /**
     * Checks if all required features on all annotations are set. If a required feature value is
     * missing, then the method scrolls to that location and schedules a re-rendering. In such a
     * case, an {@link IllegalStateException} is thrown.
     */
    protected void validateRequiredFeatures(AjaxRequestTarget aTarget, DiamContext aContext,
            CAS aCas, TypeAdapter aAdapter)
        throws ValidationException, IOException, AnnotationException
    {
        var editorCas = aCas;
        var layer = aAdapter.getLayer();
        var features = aAdapter.listFeatures();

        // If no feature is required, then we can skip the whole procedure
        if (features.stream().allMatch((f) -> !f.isRequired())) {
            return;
        }

        var evaluator = new ConstraintsEvaluator();
        var constraints = aContext.getAnnotatorState().getConstraints();

        var maybeLayerType = aAdapter.getAnnotationType(editorCas);
        if (maybeLayerType.isEmpty()) {
            return;
        }

        // Check each feature structure of this layer
        var layerType = maybeLayerType.get();
        var annotationFsType = editorCas.getAnnotationType();
        try (var fses = editorCas.select(layerType)) {
            for (var fs : fses) {
                for (var f : features) {
                    if (!f.isRequired()) {
                        continue;
                    }

                    if (!aAdapter.isFeatureDeclared(fs, f)) {
                        continue;
                    }

                    if (evaluator.isHiddenConditionalFeature(constraints, fs, f)) {
                        continue;
                    }

                    if (aAdapter.isFeatureValueValid(f, fs)) {
                        continue;
                    }

                    // Jump to invalid annotation if possible - through the context the CAS came
                    // from, since the VID is an address in that CAS.
                    if (editorCas.getTypeSystem().subsumes(annotationFsType, layerType)) {
                        aContext.actionActivateAndSelect(aTarget, VID.of(fs));
                    }

                    // Inform the user
                    throw new ValidationException("Annotation with ID [" + ICasUtil.getAddr(fs)
                            + "] on layer [" + layer.getUiName()
                            + "] has invalid feature value in [" + f.getUiName() + "].");
                }
            }
        }
    }

    /**
     * Validate the document shown in the given editor.
     *
     * @param aTarget
     *            the AJAX request target
     * @param aEditorContext
     *            the context of the editor whose document to validate
     */
    public void actionValidateDocument(AjaxRequestTarget aTarget, DiamContext aEditorContext)
        throws ValidationException, IOException, AnnotationException
    {
        var cas = aEditorContext.getEditorCas();
        for (var layer : annotationService.listAnnotationLayer(aEditorContext.getProject())) {
            if (!layer.isEnabled()) {
                continue;
            }

            if (layer.getValidationMode() == NEVER) {
                continue;
            }

            var adapter = annotationService.getAdapter(layer);

            validateRequiredFeatures(aTarget, aEditorContext, cas, adapter);

            var messages = adapter.validate(cas);
            if (!messages.isEmpty()) {
                var message = messages.get(0).getLeft();
                var fs = messages.get(0).getRight();

                aEditorContext.actionActivateAndSelect(aTarget, VID.of(fs));

                throw new ValidationException("Annotation with ID [" + VID.of(fs) + "] on layer ["
                        + layer.getUiName() + "] is invalid: " + message.getMessage());
            }
        }
    }
}
