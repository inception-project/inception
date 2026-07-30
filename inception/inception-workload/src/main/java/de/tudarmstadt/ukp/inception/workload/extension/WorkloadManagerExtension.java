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
package de.tudarmstadt.ukp.inception.workload.extension;

import java.util.Optional;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * Extensions for the workload manager. Also has a readTraits and writeTraits property for the DB
 * entry traits
 * 
 * @param <T>
 *            traits type
 */
public interface WorkloadManagerExtension<T>
    extends Extension<Project>
{
    public static final String WORKLOAD_ACTION_BAR_ROLE = "Workload";

    @Override
    default boolean accepts(Project project)
    {
        return true;
    }

    String getLabel();

    T readTraits(WorkloadManager aWorkloadManager);

    void writeTraits(WorkloadManager aWorkloadManager, T aTraits);

    void writeTraits(T aTrait, Project aProject);

    /**
     * Returns a Wicket component to configure the specific traits of this workload extension. Note
     * that every {@link WorkloadManagerExtension} has to return a <b>different class</b> here. So
     * it is not possible to simple return a Wicket {@link Panel} here, but it must be a subclass of
     * {@link Panel} used exclusively by the current {@link WorkloadManagerExtension}. If this is
     * not done, then the traits editor in the UI will not be correctly updated when switching
     * between feature types!
     * 
     * @param aId
     *            a markup ID.
     * @param aWorkloadManager
     *            a model holding the workload manager for which the traits editor should be
     *            created.
     * @return the traits editor component.
     */
    default Panel createTraitsEditor(String aId, IModel<WorkloadManager> aWorkloadManager)
    {
        return new EmptyPanel(aId);
    }

    /**
     * Ask the workload manager to immediately recalculate the state of all documents in the project
     * and of the project itself. This is necessary when switching from one workload manager to
     * another.
     * 
     * @param aProject
     *            a project
     * @return the state of the project after the recalculation has been completed
     */
    ProjectState recalculate(Project aProject);

    /**
     * Ask the workload manager to immediately recalculate the state of a single document and of the
     * project itself. Use this instead of {@link #recalculate(Project)} when only one document has
     * changed - it avoids walking every document in the project.
     *
     * @param aDocument
     *            a source document (its state will be updated as a side-effect)
     * @return the state of the project after the recalculation has been completed
     */
    ProjectState recalculate(SourceDocument aDocument);

    /**
     * Ask the workload manager to immediately refresh the state of the documents and overall
     * project. This can be called immediately before fetching the project status in order to ensure
     * that the project status is reliable.
     * 
     * @param aProject
     *            a project
     * @return the state of the project after the freshening has been completed
     */
    ProjectState freshenStatus(Project aProject);

    /**
     * @return whether the current user can access documents in any order or if the workload manager
     *         assigns the order.
     *
     *         <b>NOTE:</b> This is currently used to control the visibiltiy of the activities
     *         dashlet on on the project dashboard. A better approach would be to modularize the
     *         dashboard and then have some factory in the workload modules inject the dashlet
     *         instead.
     *
     * @param aProject
     *            a project
     */
    boolean isDocumentRandomAccessAllowed(Project aProject);

    /**
     * Whether the workload manager would consider annotation on the given document complete enough
     * for the document to be curated - an entry condition, not a continuous invariant, so it must
     * never drive an automatic transition out of a curation state.
     * <p>
     * <b>Implementations MUST be side-effect free.</b> This method is called from render paths (the
     * curation page, the workload management pages). It must not write document or project states.
     *
     * @param aDocument
     *            a source document.
     * @return whether the document is ready for curation. Extensions that have no notion of
     *         readiness report {@code true} so that no warning is shown.
     */
    default boolean isReadyForCuration(SourceDocument aDocument)
    {
        return true;
    }

    /**
     * A human-readable description of why the given document is not ready for curation - e.g. "2 of
     * 5 annotators have neither finished nor locked the document". Used to warn a curator that they
     * are about to curate incomplete data.
     * <p>
     * <b>Implementations MUST be side-effect free</b> for the same reason as
     * {@link #isReadyForCuration}.
     *
     * @param aDocument
     *            a source document.
     * @return the warning, or {@link Optional#empty()} if the document is ready for curation (or if
     *         the extension has no notion of readiness).
     */
    default Optional<CurationReadinessWarning> getCurationReadinessWarning(SourceDocument aDocument)
    {
        return Optional.empty();
    }
}
