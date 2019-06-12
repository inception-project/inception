/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.event;

import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.event.HybridApplicationUIEvent;

public class RelationUpdateEvent extends ApplicationEvent implements HybridApplicationUIEvent
{
    private static final long serialVersionUID = -8621413642390759892L;
    
    private final SourceDocument document;
    private final String user;
    private final AnnotationFS targetAnno;
    private final AnnotationFS sourceAnno;

    public RelationUpdateEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationFS aTargetAnnotation, AnnotationFS aSourceAnnotation)
    {
        super(aSource);
        document = aDocument;
        user = aUser;
        targetAnno = aTargetAnnotation;
        sourceAnno = aSourceAnnotation;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getUser()
    {
        return user;
    }

    public AnnotationFS getTargetAnnotation()
    {
        return targetAnno;
    }
    
    public AnnotationFS getSourceAnno()
    {
        return sourceAnno;
    }

    @Override
    public String toString()
    {   
        StringBuilder builder = new StringBuilder();
        builder.append("RelationCreatedEvent [");
        if (document != null) {
            builder.append("docID=");
            builder.append(document.getId());
            builder.append(", user=");
            builder.append(user);
            builder.append(", ");
        }
        builder.append("relation=[");
        builder.append(targetAnno.getBegin());
        builder.append("-");
        builder.append(targetAnno.getEnd());
        builder.append("](");
        builder.append(targetAnno.getCoveredText());
        builder.append(") <-[");
        builder.append(sourceAnno.getBegin());
        builder.append("-");
        builder.append(sourceAnno.getEnd());
        builder.append("](");
        builder.append(sourceAnno.getCoveredText());
        builder.append(")]");
        return builder.toString();
    }

}
