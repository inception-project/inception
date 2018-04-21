/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.validation.validator.UrlValidator;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.rio.RDFParseException;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class EnrichedKnowledgeBaseUtils
{
    public static final UrlValidator URL_VALIDATOR = new UrlValidator(
            new String[] { "http", "https" });

    /**
     * Handles creation/updating of knowledge bases which is necessary either when creating or
     * editing a new knowledge base in the project settings. TODO This utility class is ugly. In
     * fact, the whole EKB thing is ugly, but it works for now.
     */
    public static final void registerEkb(EnrichedKnowledgeBase ekb, KnowledgeBaseService kbService)
            throws Exception {
        KnowledgeBase kb = ekb.getKb();
        setKnowledgeBaseFields(ekb);

        // set up the repository config, then register the knowledge base
        RepositoryImplConfig cfg;
        switch (kb.getType()) {
        case LOCAL:
            cfg = kbService.getNativeConfig();
            kbService.registerKnowledgeBase(kb, cfg);
            importFiles(ekb, kbService);
            break;
        case REMOTE:
            cfg = kbService.getRemoteConfig(ekb.getUrl());
            kbService.registerKnowledgeBase(kb, cfg);
            break;
        default:
            throw new IllegalStateException();
        }
    }
    
    public static final void updateEkb(EnrichedKnowledgeBase ekb, RepositoryImplConfig cfg,
            KnowledgeBaseService kbService) throws Exception {
        setKnowledgeBaseFields(ekb);
        KnowledgeBase kb = ekb.getKb();
        kbService.updateKnowledgeBase(kb, cfg);
        if (kb.getType() == RepositoryType.LOCAL) {
            importFiles(ekb, kbService);
        }
    }

    private static final void setKnowledgeBaseFields(EnrichedKnowledgeBase ekb) {
        KnowledgeBase kb = ekb.getKb();
        ValueFactory factory = SimpleValueFactory.getInstance();
        kb.setClassIri(factory.createIRI(ekb.getClassIri()));
        kb.setSubclassIri(factory.createIRI(ekb.getSubclassIri()));
        kb.setTypeIri(factory.createIRI(ekb.getTypeIri()));
        kb.setEnabled(ekb.isEnabled());
        kb.setSupportConceptLinking(ekb.isSupportConceptLinking());
        kb.setReification(ekb.getReification());
    }

    private static final void importFiles(EnrichedKnowledgeBase ekb,
                                          KnowledgeBaseService kbService) throws Exception {
        KnowledgeBase kb = ekb.getKb();
        for (File f : ekb.getFiles()) {
            try (InputStream is = new FileInputStream(f)) {
                kbService.importData(kb, f.getName(), is);
            } catch (IOException | RDFParseException | RepositoryException e) {
                throw new Exception(e);
            }
        }
    }
}
