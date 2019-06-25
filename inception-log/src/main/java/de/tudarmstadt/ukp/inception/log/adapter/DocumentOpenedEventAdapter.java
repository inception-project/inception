/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.log.adapter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Component
public class DocumentOpenedEventAdapter
    implements EventLoggingAdapter<DocumentOpenedEvent>
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof DocumentOpenedEvent;
    }
    
    @Override
    public long getDocument(DocumentOpenedEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }
    
    @Override
    public long getProject(DocumentOpenedEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getDetails(DocumentOpenedEvent aEvent)
    {
        DocumentOpenedDetails details = new DocumentOpenedDetails(aEvent.getAnnotator(),
                aEvent.getUser(), aEvent.getDocument());
        try {
            String json = JSONUtil.toJsonString(details); 
            return json;
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
    }

    @Override
    public String getUser(DocumentOpenedEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getAnnotator(DocumentOpenedEvent aEvent)
    {
        return aEvent.getAnnotator();
    }

    private class DocumentOpenedDetails
    {
        private final String annotator;
        private final String opener;
        private final String docName;
        
        public DocumentOpenedDetails(String aAnnotator, String aOpener, SourceDocument aDoc) {
            annotator = aAnnotator;
            opener = aOpener;
            docName = aDoc.getName();
        }

        public String getAnnotator()
        {
            return annotator;
        }

        public String getOpener()
        {
            return opener;
        }

        public String getDocName()
        {
            return docName;
        }
    }
}
