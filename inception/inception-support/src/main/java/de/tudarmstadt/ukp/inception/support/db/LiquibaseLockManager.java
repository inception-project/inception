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

import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateTimeInstance;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.LockService;
import liquibase.lockservice.LockServiceFactory;

public class LiquibaseLockManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String PROP_FORCE_RELEASE_LOCK = "forceReleaseLock";

    private final DataSource dataSource;

    public LiquibaseLockManager(DataSource aDataSource)
        throws LockException, NotLockedException, LockRemovedException
    {
        dataSource = aDataSource;

        try {
            List<DatabaseChangeLogLock> locks = listLocks();

            boolean forceRemoveLock = toBoolean(
                    System.getProperty(PROP_FORCE_RELEASE_LOCK, "false"));

            if (locks.isEmpty() && forceRemoveLock) {
                throw new NotLockedException(
                        "The database is not locked. Please remove the argument '-D"
                                + PROP_FORCE_RELEASE_LOCK + "=true' and restart.");
            }

            if (!locks.isEmpty()) {
                if (!forceRemoveLock) {
                    DatabaseChangeLogLock lock = locks.get(0);
                    throw new LockException(
                            "Could not acquire change log lock. Currently locked by "
                                    + lock.getLockedBy() + " since "
                                    + getDateTimeInstance(SHORT, SHORT)
                                            .format(lock.getLockGranted()));
                }

                forceReleaseLock();

                throw new LockRemovedException(
                        "The database lock has been removed. Please remove the argument '-D"
                                + PROP_FORCE_RELEASE_LOCK + "=true' and restart.");
            }
        }
        catch (LockException | NotLockedException | LockRemovedException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Unable to list Liquibase locks: ", e.getMessage(), e);
        }
    }

    public List<DatabaseChangeLogLock> listLocks()
        throws DatabaseException, LockException, SQLException
    {
        Database database = null;
        try {
            Connection c = dataSource.getConnection();

            DatabaseConnection liquibaseConnection = new JdbcConnection(c);

            database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(liquibaseConnection);

            LockService lockService = LockServiceFactory.getInstance().getLockService(database);
            return unmodifiableList(asList(lockService.listLocks()));
        }
        finally {
            if (database != null) {
                database.close();
            }
        }
    }

    public void forceReleaseLock() throws DatabaseException, LockException, SQLException
    {
        Database database = null;
        try {
            Connection c = dataSource.getConnection();

            DatabaseConnection liquibaseConnection = new JdbcConnection(c);

            database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(liquibaseConnection);

            LockService lockService = LockServiceFactory.getInstance().getLockService(database);
            lockService.forceReleaseLock();
        }
        finally {
            if (database != null) {
                database.close();
            }
        }
    }
}
