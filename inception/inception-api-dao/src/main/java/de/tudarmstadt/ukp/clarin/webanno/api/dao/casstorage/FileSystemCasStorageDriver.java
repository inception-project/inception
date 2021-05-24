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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setDocumentId;
import static de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasMetadataUtils.failOnConcurrentModification;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

public class FileSystemCasStorageDriver
    implements CasStorageDriver
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RepositoryProperties repositoryProperties;
    private final BackupProperties backupProperties;

    public FileSystemCasStorageDriver(RepositoryProperties aRepositoryProperties,
            BackupProperties aBackupProperties)
    {
        repositoryProperties = aRepositoryProperties;
        backupProperties = aBackupProperties;

        if (backupProperties.getInterval() > 0) {
            log.info("CAS backups enabled - interval: {}sec  max-backups: {}  max-age: {}sec",
                    backupProperties.getInterval(), backupProperties.getKeep().getNumber(),
                    backupProperties.getKeep().getTime());
        }
        else {
            log.info("CAS backups disabled");
        }
    }

    @Override
    public CAS readUnmanagedCas(SourceDocument aDocument, String aUser) throws IOException
    {
        File casFile = getCasFile(aDocument.getProject().getId(), aDocument.getId(), aUser);
        File oldCasFile = new File(casFile.getPath() + ".old");

        String msgOldExists = "";
        if (oldCasFile.exists()) {
            msgOldExists = String.format(
                    "Existance of temporary annotation file [%s] indicates that a previous "
                            + "annotation storage process did not successfully complete. Contact "
                            + "your server administator and request renaming the '.ser.old' file "
                            + "to '.ser' manually on the command line. Advise the administrator to "
                            + "check for sufficient disk space and that the application has the "
                            + "necessary permissions to save files in its data folder.",
                    oldCasFile);
        }

        CAS cas;
        try {
            cas = WebAnnoCasUtil.createCas();
        }
        catch (UIMAException e) {
            throw new IOException("Unable to create empty CAS", e);
        }

        if (!casFile.exists()) {
            throw new FileNotFoundException("Annotation document of user [" + aUser
                    + "] for source document [" + aDocument.getName() + "] (" + aDocument.getId()
                    + ") not found in project [" + aDocument.getProject().getName() + "] ("
                    + aDocument.getProject().getId() + "). " + msgOldExists);
        }

        try {
            CasPersistenceUtils.readSerializedCas(cas, casFile);
            // Add/update the CAS metadata
            CasMetadataUtils.addOrUpdateCasMetadata(cas, casFile.lastModified(), aDocument, aUser);
        }
        catch (Exception e) {
            throw new IOException("Annotation document of user [" + aUser
                    + "] for source document [" + aDocument.getName() + "] (" + aDocument.getId()
                    + ") in project [" + aDocument.getProject().getName() + "] ("
                    + aDocument.getProject().getId() + ") cannot be read from file [" + casFile
                    + "]. " + msgOldExists, e);
        }

        return cas;
    }

    @Override
    public void realWriteCas(SourceDocument aDocument, String aUserName, CAS aCas)
        throws IOException
    {
        long t0 = currentTimeMillis();

        log.debug("Preparing to update annotations for user [{}] on document [{}]({}) " //
                + "in project [{}]({})", aUserName, aDocument.getName(), aDocument.getId(),
                aDocument.getProject().getName(), aDocument.getProject().getId());

        File annotationFolder = getAnnotationFolder(aDocument);
        File currentVersion = new File(annotationFolder, aUserName + ".ser");
        File oldVersion = new File(annotationFolder, aUserName + ".ser.old");

        // Check if there was a concurrent change to the file on disk
        if (currentVersion.exists()) {
            failOnConcurrentModification(aCas, currentVersion, aDocument, aUserName);
        }

        // Save current version
        try {
            // Make a backup of the current version of the file before overwriting
            if (currentVersion.exists()) {
                move(currentVersion.toPath(), oldVersion.toPath());
            }

            // Now write the new version to "<username>.ser" or CURATION_USER.ser
            long start = currentTimeMillis();
            setDocumentId(aCas, aUserName);
            writeSerializedCas(aCas, currentVersion);
            long duration = currentTimeMillis() - start;

            log.debug(
                    "Updated annotations for user [{}] on document [{}]({}) in project [{}]({}) in {}ms",
                    aUserName, aDocument.getName(), aDocument.getId(),
                    aDocument.getProject().getName(), aDocument.getProject().getId(), duration);
        }
        catch (Exception e) {
            log.error("There was an error while trying to write the CAS to [" + currentVersion
                    + "] - additional messages follow.");
            // If this is the first version, there is no old version, so do not restore anything
            if (!oldVersion.exists()) {
                log.warn("There is no old version to restore - leaving the current version which "
                        + "may be corrupt: [{}]", currentVersion);
                // Now abort anyway
                throw e;
            }

            log.error("Restoring previous annotations for user [{}] on document [{}]({}) in " //
                    + "project [{}]({}) due exception when trying to write new "
                    + "annotations: [{}]", aUserName, aDocument.getName(), aDocument.getId(),
                    aDocument.getProject().getName(), aDocument.getProject().getId(), oldVersion);
            try {
                move(oldVersion.toPath(), currentVersion.toPath(), REPLACE_EXISTING);
            }
            catch (Exception ex) {
                log.error("Unable to restore previous annotations: [{}]", oldVersion, ex);
            }

            // Now abort anyway
            throw e;
        }

        if (oldVersion.exists() && (currentVersion.length() < oldVersion.length())) {
            log.debug(
                    "Annotations truncated for user [{}] on document [{}]({}) in project "
                            + "[{}]({}): {} -> {} bytes ({} bytes removed)",
                    aUserName, aDocument.getName(), aDocument.getId(),
                    aDocument.getProject().getName(), aDocument.getProject().getId(),
                    oldVersion.length(), currentVersion.length(),
                    currentVersion.length() - oldVersion.length());
        }

        // If the saving was successful, we delete the old version
        if (oldVersion.exists()) {
            FileUtils.forceDelete(oldVersion);
        }

        // Update the timestamp in the CAS in case we attempt to save it a second time. This
        // happens for example in an annotation replacement operation (change layer of existing
        // annotation) which is implemented as a delete/create operation with an intermediate
        // save.
        CasMetadataUtils.addOrUpdateCasMetadata(aCas, currentVersion.lastModified(), aDocument,
                aUserName);

        manageHistory(currentVersion, aDocument, aUserName);

        WicketUtil.serverTiming("realWriteCas", currentTimeMillis() - t0);
    }

    /**
     * Get the folder where the annotations are stored. Creates the folder if necessary.
     *
     * @throws IOException
     *             if the folder cannot be created.
     */
    public File getAnnotationFolder(SourceDocument aDocument) throws IOException
    {
        return getAnnotationFolder(aDocument.getProject().getId(), aDocument.getId());
    }

    private File getAnnotationFolder(long aProjectId, long aDocumentId) throws IOException
    {
        File annotationFolder = new File(repositoryProperties.getPath(), "/" + PROJECT_FOLDER + "/"
                + aProjectId + "/" + DOCUMENT_FOLDER + "/" + aDocumentId + "/" + ANNOTATION_FOLDER);
        FileUtils.forceMkdir(annotationFolder);
        return annotationFolder;
    }

    private void manageHistory(File aCurrentVersion, SourceDocument aDocument, String aUserName)
        throws IOException
    {
        if (backupProperties.getInterval() <= 0) {
            return;
        }

        File annotationFolder = getAnnotationFolder(aDocument);

        // Determine the reference point in time based on the current version
        long now = aCurrentVersion.lastModified();

        // Get all history files for the current user
        File[] history = annotationFolder.listFiles(new FileFilter()
        {
            private final Matcher matcher = Pattern
                    .compile(Pattern.quote(aUserName) + "\\.ser\\.[0-9]+\\.bak").matcher("");

            @Override
            public boolean accept(File aFile)
            {
                // Check if the filename matches the pattern given above.
                return matcher.reset(aFile.getName()).matches();
            }
        });

        // Sort the files (oldest one first)
        Arrays.sort(history, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);

        // Check if we need to make a new history file
        boolean historyFileCreated = false;
        File historyFile = new File(annotationFolder, aUserName + ".ser." + now + ".bak");
        if (history.length == 0) {
            // If there is no history yet but we should keep history, then we create a
            // history file in any case.
            FileUtils.copyFile(aCurrentVersion, historyFile);
            historyFileCreated = true;
        }
        else {
            // Check if the newest history file is significantly older than the current one
            File latestHistory = history[history.length - 1];
            if (latestHistory.lastModified() + (backupProperties.getInterval() * 1000) < now) {
                FileUtils.copyFile(aCurrentVersion, historyFile);
                historyFileCreated = true;
            }
        }

        // No history file created, then we can stop here
        if (!historyFileCreated) {
            return;
        }

        // Prune history based on number of backup
        // The new version is not in the history, so we keep that in any case. That
        // means we need to keep one less.
        int toKeep = Math.max(backupProperties.getKeep().getNumber() - 1, 0);
        if ((backupProperties.getKeep().getNumber() > 0) && (toKeep < history.length)) {
            // Copy the oldest files to a new array
            File[] toRemove = new File[history.length - toKeep];
            System.arraycopy(history, 0, toRemove, 0, toRemove.length);

            // Restrict the history to what is left
            File[] newHistory = new File[toKeep];
            if (toKeep > 0) {
                System.arraycopy(history, toRemove.length, newHistory, 0, newHistory.length);
            }
            history = newHistory;

            // Remove these old files
            for (File file : toRemove) {
                FileUtils.forceDelete(file);

                log.debug(
                        "Removed surplus history file [{}] of user [{}] for document {} in project {}",
                        file.getName(), aUserName, aDocument, aDocument.getProject());
            }
        }

        // Prune history based on time
        if (backupProperties.getKeep().getTime() > 0) {
            for (File file : history) {
                if ((file.lastModified() + (backupProperties.getKeep().getTime() * 1000)) < now) {
                    FileUtils.forceDelete(file);

                    log.debug(
                            "Removed outdated history file [{}] of user [{}] for "
                                    + "document [{}]({}) in project [{}]({})",
                            file.getName(), aUserName, aDocument.getName(), aDocument.getId(),
                            aDocument.getProject().getName(), aDocument.getProject().getId());
                }
            }
        }
    }

    /**
     * Method is scheduled to become private.
     */
    @SuppressWarnings("deprecation")
    @Override
    public File getCasFile(SourceDocument aDocument, String aUser) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        return getCasFile(aDocument.getProject().getId(), aDocument.getId(), aUser);
    }

    private File getCasFile(long aProjectId, long aDocumentId, String aUser) throws IOException
    {
        return new File(getAnnotationFolder(aProjectId, aDocumentId), aUser + ".ser");
    }

    /*
     * For testing
     */
    void writeSerializedCas(CAS aCas, File aFile) throws IOException
    {
        CasPersistenceUtils.writeSerializedCas(aCas, aFile);
    }

    @Override
    public boolean deleteCas(SourceDocument aDocument, String aUser) throws IOException
    {
        return new File(getAnnotationFolder(aDocument), aUser + ".ser").delete();
    }

    @Override
    public boolean existsCas(SourceDocument aDocument, String aUser) throws IOException
    {
        return getCasFile(aDocument, aUser).exists();
    }

    @Override
    public Optional<Long> getCasTimestamp(SourceDocument aDocument, String aUser) throws IOException
    {
        File casFile = getCasFile(aDocument, aUser);
        if (!casFile.exists()) {
            return Optional.empty();
        }
        else {
            return Optional.of(casFile.lastModified());
        }
    }
}
