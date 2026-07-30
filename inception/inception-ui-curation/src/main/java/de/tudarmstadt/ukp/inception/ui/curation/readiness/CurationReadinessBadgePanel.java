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
package de.tudarmstadt.ukp.inception.ui.curation.readiness;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.workload.extension.CurationReadinessWarning;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * A compact badge warning the curator that the workload manager would not consider the current
 * document ready for curation - e.g. because not all annotators have finished it. The badge shows
 * the shortfall (e.g. <code>1/3 incomplete</code>); the full explanation is available as a tooltip.
 *
 * @see WorkloadManagerExtension#getCurationReadinessWarning
 */
public class CurationReadinessBadgePanel
    extends Panel
{
    private static final long serialVersionUID = -3162880656011300995L;

    private @SpringBean WorkloadManagementService workloadManagementService;

    // Held as a field so it is detached every request cycle - otherwise the readiness would be
    // computed once and then served stale for the lifetime of the panel
    private final IModel<Optional<CurationReadinessWarning>> warning;

    public CurationReadinessBadgePanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        setOutputMarkupPlaceholderTag(true);

        warning = LoadableDetachableModel.of(this::loadWarning);

        var badge = new WebMarkupContainer("badge");
        badge.add(new AttributeModifier("title",
                warning.map(w -> w.map(CurationReadinessWarning::message).orElse(null))));
        queue(badge);

        queue(new Label("shortLabel",
                warning.map(w -> w.map(CurationReadinessWarning::shortLabel).orElse(null))));

        add(visibleWhen(warning.map(Optional::isPresent).orElse(false)));
    }

    @Override
    protected void onDetach()
    {
        warning.detach();

        super.onDetach();
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    private Optional<CurationReadinessWarning> loadWarning()
    {
        var state = getModel().getObject();

        if (state == null || state.getDocument() == null || state.getUser() == null) {
            return Optional.empty();
        }

        // On the curation page / in a curation sidebar session, the warning only makes sense while
        // the curation target - and not some annotator's document - is being edited.
        if (!CURATION_USER.equals(state.getUser().getUsername())) {
            return Optional.empty();
        }

        var document = state.getDocument();

        return workloadManagementService.getWorkloadManagerExtension(document.getProject())
                .getCurationReadinessWarning(document);
    }
}
