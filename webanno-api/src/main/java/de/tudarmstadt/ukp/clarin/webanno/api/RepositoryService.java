/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * This interface contains methods that are related to accessing/creating/deleting... documents,
 * Users, and Projects for the annotation system. while meta data about documents and projects and
 * users are stored in the database, source and annotation documents are stored in a file system
 */
public interface RepositoryService
    extends ProjectService, ImportExportService, ConstraintsService, DocumentService,
    CorrectionDocumentService, CurationDocumentService, SettingsService
{
    // --------------------------------------------------------------------------------------------
    // Methods related to permissions
    // --------------------------------------------------------------------------------------------

    /**
     * Returns a role of a user, globally we will have ROLE_ADMIN and ROLE_USER
     *
     * @param user
     *            the {@link User} object
     * @return the roles.
     */
    List<Authority> listAuthorities(User user);
    
    void uploadTrainingDocument(File aFile, SourceDocument aDocument)
            throws IOException;
}
