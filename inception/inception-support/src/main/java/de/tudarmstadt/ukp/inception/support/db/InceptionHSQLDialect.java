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
package de.tudarmstadt.ukp.inception.support.db;

import org.hibernate.dialect.HSQLDialect;

public class InceptionHSQLDialect
    extends HSQLDialect
{
    @Override
    public int getMaxVarcharCapacity()
    {
        // Liquibase/HSQLDB treats LONGTEXT which we use e.g. for the project description as CLOB.
        // But Hibernate will not accept a CLOB type if we use `@Column(length = 64000)`. To fix
        // this, we claim that HSQLDB VARCHAR cannot carry more than 255 chars which will cause
        // Hibernate to fall back to CLOB.
        //
        // This seems a hack but it is easier right now than updating/extending the Liquibase
        // changesets.
        return 255;
    }
}
