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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.inception.curation.model.CurationSessionPreferences.KEY_CURATION_SESSION;
import static java.util.stream.Collectors.toCollection;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.model.CurationSessionPreferences;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

public class CurationSessionServiceImpl
    implements CurationSessionService
{
    private final PreferencesService preferencesService;
    private final ProjectService projectService;
    private final UserDao userRegistry;
    private final CurationDocumentService curationDocumentService;
    private final DocumentService documentService;

    public CurationSessionServiceImpl(PreferencesService aPreferencesService,
            ProjectService aProjectService, UserDao aUserRegistry,
            CurationDocumentService aCurationDocumentService, DocumentService aDocumentService)
    {
        preferencesService = aPreferencesService;
        projectService = aProjectService;
        userRegistry = aUserRegistry;
        curationDocumentService = aCurationDocumentService;
        documentService = aDocumentService;
    }

    @Transactional
    @Override
    public void setSelectedDataOwners(String aSessionOwner, long aProjectId,
            Collection<AnnotationSet> aCandidates, Collection<AnnotationSet> aSelected)
    {
        updatePreferences(getUser(aSessionOwner), projectService.getProject(aProjectId), prefs -> {
            var deselected = new LinkedHashSet<>(prefs.getDeselectedDataOwners());
            for (var candidate : aCandidates) {
                if (aSelected.contains(candidate)) {
                    deselected.remove(candidate.id());
                }
                else {
                    deselected.add(candidate.id());
                }
            }
            prefs.setDeselectedDataOwners(deselected);
        });
    }

    @Transactional
    @Override
    public Set<AnnotationSet> getSelectedDataOwners(String aSessionOwner, long aProjectId,
            Collection<AnnotationSet> aCandidates)
    {
        var deselected = listDeselectedDataOwners(getUser(aSessionOwner),
                projectService.getProject(aProjectId));

        var selected = new LinkedHashSet<>(aCandidates);
        selected.removeAll(deselected);
        return selected;
    }

    @Transactional
    @Override
    public List<AnnotationSet> listDataOwnersReadyForCuration(String aSessionOwner,
            Project aProject, SourceDocument aDocument)
    {
        return listDataOwnersReadyForCuration(getUser(aSessionOwner), aProject, aDocument);
    }

    @Transactional
    @Override
    public List<AnnotationSet> listDataOwnersReadyForCuration(User aSessionOwner, Project aProject,
            SourceDocument aDocument)
    {
        var deselected = listDeselectedDataOwners(aSessionOwner, aProject);

        return listCuratableUserNames(aDocument).stream() //
                .map(AnnotationSet::forUser) //
                .filter(dataOwner -> !deselected.contains(dataOwner)) //
                .toList();
    }

    @Transactional
    @Override
    public List<AnnotationSet> listCuratableDataOwners(SourceDocument aDocument)
    {
        var project = aDocument.getProject();
        var curatableUserNames = listCuratableUserNames(aDocument);

        var dataOwners = documentService.getDataOwners(project, curatableUserNames);
        return curatableUserNames.stream().map(dataOwners::get).toList();
    }

    /**
     * @return the usernames of the annotators that can be curated for the given document. Curation
     *         always targets the {@code CURATION_USER}, so the session owner's own annotations are
     *         curatable like everybody else's.
     */
    private List<String> listCuratableUserNames(SourceDocument aDocument)
    {
        return curationDocumentService.listCuratableUsers(aDocument).stream() //
                .map(User::getUsername) //
                .toList();
    }

    @Transactional
    @Override
    public boolean isShowAll(String aSessionOwner, Long aProjectId)
    {
        return isShowAll(getUser(aSessionOwner), projectService.getProject(aProjectId));
    }

    @Transactional
    @Override
    public boolean isShowAll(User aSessionOwner, Project aProject)
    {
        return loadPreferences(aSessionOwner, aProject).isShowAll();
    }

    @Transactional
    @Override
    public void setShowAll(String aSessionOwner, Long aProjectId, boolean aValue)
    {
        updatePreferences(getUser(aSessionOwner), projectService.getProject(aProjectId),
                prefs -> prefs.setShowAll(aValue));
    }

    @Transactional
    @Override
    public boolean isShowScore(String aSessionOwner, Long aProjectId)
    {
        return isShowScore(getUser(aSessionOwner), projectService.getProject(aProjectId));
    }

    @Transactional
    @Override
    public boolean isShowScore(User aSessionOwner, Project aProject)
    {
        return loadPreferences(aSessionOwner, aProject).isShowScore();
    }

    @Transactional
    @Override
    public void setShowScore(String aSessionOwner, Long aProjectId, boolean aValue)
    {
        updatePreferences(getUser(aSessionOwner), projectService.getProject(aProjectId),
                prefs -> prefs.setShowScore(aValue));
    }

    private Set<AnnotationSet> listDeselectedDataOwners(User aSessionOwner, Project aProject)
    {
        return loadPreferences(aSessionOwner, aProject).getDeselectedDataOwners().stream() //
                .map(AnnotationSet::forUser) //
                .collect(toCollection(LinkedHashSet::new));
    }

    /**
     * @return the stored curation settings, or the defaults if there are none - nobody deselected.
     */
    private CurationSessionPreferences loadPreferences(User aSessionOwner, Project aProject)
    {
        return preferencesService
                .loadOptionalTraitsForUserAndProject(KEY_CURATION_SESSION, aSessionOwner, aProject)
                .orElseGet(CurationSessionPreferences::new);
    }

    private void updatePreferences(User aSessionOwner, Project aProject,
            Consumer<CurationSessionPreferences> aUpdate)
    {
        var prefs = loadPreferences(aSessionOwner, aProject);
        aUpdate.accept(prefs);
        preferencesService.saveTraitsForUserAndProject(KEY_CURATION_SESSION, aSessionOwner,
                aProject, prefs);
    }

    /**
     * @throws IllegalStateException
     *             if the user does not exist - the session owner is always a logged-in user, so a
     *             missing one is a programming error that must not silently drop a change.
     */
    private User getUser(String aUsername)
    {
        var user = userRegistry.get(aUsername);
        if (user == null) {
            throw new IllegalStateException("User [" + aUsername + "] does not exist");
        }
        return user;
    }
}
