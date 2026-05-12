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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PubAnnotationTrack
{
    private String project;

    private List<PubAnnotationDenotation> denotations;

    private List<PubAnnotationRelation> relations;

    private List<PubAnnotationAttribute> attributes;

    public String getProject()
    {
        return project;
    }

    public void setProject(String aProject)
    {
        project = aProject;
    }

    public List<PubAnnotationDenotation> getDenotations()
    {
        return denotations;
    }

    public void setDenotations(List<PubAnnotationDenotation> aDenotations)
    {
        denotations = aDenotations;
    }

    public List<PubAnnotationRelation> getRelations()
    {
        return relations;
    }

    public void setRelations(List<PubAnnotationRelation> aRelations)
    {
        relations = aRelations;
    }

    public List<PubAnnotationAttribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(List<PubAnnotationAttribute> aAttributes)
    {
        attributes = aAttributes;
    }
}
