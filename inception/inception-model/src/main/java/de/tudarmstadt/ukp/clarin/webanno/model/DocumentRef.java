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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;

/**
 * An immutable snapshot of the identifying properties of a {@link SourceDocument}, for use where
 * holding on to the entity is not appropriate.
 *
 * @param project
 *            the project the document belongs to.
 * @param id
 *            the ID of the document.
 * @param name
 *            the name of the document.
 */
public record DocumentRef(ProjectRef project, long id, String name)
    implements Serializable
{
    public static DocumentRef of(SourceDocument aDocument)
    {
        return new DocumentRef(ProjectRef.of(aDocument.getProject()), aDocument.getId(),
                aDocument.getName());
    }
}
