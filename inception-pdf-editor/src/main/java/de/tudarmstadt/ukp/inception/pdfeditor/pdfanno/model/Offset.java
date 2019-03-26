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

import java.io.Serializable;
import java.util.Objects;

import org.apache.wicket.request.IRequestParameters;

public class Offset implements Serializable
{

    private static final long serialVersionUID = -6076735271434213572L;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offset offset = (Offset) o;
        return begin == offset.begin &&
            end == offset.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

    @Override
    public String toString() {
        return "Offset{" +
            "begin=" + begin +
            ", end=" + end +
            '}';
    }
}
