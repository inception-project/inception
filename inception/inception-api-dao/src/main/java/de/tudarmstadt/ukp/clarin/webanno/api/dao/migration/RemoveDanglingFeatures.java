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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.migration;

import static java.lang.String.format;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * WebAnno 4.0.0-beta-21 introduces a DB-level foreign key relation from
 * {@code AnnotationFeature.layer} to {@code AnnotationLayer.id}. Due to bugs in the code, it might
 * be that there are existing {@code AnnotationFeature} entries in the DB which point to
 * non-existing layers. These features are invalid and prevent the foreign key relation from being
 * able to be created. Thus, such features must be removed before the database schema is updated.
 */
public class RemoveDanglingFeatures
    implements CustomTaskChange
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private int changed = 0;

    @Override
    public String getConfirmationMessage()
    {
        return format("[%d] dangling features were removed.", changed);
    }

    @Override
    public void setFileOpener(ResourceAccessor aArg0)
    {
        // Nothing to do
    }

    @Override
    public void setUp() throws SetupException
    {
        changed = 0;
    }

    @Override
    public ValidationErrors validate(Database aArg0)
    {
        // Nothing to do
        return null;
    }

    @Override
    public void execute(Database aDatabase) throws CustomChangeException
    {
        JdbcConnection dbConn = (JdbcConnection) aDatabase.getConnection();
        try {
            PreparedStatement selection = dbConn.prepareStatement(String.join("\n", //
                    "SELECT f.id", //
                    "FROM annotation_feature f", //
                    "LEFT JOIN annotation_type l ON f.annotation_type = l.id \n", //
                    "WHERE l.id IS NULL"));

            PreparedStatement deletion = dbConn
                    .prepareStatement("DELETE FROM annotation_feature WHERE id = ?");

            try (ResultSet danglingFeatures = selection.executeQuery()) {
                while (danglingFeatures.next()) {
                    long danglingFeatureId = danglingFeatures.getLong(1);

                    deletion.setLong(1, danglingFeatureId);
                    deletion.execute();

                    changed++;
                }
            }

            if (changed > 0) {
                log.info("DATABASE UPGRADE PERFORMED: [{}] dangling features were removed.",
                        changed);
            }
        }
        catch (Exception e) {
            // swallow the exception !
        }
    }
}
