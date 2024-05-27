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

import static de.tudarmstadt.ukp.inception.support.SettingsUtil.PROP_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;

public class EmbeddedDatabaseBackupHandler
{
    private static final String UNKNOWN_VERSION = "unknown-version";
    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    public void maybeBackupEmbeddedDatabase() throws IOException
    {
        var appHome = SettingsUtil.getApplicationHome();

        var embeddedDbFolder = new File(appHome, "db");

        if (embeddedDbFolder.exists()) {
            var lastKnownVersion = getPreviouslyStartedVersion();
            var currentVersion = getCurrentVersion();

            if (!Objects.equals(lastKnownVersion, currentVersion)) {
                LOG.info("Current version [{}] is different from the version last started " //
                        + "[{}] - triggering database backup", currentVersion, lastKnownVersion);
                backupDatabase(embeddedDbFolder, lastKnownVersion);
            }
            else {
                LOG.debug("Current version [{}] is the same as the version last started " //
                        + "[{}] - no database backup triggered", currentVersion, lastKnownVersion);
            }
        }
        else {
            // If there is no DB, it might not be there yet (because it is the first time we start)
            // or the instance is using an external database. In both cases, we have nothing to do.
            LOG.debug("Instance has no embedded database - so not triggering a backup");
        }

        writeCurrentVersion();
    }

    private String getCurrentVersion()
    {
        var props = SettingsUtil.getVersionProperties();

        String version = props.getProperty(PROP_VERSION);
        if ("unknown".equals(version)) {
            return UNKNOWN_VERSION;
        }

        return props.getProperty(PROP_VERSION);
    }

    private void writeCurrentVersion() throws IOException
    {
        var appHome = SettingsUtil.getApplicationHome();
        var currentVersion = getCurrentVersion();
        File versionFile = new File(appHome, "version");
        FileUtils.write(versionFile, currentVersion, UTF_8);
    }

    private void backupDatabase(File embeddedDbFolder, String lastKnownVersion) throws IOException
    {
        var appHome = SettingsUtil.getApplicationHome();

        File dbBackupFolder = new File(appHome, "db-backup");
        if (!dbBackupFolder.exists()) {
            dbBackupFolder.mkdirs();
            var notice = String.join("\n", //
                    "DATABASE BACKUP FOLDER", //
                    "======================", //
                    "", //
                    "This folder contains backups of the embedded database folder ('db'). These",
                    "backups are created when the application detect that is was upgraded from a",
                    "previous version.", //
                    "", //
                    "These backups are mainly meant for an emergency recovery of an embedded if ",
                    "the database migration scripts failed performing a database upgrade, if the ",
                    "database was corrupted due to a system crash, power loss or similary",
                    "catastrophic event.", //
                    "", //
                    "Mind that these are backups *only* of the embedded database which *does not*",
                    "contain the annotations themselves, only the project metadata. Thus, if you",
                    "replace a broken 'db' folder with the contents of one of these backups, note",
                    "that e.g. any documents that were added/removed since the backup may appear",
                    "to be lost/restored, but the actual data and annotations of these documents",
                    "may actually (not) be present in the 'repository' folder.", //
                    "", //
                    "Be sure to backup the 'db' folder once more yourself before trying to recover",
                    "your system by overwriting it with the contents of one of these backups.", "", //
                    "These backups are not used by the system itself. If you do not want to keep",
                    "them, you can simply delete them.", //
                    "", //
                    "THESE BACKUPS DO NOT REPLACE A PROPER BACKUP THAT YOU SHOULD REGULARLY MAKE",
                    "OF ANY IMPORTANT DATA! It is recommended to regularly export backup archives",
                    "of important annotation projects and/or to regularly fully backup the",
                    "application database and data folders.");
            FileUtils.write(new File(dbBackupFolder, "README.txt"), notice, UTF_8);
        }

        File dbBackupFile = new File(dbBackupFolder, "db-backup-" + lastKnownVersion + ".zip");

        LOG.info("Embedded database backup starting. This might take a moment...");
        LOG.info("Writing database backup to: {}", dbBackupFile);

        ZipUtils.zipFolder(embeddedDbFolder, dbBackupFile);

        LOG.info("Embedded database backup completed");
    }

    /**
     * @return which was the last version of INCEpTION that was started. If no version file exists,
     *         it will be "unknown".
     * @throws IOException
     *             if there was a problem reading the previous version tag file.
     */
    private String getPreviouslyStartedVersion() throws IOException
    {
        var appHome = SettingsUtil.getApplicationHome();
        File versionFile = new File(appHome, "version");
        if (!versionFile.exists()) {
            return UNKNOWN_VERSION;
        }

        var version = readFileToString(versionFile, UTF_8);

        if (!Pattern.matches("[-.0-9a-zA-Z]+", version)) {
            LOG.warn(
                    "Illegal last-started version. Considering last started version to be unknown.");
            return UNKNOWN_VERSION;
        }

        return version;
    }
}
