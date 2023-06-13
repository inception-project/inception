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
package de.tudarmstadt.ukp.inception.workload.matrix.trait;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Trait class for matrix workload
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatrixWorkloadTraits
    implements Serializable
{
    private static final long serialVersionUID = 6984531953353384507L;

    private boolean reopenableByAnnotator = false;

    private boolean randomDocumentAccessAllowed = true;

    private boolean documentResetAllowed = true;

    public boolean isReopenableByAnnotator()
    {
        return reopenableByAnnotator;
    }

    public void setReopenableByAnnotator(boolean aReopenableByAnnotator)
    {
        reopenableByAnnotator = aReopenableByAnnotator;
    }

    public boolean isRandomDocumentAccessAllowed()
    {
        return randomDocumentAccessAllowed;
    }

    public void setRandomDocumentAccessAllowed(boolean aRandomDocumentAccessAllowed)
    {
        randomDocumentAccessAllowed = aRandomDocumentAccessAllowed;
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
