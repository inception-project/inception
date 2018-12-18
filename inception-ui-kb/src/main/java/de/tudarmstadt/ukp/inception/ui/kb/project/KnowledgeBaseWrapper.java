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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Wrapper class around {@link KnowledgeBase}.<br>
 * Purpose: any forms containing file upload fields (for adding files to local knowledge bases) need
 * a knowledge-base-like model object which can hold {@link File} objects. Similarly, a remote KB's
 * URL needs to be captured in a form. Since {@code KnowledgeBase} should have neither of those
 * attributes, this wrapper exists.
 */
public class KnowledgeBaseWrapper
    implements Serializable
{
    private static final long serialVersionUID = 4639345743242356537L;

    private KnowledgeBase kb;
    private String url;
    private final List<Pair<String, File>> files = new ArrayList<>();

    public KnowledgeBase getKb()
    {
        return kb;
    }

    public void setKb(KnowledgeBase aKB)
    {
        kb = aKB;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    /**
     * Record that the given file should be added to the KB.
     * 
     * @param aTitle
     *            the title used in error or success messages (e.g. the file name or the name of the
     *            KB profile).
     * @param aFile
     *            the file.
     */
    public void putFile(String aTitle, File aFile)
    {
        files.add(Pair.of(aTitle, aFile));
    }
    
    public List<Pair<String, File>> getFiles()
    {
        return Collections.unmodifiableList(files);
    }
    
    public void clearFiles()
    {
        files.clear();
    }
}
