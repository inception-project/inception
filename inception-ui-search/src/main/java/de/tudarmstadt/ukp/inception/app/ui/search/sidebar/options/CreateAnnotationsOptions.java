/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options;

public class CreateAnnotationsOptions
    extends Options
{
    /**
     * Whether or not to override existing annotations of the same layer
     */
    private boolean overrideExistingAnnotations = false;

    public boolean isOverrideExistingAnnotations()
    {
        return overrideExistingAnnotations;
    }

    public void setOverrideExistingAnnotations(boolean aOverrideExistingAnnotations)
    {
        overrideExistingAnnotations = aOverrideExistingAnnotations;
    }
}
