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
package de.tudarmstadt.ukp.inception.workload.dynamic.trait;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;

import java.io.Serializable;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;

/**
 * Trait class for dynamic workload
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicWorkloadTraits
    implements Serializable
{
    private static final long serialVersionUID = 7558423338392462923L;

    private String workflowType;
    private int defaultNumberOfAnnotations;
    private boolean confirmFinishingDocuments = true;
    private boolean documentResetAllowed = true;

    private Duration abandonationTimeout;
    private AnnotationDocumentState abandonationState = IGNORE;

    public DynamicWorkloadTraits()
    {
        workflowType = DefaultWorkflowExtension.DEFAULT_WORKFLOW;
        defaultNumberOfAnnotations = 3;
    }

    public DynamicWorkloadTraits(String aWorkflowType, int aDefaultNumberOfAnnotations)
    {
        workflowType = aWorkflowType;
        defaultNumberOfAnnotations = aDefaultNumberOfAnnotations;
    }

    public int getDefaultNumberOfAnnotations()
    {
        return defaultNumberOfAnnotations;
    }

    public void setDefaultNumberOfAnnotations(int aDefaultNumberOfAnnotations)
    {
        defaultNumberOfAnnotations = aDefaultNumberOfAnnotations;
    }

    public String getWorkflowType()
    {
        return workflowType;
    }

    public void setWorkflowType(String aWorkflowType)
    {
        this.workflowType = aWorkflowType;
    }

    public Duration getAbandonationTimeout()
    {
        if (abandonationTimeout == null) {
            return Duration.ofMinutes(0);
        }

        return abandonationTimeout;
    }

    public void setAbandonationTimeout(Duration aAbandonationTimeout)
    {
        abandonationTimeout = aAbandonationTimeout;
    }

    public AnnotationDocumentState getAbandonationState()
    {
        if (abandonationState == null) {
            return AnnotationDocumentState.IGNORE;
        }

        return abandonationState;
    }

    public void setAbandonationState(AnnotationDocumentState aAbandonationState)
    {
        abandonationState = aAbandonationState;
    }

    public boolean isConfirmFinishingDocuments()
    {
        return confirmFinishingDocuments;
    }

    public void setConfirmFinishingDocuments(boolean aConfirmFinishingDocuments)
    {
        confirmFinishingDocuments = aConfirmFinishingDocuments;
    }

    public boolean isDocumentResetAllowed()
    {
        return documentResetAllowed;
    }

    public void setDocumentResetAllowed(boolean aDocumentResetAllowed)
    {
        documentResetAllowed = aDocumentResetAllowed;
    }
}
