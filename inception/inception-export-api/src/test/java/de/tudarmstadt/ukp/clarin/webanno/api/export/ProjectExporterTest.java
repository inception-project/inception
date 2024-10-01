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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter.getEntry;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter.normalizeEntryName;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter.writeEntry;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectExporterTest
{

    @Test
    void testGetEntry(@TempDir Path tempDir) throws Exception
    {
        var zipFile = tempDir.resolve("test.zip").toFile();

        try (var fos = new FileOutputStream(zipFile); var zos = new ZipOutputStream(fos)) {
            zos.putNextEntry(new ZipEntry("/foo.txt"));
            zos.write("foo".getBytes(UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("bar.txt"));
            zos.write("bar".getBytes(UTF_8));
            zos.closeEntry();
        }

        try (var zip = new ZipFile(zipFile)) {
            assertThat(getEntry(zip, "/foo.txt")) //
                    .isNotNull() //
                    .extracting(ZipEntry::getName) //
                    .isEqualTo("/foo.txt");

            assertThat(getEntry(zip, "foo.txt")) //
                    .isNotNull() //
                    .extracting(ZipEntry::getName) //
                    .isEqualTo("/foo.txt");

            assertThat(getEntry(zip, "/bar.txt")) //
                    .isNotNull() //
                    .extracting(ZipEntry::getName) //
                    .isEqualTo("bar.txt");

            assertThat(getEntry(zip, "bar.txt")) //
                    .isNotNull() //
                    .extracting(ZipEntry::getName) //
                    .isEqualTo("bar.txt");

            assertThat(getEntry(zip, "nonexistent.txt")) //
                    .isNull();
        }
    }

    @Test
    void testWriteEntry(@TempDir Path tempDir) throws Exception
    {
        var zipFile = tempDir.resolve("test.zip").toFile();

        try (var fos = new FileOutputStream(zipFile); var zos = new ZipOutputStream(fos)) {
            writeEntry(zos, "/foo.txt", os -> os.write("foo".getBytes(UTF_8)));
            writeEntry(zos, "bar.txt", os -> os.write("bar".getBytes(UTF_8)));
        }

        try (var zf = new ZipFile(zipFile)) {
            assertThat(zf.getEntry("foo.txt")) //
                    .isNotNull() //
                    .extracting(ZipEntry::getName) //
                    .isEqualTo("foo.txt");
            assertThat(zf.getEntry("bar.txt")) //
                    .isNotNull() //
                    .extracting(ZipEntry::getName) //
                    .isEqualTo("bar.txt");
        }
    }

    @Test
    void testNormalizeEntryName()
    {
        // Test entry with leading slash
        assertThat(normalizeEntryName(new ZipEntry("/foo.txt"))).isEqualTo("foo.txt");

        // Test entry without leading slash
        assertThat(normalizeEntryName(new ZipEntry("bar.txt"))).isEqualTo("bar.txt");

        // Test entry with multiple leading slashes
        assertThat(normalizeEntryName(new ZipEntry("///baz.txt"))).isNull();

        // Test entry with no name
        assertThat(normalizeEntryName(new ZipEntry(""))).isNull();
    }
}
