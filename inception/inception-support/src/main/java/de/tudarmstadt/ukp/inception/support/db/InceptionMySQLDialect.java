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

import static org.hibernate.type.SqlTypes.BOOLEAN;

import org.hibernate.dialect.MySQLDialect;

public class InceptionMySQLDialect
    extends MySQLDialect
{
    @Override
    protected String columnType(int sqlTypeCode)
    {
        switch (sqlTypeCode) {
        // In our Liquibase scripts, we create boolean columns using the "BOOLEAN" type which seems
        // to be converted to "TINYINT(1)" by MySQL. However, Hibernate expects MySQL to be using
        // "BIT(1)" instead.
        case BOOLEAN:
            return "tinyint(1)";
        default:
            return super.columnType(sqlTypeCode);
        }
    }
}
