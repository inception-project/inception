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
package de.tudarmstadt.ukp.inception.kb.reification;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnumUserType;

/**
 * Implementation of {@link PersistentEnumUserType}
 *
 */
public class ReificationType
    extends PersistentEnumUserType<Reification>
{
    private static final long serialVersionUID = -5458676818846846515L;

    @Override
    public Class<Reification> returnedClass()
    {
        return Reification.class;
    }
}
