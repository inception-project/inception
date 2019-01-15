/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

import org.apache.wicket.request.IRequestParameters;

public class Offset {

    private int begin;
    private int end;

    public Offset(int begin, int end)
    {
        this.begin = begin;
        this.end = end;
    }

    public Offset(IRequestParameters aPostParams)
    {
        begin = Integer.parseInt(aPostParams.getParameterValue("begin").toString());
        end = Integer.parseInt(aPostParams.getParameterValue("end").toString());
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }
}
