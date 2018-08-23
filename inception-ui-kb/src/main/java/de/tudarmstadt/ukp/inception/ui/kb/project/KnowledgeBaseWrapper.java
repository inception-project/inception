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
import java.io.Serializable;
import java.util.List;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.rio.RDFParseException;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Wrapper class around {@link KnowledgeBase}.<br>
 * Purpose: any forms containing file upload fields (for adding files to local knowledge bases) need
 * a knowledge-base-like model object which can hold {@link File} objects. Similarly, a remote KB's
 * URL needs to be captured in a form. Since {@code KnowledgeBase} should have neither of those
 * attributes, this wrapper exists.
 */
public class KnowledgeBaseWrapper implements Serializable {

    private static final long serialVersionUID = 4639345743242356537L;

    private KnowledgeBase kb;
    private String url;
    private List<File> files;

    public KnowledgeBase getKb() {
        return kb;
    }
    
    public void setKb(KnowledgeBase kb) {
        this.kb = kb;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public static final void updateKb(KnowledgeBaseWrapper kbw, RepositoryImplConfig cfg,
            KnowledgeBaseService kbService) throws Exception {
        KnowledgeBase kb = kbw.getKb();
        kbService.updateKnowledgeBase(kb, cfg);
        if (kb.getType() == RepositoryType.LOCAL) {
            kbService.defineBaseProperties(kb);
            KnowledgeBaseWrapper.importFiles(kbw, kbService);
        }
    }

    /**
     * Handles creation/updating of knowledge bases which is necessary either when creating or
     * editing a new knowledge base in the project settings.
     */
    public static final void registerKb(KnowledgeBaseWrapper kbw, KnowledgeBaseService kbService)
            throws Exception {
        KnowledgeBase kb = kbw.getKb();
    
        // set up the repository config, then register the knowledge base
        RepositoryImplConfig cfg;
        switch (kb.getType()) {
        case LOCAL:
            cfg = kbService.getNativeConfig();
            kbService.registerKnowledgeBase(kb, cfg);
            kbService.defineBaseProperties(kb);
            KnowledgeBaseWrapper.importFiles(kbw, kbService);
            kbService.indexLocalKb(kb);
            break;
        case REMOTE:
            cfg = kbService.getRemoteConfig(kbw.getUrl());
            kbService.registerKnowledgeBase(kb, cfg);
            break;
        default:
            throw new IllegalStateException();
        }
    }
    

    private static final void importFiles(KnowledgeBaseWrapper kbw,
                                          KnowledgeBaseService kbService) throws Exception {
        KnowledgeBase kb = kbw.getKb();
        for (File f : kbw.getFiles()) {
            try (InputStream is = new FileInputStream(f)) {
                kbService.importData(kb, f.getName(), is);
            } catch (IOException | RDFParseException | RepositoryException e) {
                throw new Exception(e);
            }
        }
    }

}
