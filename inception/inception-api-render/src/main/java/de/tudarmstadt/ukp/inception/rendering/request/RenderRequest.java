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
package de.tudarmstadt.ukp.inception.rendering.request;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class RenderRequest
{
    private final AnnotatorState state;
    private final SourceDocument sourceDocument;
    private final User annotationUser;
    private final int windowBeginOffset;
    private final int windowEndOffset;
    private final boolean includeText;
    private final List<AnnotationLayer> allLayers;
    private final List<AnnotationLayer> visibleLayers;
    private final CAS cas;
    private final ColoringStrategy coloringStrategyOverride;

    private RenderRequest(Builder builder)
    {
        this.windowBeginOffset = builder.windowBeginOffset;
        this.windowEndOffset = builder.windowEndOffset;
        this.includeText = builder.includeText;
        this.state = builder.state;
        this.sourceDocument = builder.sourceDocument;
        this.annotationUser = builder.annotationUser;
        this.cas = builder.cas;
        this.allLayers = builder.allLayers;
        this.visibleLayers = builder.visibleLayers;
        this.coloringStrategyOverride = builder.coloringStrategyOverride;
    }

    public Optional<ColoringStrategy> getColoringStrategyOverride()
    {
        return Optional.ofNullable(coloringStrategyOverride);
    }

    public int getWindowBeginOffset()
    {
        return windowBeginOffset;
    }

    public int getWindowEndOffset()
    {
        return windowEndOffset;
    }

    public boolean isIncludeText()
    {
        return includeText;
    }

    public User getAnnotationUser()
    {
        return annotationUser;
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public Project getProject()
    {
        return sourceDocument.getProject();
    }

    public CAS getCas()
    {
        return cas;
    }

    public List<AnnotationLayer> getAllLayers()
    {
        return allLayers;
    }

    public List<AnnotationLayer> getVisibleLayers()
    {
        return visibleLayers;
    }

    /**
     * @deprecated We want to minimize the state information carried around in the render request,
     *             so better not use the full annotator state and instead add relevant information
     *             as fields to the render request itself.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public AnnotatorState getState()
    {
        return state;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private int windowBeginOffset;
        private int windowEndOffset;
        private boolean includeText = true;
        private AnnotatorState state;
        private SourceDocument sourceDocument;
        private User annotationUser;
        private CAS cas;
        private List<AnnotationLayer> allLayers;
        private List<AnnotationLayer> visibleLayers;
        private ColoringStrategy coloringStrategyOverride;

        private Builder()
        {
            // No instances without a CAS!
        }

        public Builder withCas(CAS aCas)
        {
            cas = aCas;
            return this;
        }

        /**
         * @deprecated See {@link RenderRequest#getState}
         */
        @SuppressWarnings("javadoc")
        @Deprecated
        public Builder withState(AnnotatorState aState)
        {
            state = aState;
            allLayers = aState.getAllAnnotationLayers();
            visibleLayers = aState.getAnnotationLayers();
            sourceDocument = state.getDocument();
            annotationUser = state.getUser();
            return this;
        }

        public Builder withDocument(SourceDocument aDocument, User aUser)
        {
            sourceDocument = aDocument;
            annotationUser = aUser;
            return this;
        }

        public Builder withText(boolean aIncludeText)
        {
            includeText = aIncludeText;
            return this;
        }

        public Builder withWindow(int aBegin, int aEnd)
        {
            this.windowBeginOffset = aBegin;
            this.windowEndOffset = aEnd;
            return this;
        }

        public Builder withVisibleLayers(List<AnnotationLayer> aLayers)
        {
            visibleLayers = aLayers;
            return this;
        }

        public Builder withColoringStrategyOverride(ColoringStrategy aColoringStrategyOverride)
        {
            coloringStrategyOverride = aColoringStrategyOverride;
            return this;
        }

        public RenderRequest build()
        {
            return new RenderRequest(this);
        }
    }
}
