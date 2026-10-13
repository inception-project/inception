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

import static de.tudarmstadt.ukp.inception.guidelines.GuidelineFiles.NAME_NOT_ALLOWED_MESSAGE;
import static de.tudarmstadt.ukp.inception.guidelines.GuidelineFiles.NOT_A_PDF_MESSAGE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.walk;
import static java.nio.file.Files.writeString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;

class GuidelinesServiceImplTest
{
    private static final String PDF = "%PDF-1.4\n%%EOF\n";

    private @TempDir File tempDir;

    private GuidelinesServiceImpl sut;
    private Project project;

    @BeforeEach
    void setup()
    {
        project = Project.builder().withId(1l).withName("Test Project").build();

        var repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(tempDir);
        sut = new GuidelinesServiceImpl(repositoryProperties);
    }

    private void create(String aName, String aContent) throws Exception
    {
        sut.createGuideline(project, new ByteArrayInputStream(aContent.getBytes(UTF_8)), aName);
    }

    private void assertRejected(String aName, String aContent, String aMessage)
    {
        assertThatExceptionOfType(InvalidGuidelineException.class)
                .isThrownBy(() -> create(aName, aContent)).withMessageContaining(aMessage);
    }

    private void assertRejectedAsNotPdf(String aName, String aContent)
    {
        assertRejected(aName, aContent, NOT_A_PDF_MESSAGE);
    }

    @Test
    void thatPdfIsAccepted() throws Exception
    {
        create("a.pdf", PDF);

        assertThat(sut.listGuidelines(project)).containsExactly("a.pdf");
        assertThat(readString(sut.getGuideline(project, "a.pdf").toPath())).isEqualTo(PDF);
    }

    @Test
    void thatUpperCaseExtensionIsAccepted() throws Exception
    {
        create("UPPER.PDF", PDF);

        assertThat(sut.listGuidelines(project)).containsExactly("UPPER.PDF");
    }

    @Test
    void thatWrongExtensionIsRejected()
    {
        assertRejectedAsNotPdf("a.html", PDF);
        assertRejectedAsNotPdf("a.txt", PDF);
        assertRejectedAsNotPdf("a", PDF);
        assertRejected(".pdf", PDF, NAME_NOT_ALLOWED_MESSAGE);
    }

    @Test
    void thatPdfNameWithOtherContentIsRejected()
    {
        assertRejectedAsNotPdf("a.pdf", "<html><script>alert(1)</script></html>");
        assertRejectedAsNotPdf("a.pdf", "");
        assertRejectedAsNotPdf("a.pdf", "%PDF");
    }

    @Test
    void thatMarkerNotAtByteZeroIsRejected()
    {
        assertRejectedAsNotPdf("a.pdf", " %PDF-1.4\n");
        assertRejectedAsNotPdf("a.pdf", "<html></html>\n%PDF-1.4\n");
    }

