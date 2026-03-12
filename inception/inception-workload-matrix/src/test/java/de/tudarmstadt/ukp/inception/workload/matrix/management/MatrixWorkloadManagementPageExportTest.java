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
package de.tudarmstadt.ukp.inception.workload.matrix.management;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixRow;

class MatrixWorkloadManagementPageExportTest
{
    @Test
    void testBuildAnnotatorListSortsByDisplayName()
    {
        // Create test users with different names to test sorting
        var userCharlie = User.builder().withUsername("charlie").withUiName("Charlie").build();
        var userAlice = User.builder().withUsername("alice").withUiName("Alice").build();
        var userBob = User.builder().withUsername("bob").withUiName("Bob").build();

        var users = List.of(userCharlie, userAlice, userBob);

        // After the fix, this should return a sorted list by displayName
        var result = MatrixWorkloadManagementPage.buildAnnotatorList(users);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).displayName()).isEqualTo("Alice");
        assertThat(result.get(1).displayName()).isEqualTo("Bob");
        assertThat(result.get(2).displayName()).isEqualTo("Charlie");
    }

    @Test
    void testExportWorkloadToCsvFormat() throws IOException
    {
        // Create test users
        var userCharlie = User.builder().withUsername("charlie").withUiName("Charlie").build();
        var userAlice = User.builder().withUsername("alice").withUiName("Alice").build();
        var userBob = User.builder().withUsername("bob").withUiName("Bob").build();

        var users = List.of(userCharlie, userAlice, userBob);

        // Build sorted annotator list using the fixed method
        var annotators = MatrixWorkloadManagementPage.buildAnnotatorList(users);

        // Create test project and documents
        var project = Project.builder().withName("test-project").build();

        var doc1 = SourceDocument.builder() //
                .withName("document1.txt") //
                .withProject(project) //
                .withFormat("text") //
                .build();

        var doc2 = SourceDocument.builder() //
                .withName("document2.txt") //
                .withProject(project) //
                .withFormat("text") //
                .build();

        // Create DocumentMatrixRow instances
        var annotatorSet = Set.of(AnnotationSet.forUser(userAlice), AnnotationSet.forUser(userBob),
                AnnotationSet.forUser(userCharlie));

        var row1 = new DocumentMatrixRow(doc1, annotatorSet);
        row1.add(AnnotationDocument.builder() //
                .withUser("alice") //
                .forDocument(doc1) //
                .withState(FINISHED) //
                .build());
        row1.add(AnnotationDocument.builder() //
                .withUser("bob") //
                .forDocument(doc1) //
                .withState(IN_PROGRESS) //
                .build());
        // Charlie has no annotation document for doc1 (should show as NEW)

        var row2 = new DocumentMatrixRow(doc2, annotatorSet);
        row2.add(AnnotationDocument.builder() //
                .withUser("alice") //
                .forDocument(doc2) //
                .withState(FINISHED) //
                .build());
        row2.add(AnnotationDocument.builder() //
                .withUser("bob") //
                .forDocument(doc2) //
                .withState(FINISHED) //
                .build());
        row2.add(AnnotationDocument.builder() //
                .withUser("charlie") //
                .forDocument(doc2) //
                .withState(FINISHED) //
                .build());

        var rows = List.of(row1, row2);

        // Export to CSV
        var stringWriter = new StringWriter();
        try (var csvPrinter = new CSVPrinter(stringWriter, EXCEL)) {
            MatrixWorkloadManagementPage.exportWorkloadToCsv(csvPrinter, annotators,
                    rows.iterator());
        }

        var csvOutput = stringWriter.toString();

        // Parse the CSV output
        try (var parser = CSVParser.parse(csvOutput, EXCEL)) {
            var records = parser.getRecords();

            // Verify we have header + 2 data rows
            assertThat(records).hasSize(3);

            // Verify headers are in correct order
            var headers = records.get(0);
            assertThat(headers.toList()).containsExactly("document name", "document state",
                    "curation state", "Alice", "Bob", "Charlie");

            // Verify first data row
            var dataRow1 = records.get(1);
            assertThat(dataRow1.get(0)).isEqualTo("document1.txt"); // document name
            // document state (calculated from annotation docs)
            assertThat(dataRow1.get(1)).isEqualTo("ANNOTATION_INPROGRESS");
            assertThat(dataRow1.get(2)).isEqualTo("NEW"); // curation state
            assertThat(dataRow1.get(3)).isEqualTo("FINISHED"); // Alice
            assertThat(dataRow1.get(4)).isEqualTo("INPROGRESS"); // Bob
            assertThat(dataRow1.get(5)).isEqualTo("NEW"); // Charlie (no annotation document)

            // Verify second data row
            var dataRow2 = records.get(2);
            assertThat(dataRow2.get(0)).isEqualTo("document2.txt"); // document name
            // document state (all annotators finished)
            assertThat(dataRow2.get(1)).isEqualTo("ANNOTATION_FINISHED");
            assertThat(dataRow2.get(2)).isEqualTo("NEW"); // curation state
            assertThat(dataRow2.get(3)).isEqualTo("FINISHED"); // Alice
            assertThat(dataRow2.get(4)).isEqualTo("FINISHED"); // Bob
            assertThat(dataRow2.get(5)).isEqualTo("FINISHED"); // Charlie
        }
    }
}
