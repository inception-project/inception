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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static java.util.Collections.unmodifiableList;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.model.RemoteRepositoryTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;

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

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBaseWrapper.class);

    private final List<Pair<String, File>> files = new ArrayList<>();

    private KnowledgeBase kb;
    private String url;
    private AuthenticationType authenticationType;
    private RemoteRepositoryTraits traits = new RemoteRepositoryTraits();

    public KnowledgeBaseWrapper()
    {
        // Nothing to do
    }

    public KnowledgeBaseWrapper(KnowledgeBase aKb)
    {
        setKb(aKb);
    }

    public KnowledgeBase getKb()
    {
        return kb;
    }

    public boolean isKbSaved()
    {
        return kb != null && kb.getRepositoryId() != null;
    }

    public void setKb(KnowledgeBase aKB)
    {
        kb = aKB;
        if (kb != null && kb.getType() != null) {
            switch (kb.getType()) {
            case LOCAL:
                // Local repos have no traits
                break;
            case REMOTE:
                traits = new RemoteRepositoryTraits();
                if (kb.getTraits() != null) {
                    try {
                        traits = fromJsonString(RemoteRepositoryTraits.class, kb.getTraits());
                    }
                    catch (IOException e) {
                        LOG.error("Unable to read traits - resetting them", e);
                    }
                }
                if (traits.getAuthentication() != null) {
                    authenticationType = traits.getAuthentication().getType();
                }
                else {
                    authenticationType = null;
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported knowledge base type [" + kb.getType() + "]");
            }
        }
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    public RemoteRepositoryTraits getTraits()
    {
        return traits;
    }

    public void setTraits(RemoteRepositoryTraits aTraits)
    {
        traits = aTraits;
    }

    public AuthenticationType getAuthenticationType()
    {
        return authenticationType;
    }

    public void setAuthenticationType(AuthenticationType aAuthenticationType)
    {
        authenticationType = aAuthenticationType;
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
        return unmodifiableList(files);
    }

    public void clearFiles()
    {
        files.clear();
    }
}
