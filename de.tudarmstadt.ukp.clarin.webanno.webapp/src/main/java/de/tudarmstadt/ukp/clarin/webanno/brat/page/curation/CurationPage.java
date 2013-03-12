/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation;


import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationDocumentVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 *
 * @author Andreas Straninger
 * This is the main class for the curation page. It contains an
 * interface which displays differences between user annotations
 * for a specific document. The interface provides a tool for
 * merging these annotations and storing them as a new annotation.
 */
public class CurationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private AjaxLink<Void> reload1;

    private AjaxLink<Void> reload2;

    private BratAnnotationDocumentVisualizer embedder1;

    private BratAnnotationDocumentVisualizer embedder2;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    public CurationPage()
    {

        // do it if the user is an admin or a curator
    	// get first sourceDocument from project repository
    	// TODO integrate open dialog
    	List<Project> projects = repository.listProjects();
    	SourceDocument sourceDocument = null;
    	for (Project project : projects) {
    	    if(isAllowed(project) && project.getName().equals("NER_V1")) {
			List<SourceDocument> sourceDocuments = repository.listSourceDocuments(project);
				for (SourceDocument sourceDocument2 : sourceDocuments) {
					if(sourceDocument2.getName().equals("NER_deu_blocks0K2-ab.tcf")) {
						sourceDocument = sourceDocument2;
						break;
					}
				}
				if(sourceDocument != null) {
					break;
				}
    	    }
		}
    	

    	// transform jcas to objects for wicket components
    	CurationBuilder builder = new CurationBuilder(repository);
    	CurationContainer curationContainer = builder.buildCurationContainer(sourceDocument);
    	curationContainer.setSourceDocument(sourceDocument);
//        getSession().setAttribute("dummy", curationContainer);

    	// add panel
        add(new CurationPanel("curationPanel", curationContainer));

    }

    /**
     * Fetch a random annotation document for visualization
     */
    public boolean isAllowed(Project aProject)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = repository.getUser(username);
        boolean roleAdmin = false;
        List<Authority> authorities = repository.getAuthorities(user);
        for (Authority authority : authorities) {
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean admin = false;
        if (!roleAdmin) {
            try {
                if (repository.getPermisionLevel(user, aProject).equals("curator")) {
                    admin = true;
                }
            }
            catch (NoResultException ex) {
                error("No permision is given to this user " + ex);
            }
        }

        return (admin || roleAdmin);
    }
}
