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
package de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.setDocumentId;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_COMPARATOR;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageMetadata;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.CasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

public class FileSystemCasStorageDriver
    implements CasStorageDriver
{
    public static final String SER_CAS_EXTENSION = ".ser";
    public static final String OLD_EXTENSION = ".old";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RepositoryProperties repositoryProperties;
    private final CasStorageProperties casStorageProperties;
    private final CasStorageBackupProperties backupProperties;
    private final LoadingCache<File, InternalMetadata> metadataCache;

    public FileSystemCasStorageDriver(RepositoryProperties aRepositoryProperties,
            CasStorageBackupProperties aBackupProperties,
            CasStorageProperties aCasStorageProperties)
    {
        repositoryProperties = aRepositoryProperties;
        backupProperties = aBackupProperties;
        casStorageProperties = aCasStorageProperties;

        if (casStorageProperties.isTraceAccess()) {
            metadataCache = Caffeine.newBuilder() //
                    .expireAfterWrite(Duration.ofHours(1)) //
                    .build(k -> new InternalMetadata());
        }
        else {
            metadataCache = null;
        }

        if (backupProperties.getInterval() > 0) {
            BOOT_LOG.info("CAS backups enabled - interval: {}sec  max-backups: {}  max-age: {}sec",
                    backupProperties.getInterval(), backupProperties.getKeep().getNumber(),
                    backupProperties.getKeep().getTime());
        }
        else {
            BOOT_LOG.info("CAS backups disabled");
        }
    }

    @Override
    public CAS readCas(SourceDocument aDocument, String aUser) throws IOException
    {
        LOG.trace("Reading CAS [{}]@{}", aUser, aDocument);

        var casFile = getCasFile(aDocument.getProject().getId(), aDocument.getId(), aUser);
        var oldCasFile = new File(casFile.getPath() + OLD_EXTENSION);

        if (metadataCache != null) {
            metadataCache.get(casFile).readAttempt();
        }

        var msgOldExists = "";
        if (oldCasFile.exists()) {
            msgOldExists = String.format(
                    "Existence of temporary annotation file [%s] indicates that a previous "
                            + "annotation storage process did not successfully complete. Contact "
                            + "your server administator and request renaming the '%s%s' file "
                            + "to '.ser' manually on the command line. Advise the administrator to "
                            + "check for sufficient disk space and that the application has the "
                            + "necessary permissions to save files in its data folder.",
                    oldCasFile, SER_CAS_EXTENSION, OLD_EXTENSION);
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
                    + "] for source document " + aDocument + " not found in project ["
                    + aDocument.getProject() + "). " + msgOldExists);
        }

        try {
            CasPersistenceUtils.readSerializedCas(cas, casFile);
            // Add/update the CAS metadata
            CasMetadataUtils.addOrUpdateCasMetadata(cas, casFile.lastModified(), aDocument, aUser);
        }
        catch (Exception e) {
            throw new IOException("Annotation document of user [" + aUser + "] for source document "
                    + aDocument + " in project [" + aDocument.getProject()
                    + " cannot be read from file [" + casFile + "]. " + msgOldExists, e);
        }

        if (metadataCache != null) {
            metadataCache.get(casFile).readSuccess();
        }

        return cas;
    }

    @Override
    public void writeCas(SourceDocument aDocument, String aUserName, CAS aCas) throws IOException
    {
        var t0 = currentTimeMillis();

        LOG.debug("Preparing to update annotations for user [{}] on document {} " //
                + "in project {}", aUserName, aDocument, aDocument.getProject());

        var annotationFolder = getAnnotationFolder(aDocument);
        var currentVersion = new File(annotationFolder, aUserName + SER_CAS_EXTENSION);
        var oldVersion = new File(annotationFolder, aUserName + SER_CAS_EXTENSION + OLD_EXTENSION);

        if (metadataCache != null) {
            metadataCache.get(currentVersion).writeAttempt();
        }

        // Check if there was a concurrent change to the file on disk
        if (currentVersion.exists()) {
            failOnConcurrentModification(aCas, currentVersion, aDocument, aUserName, "writing");
        }

        // Save current version
        try {
            // Make a backup of the current version of the file before overwriting
            if (currentVersion.exists()) {
                move(currentVersion.toPath(), oldVersion.toPath());
            }

            // Now write the new version to "<username>.ser" or CURATION_USER.ser
            setDocumentId(aCas, aUserName);
            if (casStorageProperties.isParanoidCasSerialization()) {
                CasPersistenceUtils.writeSerializedCasParanoid(aCas, currentVersion);
            }
            else if (casStorageProperties.isCompressedCasSerialization()) {
                CasPersistenceUtils.writeSerializedCasCompressed(aCas, currentVersion);
            }
            else {
                CasPersistenceUtils.writeSerializedCas(aCas, currentVersion);
            }
        }
        catch (Exception e) {
            LOG.error("There was an error while trying to write the CAS to [" + currentVersion
                    + "] - additional messages follow.");
            // If this is the first version, there is no old version, so do not restore anything
            if (!oldVersion.exists()) {
                LOG.warn("There is no old version to restore - leaving the current version which "
                        + "may be corrupt: [{}]", currentVersion);
                // Now abort anyway
                throw e;
            }

            LOG.error("Restoring previous annotations for user [{}] on document {} in " //
                    + "project {} due exception when trying to write new " + "annotations: [{}]",
                    aUserName, aDocument, aDocument.getProject(), oldVersion);
            try {
                move(oldVersion.toPath(), currentVersion.toPath(), REPLACE_EXISTING);
            }
            catch (Exception ex) {
                LOG.error("Unable to restore previous annotations: [{}]", oldVersion, ex);
            }

            // Now abort anyway
            throw e;
        }

        if (oldVersion.exists() && (currentVersion.length() < (oldVersion.length()
                * (casStorageProperties.isCompressedCasSerialization() ? 0.95d : 1.0d)))) {
            // If compression is enabled, then it is not so uncommon that the file size may also
            // become smaller at times, so we allow a bit of slip
            LOG.debug(
                    "Annotations shrunk for user [{}] on document {} in project "
                            + "{}: {} -> {} bytes ({} bytes removed)",
                    aUserName, aDocument, aDocument.getProject(), oldVersion.length(),
                    currentVersion.length(), currentVersion.length() - oldVersion.length());
        }

        // If the saving was successful, we delete the old version
        if (oldVersion.exists()) {
            FileUtils.forceDelete(oldVersion);
        }

        // Update the timestamp in the CAS in case we attempt to save it a second time. This
        // happens for example in an annotation replacement operation (change layer of existing
        // annotation) which is implemented as a delete/create operation with an intermediate
        // save.
        var lastModified = currentVersion.lastModified();
        CasMetadataUtils.addOrUpdateCasMetadata(aCas, lastModified, aDocument, aUserName);
        if (metadataCache != null) {
            metadataCache.get(currentVersion).writeSuccess(lastModified);
        }

        manageHistory(currentVersion, aDocument, aUserName);

        var duration = currentTimeMillis() - t0;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated annotations for user [{}] on document {} in project {} " //
                    + "{} bytes in {}ms (file timestamp: {}, compression: {})", aUserName,
                    aDocument, aDocument.getProject(), currentVersion.length(), duration,
                    formatTimestamp(lastModified),
                    casStorageProperties.isCompressedCasSerialization());
        }

        WicketUtil.serverTiming("realWriteCas", duration);
    }

    /**
     * @param aDocument
     *            the document of interest.
     * @return the folder where the annotations are stored. Creates the folder if necessary.
     *
     * @throws IOException
     *             if the folder cannot be created.
     */
    // Public for testing
    public File getAnnotationFolder(SourceDocument aDocument) throws IOException
    {
        return getAnnotationFolder(aDocument.getProject().getId(), aDocument.getId());
    }

    private File getAnnotationFolder(long aProjectId, long aDocumentId) throws IOException
    {
        var annotationFolder = new File(repositoryProperties.getPath(), "/" + PROJECT_FOLDER + "/"
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

        var annotationFolder = getAnnotationFolder(aDocument);

        // Determine the reference point in time based on the current version
        long now = aCurrentVersion.lastModified();

        // Get all history files for the current user
        var history = annotationFolder.listFiles(new FileFilter()
        {
            private final Matcher matcher = compile(quote(aUserName) + "\\.ser\\.[0-9]+\\.bak")
                    .matcher("");

            @Override
            public boolean accept(File aFile)
            {
                // Check if the filename matches the pattern given above.
                return matcher.reset(aFile.getName()).matches();
            }
        });

        // Sort the files (oldest one first)
        Arrays.sort(history, LASTMODIFIED_COMPARATOR);

        // Check if we need to make a new history file
        var historyFileCreated = false;
        var historyFile = new File(annotationFolder, aUserName + ".ser." + now + ".bak");
        if (history.length == 0) {
            // If there is no history yet but we should keep history, then we create a
            // history file in any case.
            FileUtils.copyFile(aCurrentVersion, historyFile);
            historyFileCreated = true;
        }
        else {
            // Check if the newest history file is significantly older than the current one
            var latestHistory = history[history.length - 1];
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
        var toKeep = Math.max(backupProperties.getKeep().getNumber() - 1, 0);
        if ((backupProperties.getKeep().getNumber() > 0) && (toKeep < history.length)) {
            // Copy the oldest files to a new array
            var toRemove = new File[history.length - toKeep];
            System.arraycopy(history, 0, toRemove, 0, toRemove.length);

            // Restrict the history to what is left
            var newHistory = new File[toKeep];
            if (toKeep > 0) {
                System.arraycopy(history, toRemove.length, newHistory, 0, newHistory.length);
            }
            history = newHistory;

            // Remove these old files
            for (var file : toRemove) {
                FileUtils.forceDelete(file);

                LOG.debug(
                        "Removed surplus history file [{}] of user [{}] for document {} in project {}",
                        file.getName(), aUserName, aDocument, aDocument.getProject());
            }
        }

        // Prune history based on time
        if (backupProperties.getKeep().getTime() > 0) {
            for (var file : history) {
                if ((file.lastModified() + (backupProperties.getKeep().getTime() * 1000)) < now) {
                    FileUtils.forceDelete(file);

                    LOG.debug(
                            "Removed outdated history file [{}] of user [{}] for "
                                    + "document {} in project {}",
                            file.getName(), aUserName, aDocument, aDocument.getProject());
                }
            }
        }
    }

    // Public for testing
    public File getCasFile(SourceDocument aDocument, String aUser) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        return getCasFile(aDocument.getProject().getId(), aDocument.getId(), aUser);
    }

    @Override
    public void exportCas(SourceDocument aDocument, String aUser, OutputStream aStream)
        throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        try (var is = Files.newInputStream(getCasFile(aDocument, aUser).toPath())) {
            IOUtils.copyLarge(is, aStream);
        }
    }

    @Override
    public void importCas(SourceDocument aDocument, String aUser, InputStream aStream)
        throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        try (var os = Files.newOutputStream(getCasFile(aDocument, aUser).toPath())) {
            IOUtils.copyLarge(aStream, os);
        }
    }

    public File getCasFile(long aProjectId, long aDocumentId, String aUser) throws IOException
    {
        return new File(getAnnotationFolder(aProjectId, aDocumentId), aUser + SER_CAS_EXTENSION);
    }

    @Override
    public boolean deleteCas(SourceDocument aDocument, String aUser) throws IOException
    {
        var casFile = getCasFile(aDocument.getProject().getId(), aDocument.getId(), aUser);

        if (metadataCache != null) {
            metadataCache.invalidate(casFile);
        }

        return casFile.delete();
    }

    @Override
    public boolean existsCas(SourceDocument aDocument, String aUser) throws IOException
    {
        return getCasFile(aDocument, aUser).exists();
    }

    @Override
    public Optional<Long> getCasFileSize(SourceDocument aDocument, String aUser) throws IOException
    {
        var file = getCasFile(aDocument, aUser);
        if (file.exists()) {
            return Optional.of(file.length());
        }

        return Optional.empty();
    }

    @Override
    public Optional<CasStorageMetadata> getCasMetadata(SourceDocument aDocument, String aUser)
        throws IOException
    {
        var casFile = getCasFile(aDocument, aUser);

        if (!casFile.exists()) {
            return Optional.empty();
        }

        return Optional.of(new Metadata(casFile));
    }

    @Override
    public Optional<Long> verifyCasTimestamp(SourceDocument aDocument, String aUser,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException
    {
        var casFile = getCasFile(aDocument, aUser);

        if (!casFile.exists()) {
            return Optional.empty();
        }

        var diskLastModified = casFile.lastModified();
        if (Math.abs(diskLastModified - aExpectedTimeStamp) > casStorageProperties
                .getFileSystemTimestampAccuracy().toMillis()) {
            StringBuilder lastWriteMsg = new StringBuilder();
            if (metadataCache != null) {
                InternalMetadata meta = metadataCache.get(casFile);
                if (meta.lastWriteSuccessTrace != null) {
                    lastWriteMsg.append("\n");
                    lastWriteMsg.append("Last known successful write was at ");
                    lastWriteMsg.append(formatTimestamp(meta.lastWriteSuccessTimestamp));
                    lastWriteMsg.append(" by:\n");
                    for (StackTraceElement e : meta.lastWriteSuccessTrace) {
                        lastWriteMsg.append("    ");
                        lastWriteMsg.append(e);
                        lastWriteMsg.append("\n");
                    }
                }
            }
            throw new ConcurentCasModificationException("While [" + aContextAction
                    + "], the file system CAS storage detected a concurrent modification to the annotation CAS for user ["
                    + aUser + "] in document " + aDocument + " or project " + aDocument.getProject()
                    + " (expected: " + formatTimestamp(aExpectedTimeStamp) + " actual on storage: "
                    + formatTimestamp(diskLastModified) + ", delta: "
                    + formatDurationHMS(Math.abs(diskLastModified - aExpectedTimeStamp)) + ")"
                    + lastWriteMsg + ", accuracy: "
                    + casStorageProperties.getFileSystemTimestampAccuracy().toMillis() + "ms");

        }

        return Optional.of(diskLastModified);
    }

    private void failOnConcurrentModification(CAS aCas, File aCasFile, SourceDocument aDocument,
            String aUsername, String aContextAction)
        throws IOException
    {
        // If the type system of the CAS does not yet support CASMetadata, then we do not add it
        // and wait for the next regular CAS upgrade before we include this data.
        if (aCas.getTypeSystem().getType(CASMetadata._TypeName) == null) {
            LOG.warn("Annotation file [{}] of user [{}] for document {} in project {} "
                    + "does not support CASMetadata yet - unable to detect concurrent modifications",
                    aCasFile.getName(), aUsername, aDocument, aDocument.getProject());
            return;
        }

        var cmds = aCas.select(CASMetadata._TypeName).asList();
        if (cmds.isEmpty()) {
            LOG.warn(
                    "Annotation file [{}] of user [{}] for document {} in project "
                            + "{} does not contain CASMetadata yet - unable to check for "
                            + "concurrent modifications",
                    aCasFile.getName(), aUsername, aDocument, aDocument.getProject());
            return;
        }

        if (cmds.size() > 1) {
            throw new IOException("CAS contains more than one CASMetadata instance");
        }

        var cmd = cmds.get(0);
        var lastKnownUpdate = FSUtil.getFeature(cmd, "lastChangedOnDisk", Long.class);
        verifyCasTimestamp(aDocument, aUsername, lastKnownUpdate, aContextAction);
    }

    private static String formatTimestamp(long aTime)
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(aTime);
    }

    @SuppressWarnings("unused")
    private static class InternalMetadata
    {
        private StackTraceElement[] lastWriteAttemptTrace;
        private StackTraceElement[] lastWriteSuccessTrace;
        private long lastWriteSuccessTimestamp;
        private StackTraceElement[] lastReadAttemptTrace;
        private StackTraceElement[] lastReadSuccessTrace;

        private void writeAttempt()
        {
            lastWriteAttemptTrace = Thread.currentThread().getStackTrace();
        }

        private void writeSuccess(long aTimestamp)
        {
            lastWriteSuccessTrace = Thread.currentThread().getStackTrace();
            lastWriteSuccessTimestamp = aTimestamp;
        }

        private void readAttempt()
        {
            lastReadAttemptTrace = Thread.currentThread().getStackTrace();
        }

        private void readSuccess()
        {
            lastReadSuccessTrace = Thread.currentThread().getStackTrace();
        }
    }

    public static class Metadata
        implements CasStorageMetadata
    {
        private final long timestamp;
        private final long size;
        private final String path;

        public Metadata(File aFile)
        {
            timestamp = aFile.lastModified();
            size = aFile.length();
            path = aFile.getAbsolutePath();
        }

        @Override
        public long getTimestamp()
        {
            return timestamp;
        }

        @Override
        public long getSize()
        {
            return size;
        }

        @Override
        public long getVersion()
        {
            return timestamp;
        }

        public String getPath()
        {
            return path;
        }
    }
}