    @ParameterizedTest
    @ValueSource(strings = { "..pdf", "../a.pdf", "dir/a.pdf", "dir\\a.pdf", ".hidden.pdf",
            "-dash.pdf", "a\u0000.pdf", "a\n.pdf", "a:b.pdf", "a.html:x.pdf", "a*b.pdf", "a?b.pdf",
            "a<b>.pdf", "a|b.pdf", "a\"b.pdf", "a&b.pdf", "a$b.pdf" })
    void thatBadNamesAreRejected(String aName)
    {
        assertRejected(aName, PDF, NAME_NOT_ALLOWED_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  ", " .pdf" })
    void thatEmptyNamesAreRejected(String aName)
    {
        assertRejected(aName, PDF, "must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = { "Guide v1..2.pdf", "notes..pdf", "01 General (v2).pdf",
            "Anleitung \u00FC.pdf", "\u0631\u0627\u0647\u0646\u0645\u0627\u200C\u06CC.pdf",
            "\uD83D\uDCC4.pdf", "UPPER.PDF" })
    void thatHarmlessNamesAreAccepted(String aName) throws Exception
    {
        create(aName, PDF);

        assertThat(sut.listGuidelines(project)).containsExactly(aName);
    }

    @Test
    void thatOverlongNameIsRejected() throws Exception
    {
        assertRejected("a".repeat(252) + ".pdf", PDF, "too long");

        create("a".repeat(251) + ".pdf", PDF);
        assertThat(sut.listGuidelines(project)).hasSize(1);
    }

    @Test
    void thatNameLimitCountsBytesNotCharacters() throws Exception
    {
        // 2 bytes each: 126 * 2 + 4 = 256
        assertRejected("\u00E9".repeat(126) + ".pdf", PDF, "too long");

        create("\u00E9".repeat(125) + ".pdf", PDF);
        assertThat(sut.listGuidelines(project)).hasSize(1);
    }

    @Test
    void thatExistingGuidelineIsNotOverwritten() throws Exception
    {
        create("a.pdf", PDF);

        assertRejected("a.pdf", PDF + "other", "already exists");
        assertThat(readString(sut.getGuideline(project, "a.pdf").toPath())).isEqualTo(PDF);
    }

    @Test
    void thatRejectedUploadLeavesNothingBehind() throws Exception
    {
        create("keep.pdf", PDF);
        var before = snapshot();

        assertRejectedAsNotPdf("a.html", PDF);
        assertRejectedAsNotPdf("a.pdf", "<html></html>");
        assertRejected("keep.pdf", PDF, "already exists");
        assertRejected("../a.pdf", PDF, NAME_NOT_ALLOWED_MESSAGE);

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void thatFailingUploadLeavesNothingBehind() throws Exception
    {
        create("keep.pdf", PDF);
        var before = snapshot();

        // Passes the check on the first bytes, then fails while the content is being stored
        var failing = new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                throw new IOException("boom");
            }
        };
        var content = new SequenceInputStream(new ByteArrayInputStream(PDF.getBytes(UTF_8)),
                failing);

        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> sut.createGuideline(project, content, "a.pdf"))
                .withMessage("boom");

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void thatHasGuidelinesAgreesWithListing() throws Exception
    {
        assertThat(sut.hasGuidelines(project)).isFalse();

        createDirectories(sut.getGuidelinesFolder(project).toPath());
        createDirectory(new File(sut.getGuidelinesFolder(project), "sub").toPath());
        assertThat(sut.hasGuidelines(project)).isFalse();

        create("a.pdf", PDF);
        assertThat(sut.hasGuidelines(project)).isTrue();
    }

    @Test
    void thatFileOverloadAppliesTheSameRules() throws Exception
    {
        var html = new File(tempDir, "upload.tmp");
        writeString(html.toPath(), "<html></html>");

        assertThatExceptionOfType(InvalidGuidelineException.class)
                .isThrownBy(() -> sut.createGuideline(project, html, "a.pdf"));
    }

    @Test
    void thatGuidelinesAreSortedIgnoringCase() throws Exception
    {
        create("b.pdf", PDF);
        create("B2.pdf", PDF);
        create("a.pdf", PDF);
        create("01 z.pdf", PDF);

        assertThat(sut.listGuidelines(project)) //
                .containsExactly("01 z.pdf", "a.pdf", "b.pdf", "B2.pdf");
    }

    @Test
    void thatListingIgnoresDirectories() throws Exception
    {
        create("a.pdf", PDF);
        createDirectory(new File(sut.getGuidelinesFolder(project), "sub").toPath());

        assertThat(sut.listGuidelines(project)).containsExactly("a.pdf");
    }

    @Test
    void thatListingMissingFolderIsEmpty()
    {
        assertThat(sut.listGuidelines(project)).isEmpty();
    }

    @Test
    void thatListingEmptyFolderIsEmpty() throws Exception
    {
        createDirectories(sut.getGuidelinesFolder(project).toPath());

        assertThat(sut.listGuidelines(project)).isEmpty();
    }

    private List<String> snapshot() throws Exception
    {
        try (var files = walk(tempDir.toPath())) {
            return files.map(f -> tempDir.toPath().relativize(f).toString()).sorted().toList();
        }
    }
}
