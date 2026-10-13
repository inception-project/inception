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
package de.tudarmstadt.ukp.inception.guidelines;

import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.RELAXED_SHELL_SPECIAL_CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.isValidFilename;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.StringUtils.containsAny;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Rules for what is accepted as a guideline file: a PDF, by name and by content, with a harmless
 * name.
 */
final class GuidelineFiles
{
    static final String NOT_A_PDF_MESSAGE = "Guidelines must be PDF files. Convert the document to PDF first.";

    static final String NAME_NOT_ALLOWED_MESSAGE = "The guideline file name contains characters that are not allowed.";

    static final int MAX_NAME_BYTES = 255;

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(US_ASCII);

    private GuidelineFiles()
    {
        // No instances
    }

    /**
     * @return the number of bytes that need to be read to decide with {@link #isPdf(byte[], int)}.
     */
    static int pdfMagicLength()
    {
        return PDF_MAGIC.length;
    }

    /**
     * The marker must be at byte 0. PDF readers tolerate it further into the file, which would
     * allow files that are a PDF and something else at the same time.
     */
    static boolean isPdf(byte[] aHead, int aLength)
    {
        return aLength >= PDF_MAGIC.length
                && Arrays.equals(aHead, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length);
    }

    /**
     * Reads only the first bytes of the file.
     */
    static boolean isPdf(File aFile)
    {
        try (var is = newInputStream(aFile.toPath())) {
            var head = is.readNBytes(PDF_MAGIC.length);
            return isPdf(head, head.length);
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * @return the name
     * @throws InvalidGuidelineException
     *             if the name is not acceptable for a guideline.
     */
    static String validateName(String aName) throws InvalidGuidelineException
    {
        if (aName == null || aName.isBlank()) {
            throw new InvalidGuidelineException("The guideline file name must not be empty.");
        }

        var name = aName;

        if (name.getBytes(UTF_8).length > MAX_NAME_BYTES) {
            throw new InvalidGuidelineException(
                    "The guideline file name is too long (at most " + MAX_NAME_BYTES + " bytes).");
        }

        if (!isValidFilename(name) || containsAny(name, RELAXED_SHELL_SPECIAL_CHARACTERS)) {
            throw new InvalidGuidelineException(NAME_NOT_ALLOWED_MESSAGE);
        }

        if (!name.toLowerCase(ROOT).endsWith(".pdf")) {
            throw new InvalidGuidelineException(NOT_A_PDF_MESSAGE);
        }

        if (name.substring(0, name.length() - ".pdf".length()).isBlank()) {
            throw new InvalidGuidelineException("The guideline file name must not be empty.");
        }

        return name;
    }
}
