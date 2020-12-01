/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.workload.matrix.trait;

import java.io.Serializable;

/**
 * Trait class for default matrix workload
 */
public class MatrixWorkloadTrait
    implements Serializable
{
    private static final long serialVersionUID = 6984531953353384507L;
    private static final String MATRIX_WORKLOAD_TRAIT = "matrix";

    private int defaultNumberOfAnnotations;

    public MatrixWorkloadTrait()
    {
    }

    public String getType()
    {
        return MATRIX_WORKLOAD_TRAIT;
    }

    public int getDefaultNumberOfAnnotations()
    {
        return defaultNumberOfAnnotations;
    }

    public void setDefaultNumberOfAnnotations(int defaultNumberOfAnnotations)
    {
        this.defaultNumberOfAnnotations = defaultNumberOfAnnotations;
    }
}
