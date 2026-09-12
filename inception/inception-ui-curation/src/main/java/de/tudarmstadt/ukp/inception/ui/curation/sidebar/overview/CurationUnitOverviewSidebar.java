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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.overview;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil.handleException;
import static java.lang.System.currentTimeMillis;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.event.CurationUnitClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnit;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitOverview;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationEditingService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class CurationUnitOverviewSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -5312616540545974339L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CurationEditingService curationEditingService;
    private @SpringBean CurationService curationService;
    private @SpringBean DiffAdapterRegistry diffAdapterRegistry;

    private WebMarkupContainer mainContainer;

    public CurationUnitOverviewSidebar(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aAnnotationPage);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        mainContainer.add(new CurationUnitOverview("unitOverview",
                LoadableDetachableModel.of(() -> getActiveEditorState().orElse(null)),
                LoadableDetachableModel.of(this::getUnitsForCurrentDocument)));
        mainContainer.add(new LambdaAjaxLink("refresh", this::actionRefresh));

        mainContainer.add(new Label("notComputable", new ResourceModel("notComputable"))
                .add(visibleWhen(() -> hasDocument() && isNotComputable())));
        mainContainer.add(new Label("noUnits", new ResourceModel("noUnits"))
                .add(visibleWhen(() -> hasDocument() && !isNotComputable()
                        && getUnitsForCurrentDocument().isEmpty())));
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        // The units are computed the first time the sidebar is actually shown rather than when the
        // document is loaded: the per-unit CasDiff is expensive and most curators never open this
        // tab at all. Recomputing on a document switch is not optional though - the units of the
        // previous document are simply wrong for this one.
        var document = getActiveEditorState().map(AnnotatorState::getDocument).orElse(null);
        if (getUnits().isStaleFor(document)) {
            refreshUnits(null);
        }
    }

    @OnEvent
    public void onUnitClickedEvent(CurationUnitClickedEvent aEvent)
    {
        try {
            var context = getActiveContext().orElse(null);
            if (context == null) {
                return;
            }

            var state = context.getAnnotatorState();
            var cas = curationDocumentService.readCurationCas(state.getDocument());
            state.getPagingStrategy().moveToOffset(state, cas, aEvent.getUnit().getBegin(),
                    CENTERED);
            state.setFocusUnitIndex(aEvent.getUnit().getUnitIndex());

            context.actionRefreshDocument(aEvent.getTarget());

            if (aEvent.getTarget() != null) {
                aEvent.getTarget().add(mainContainer);
            }
        }
        catch (Exception e) {
            handleException(LOG, this, aEvent.getTarget(), e);
        }
    }

    /**
     * Repaint the badges when an annotation changes, so the "current unit" highlight and the
     * in-range/out-of-range borders keep up with the editor.
     * <p>
     * Do not recalculate diff (slow) - let users use the refresh button instead.
     */
    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        aEvent.getRequestTarget().ifPresent(target -> target.add(mainContainer));
    }

    private void actionRefresh(AjaxRequestTarget aTarget)
    {
        refreshUnits(aTarget);
        aTarget.add(mainContainer);
    }

    private void refreshUnits(AjaxRequestTarget aTarget)
    {
        try {
            var state = getActiveEditorState().orElse(null);
            if (state == null) {
                return;
            }

            buildUnitOverview(state).ifPresent(units -> getUnits().set(state.getDocument(), units));
        }
        catch (Exception e) {
            handleException(LOG, this, aTarget, e);
        }
    }

    private List<CurationUnit> getUnitsForCurrentDocument()
    {
        var document = getActiveEditorState().map(AnnotatorState::getDocument).orElse(null);
        if (document == null || getUnits().isStaleFor(document)) {
            return emptyList();
        }

        return getUnits().getUnits();
    }

    private boolean hasDocument()
    {
        return getActiveEditorState().map(AnnotatorState::getDocument).isPresent();
    }

    private Optional<AnnotatorState> getActiveEditorState()
    {
        return getActiveContext().map(DiamContext::getAnnotatorState);
    }

    private boolean isNotComputable()
    {
        return getUnits()
                .isStaleFor(getActiveEditorState().map(AnnotatorState::getDocument).orElse(null));
    }

    private CurationUnits getUnits()
    {
        return CurationUnitsMetaDataKey.get(getPage());
    }

    /**
     * @return the computed units, or {@link Optional#empty()} if they could not be computed for the
     *         given state.
     */
    private Optional<List<CurationUnit>> buildUnitOverview(AnnotatorState aState)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        var document = aState.getDocument();

        if (!curationDocumentService.existsCurationCas(document)) {
            return Optional.empty();
        }

        var curatableUsers = curationDocumentService.listCuratableUsers(document);
        var casses = documentService.readAllCasesSharedNoUpgrade(document, curatableUsers);

        // Load CAS without updating its timestamp since the sidebar is only an auxiliary view
        var editorCas = curationEditingService.readCurationCas(document,
                aState.getUser().getUsername(), casses, null, false, aState.getAnnotationLayers(),
                curationService.getDefaultMergeStrategy(document.getProject()), LOAD_ONLY);
        casses.put(CURATION_USER, editorCas);

        var adapters = diffAdapterRegistry.getDiffAdapters(aState.getAnnotationLayers());

        var diffStart = currentTimeMillis();
        LOG.debug("Calculating differences...");
        var unitIndex = 0;
        var curationUnitList = new ArrayList<CurationUnit>();
        var units = aState.getPagingStrategy().units(editorCas);
        for (var unit : units) {
            unitIndex++;
            if (unitIndex % 100 == 0) {
                LOG.debug("Processing differences: {} of {} units...", unitIndex, units.size());
            }

            var diff = doDiff(adapters, casses, unit.getBegin(), unit.getEnd()).toResult();

            var curationUnit = new CurationUnit(unit.getBegin(), unit.getEnd(), unitIndex);
            curationUnit.setState(CasDiffSummaryState.calculateState(diff));

            curationUnitList.add(curationUnit);
        }
        LOG.debug("Difference calculation completed in {}ms", (currentTimeMillis() - diffStart));

        return Optional.of(curationUnitList);
    }
}
