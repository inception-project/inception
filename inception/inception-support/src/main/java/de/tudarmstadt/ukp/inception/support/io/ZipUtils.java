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
package de.tudarmstadt.ukp.inception.support.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * A utility class.
 */
public class ZipUtils
{
    // The magic bytes for ZIP
    // see
    // http://notepad2.blogspot.de/2012/07/java-detect-if-stream-or-file-is-zip.html
    private static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

    /**
     * check if the {@link InputStream} provided is a zip file
     * 
     * @param in
     *            the stream.
     * @return if it is a ZIP file.
     * @throws IOException
     *             if there was an error operating on the input stream
     */
    public static boolean isZipStream(InputStream in) throws IOException
    {
        if (!in.markSupported()) {
            throw new IllegalArgumentException("Mark is not supported.");
        }

        byte[] buf = new byte[MAGIC.length];
        in.mark(MAGIC.length);
        for (int i = 0; i < MAGIC.length; i++) {
            int b = in.read();
            if (b == -1) {
                break;
            }
            else {
                buf[i] = (byte) b;
            }
        }
        in.reset();

        return Arrays.equals(buf, MAGIC);
    }

    /**
     * While exporting annotation documents, some of the writers generate multiple outputs, e.g. a
     * type system file in addition to the annotation data. This method generates a zip file if the
     * exported file do contain multiple file output
     * 
     * @param srcFolder
     *            source folder.
     * @param destZipFile
     *            target folder.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void zipFolder(File srcFolder, File destZipFile) throws IOException
    {
        try (var zip = new ZipOutputStream(new FileOutputStream(destZipFile));) {
            for (var file : srcFolder.getAbsoluteFile().listFiles()) {
                addToZip(zip, srcFolder.getAbsoluteFile(), file);
            }
            zip.flush();
        }
    }

    private static void addToZip(ZipOutputStream zip, File aBasePath, File aPath) throws IOException
    {
        if (aPath.isDirectory()) {
            for (File file : aPath.listFiles()) {
                addToZip(zip, aBasePath, file);
            }
        }
        else {
            try (FileInputStream in = new FileInputStream(aPath)) {
                String relativePath = aBasePath.toURI().relativize(aPath.toURI()).getPath();
                zip.putNextEntry(new ZipEntry(relativePath));
                IOUtils.copy(in, zip);
            }
        }
    }

    public static String normalizeEntryName(ZipEntry aEntry)
    {
        // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
        String entryName = aEntry.toString();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }

        return entryName;
    }

    public static InputStream openResourceStream(File aZipFile, String aEntryName)
        throws IOException
    {
        if (aEntryName.contains("..") || aEntryName.contains("//")) {
            throw new FileNotFoundException("Resource not found [" + aEntryName + "]");
        }

        ZipFile zipFile = null;
        var success = false;
        try {
            zipFile = new ZipFile(aZipFile);
            var entry = zipFile.getEntry(aEntryName);
            if (entry == null) {
                throw new FileNotFoundException("Resource not found [" + aEntryName + "]");
            }

            var finalZipFile = zipFile;
            var is = new FilterInputStream(zipFile.getInputStream(entry))
            {
                @Override
                public void close() throws IOException
                {
                    super.close();
                    finalZipFile.close();
                }
            };
            success = true;
            return is;
        }
        finally {
            if (!success && zipFile != null) {
                zipFile.close();
            }
        }
    }
}
