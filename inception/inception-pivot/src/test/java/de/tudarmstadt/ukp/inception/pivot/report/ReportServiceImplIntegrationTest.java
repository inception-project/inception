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
package de.tudarmstadt.ukp.inception.pivot.report;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.PersistenceException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.pivot.api.report.AggregatorDef;
import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;
import de.tudarmstadt.ukp.inception.pivot.api.model.PivotReport;
import de.tudarmstadt.ukp.inception.pivot.api.report.ReportDef;

@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
class ReportServiceImplIntegrationTest
{
    private @Autowired TestEntityManager testEntityManager;

    private ReportServiceImpl sut;

    private Project project;
    private Project otherProject;

    @BeforeEach
    void setUp()
    {
        sut = new ReportServiceImpl(testEntityManager.getEntityManager(), null, null, null, null,
                null);
        project = createProject("Project A");
        otherProject = createProject("Project B");
    }

    @Test
    void createPersistsNewReport()
    {
        var report = new PivotReport(project, "report1");
        report.setDescription("My first report");

        sut.createOrUpdateReport(report);

        assertThat(report.getId()).isNotNull();
        assertThat(report.getCreated()).isNotNull();
        assertThat(report.getUpdated()).isEqualTo(report.getCreated());
    }

    @Test
    void updateChangesUpdatedTimestamp() throws Exception
    {
        var report = new PivotReport(project, "report1");
        sut.createOrUpdateReport(report);
        var createdAt = report.getCreated();

        Thread.sleep(10); // ensure timestamp granularity tick
        report.setDescription("changed");
        sut.createOrUpdateReport(report);
        testEntityManager.flush();

        var reloaded = sut.getReport(report.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo("changed");
        assertThat(reloaded.getCreated()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdated()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void getReportReturnsEmptyWhenNotFound()
    {
        assertThat(sut.getReport(99999L)).isEmpty();
    }

    @Test
    void listReportsFiltersByProjectAndSortsByName()
    {
        sut.createOrUpdateReport(new PivotReport(project, "zebra"));
        sut.createOrUpdateReport(new PivotReport(project, "apple"));
        sut.createOrUpdateReport(new PivotReport(project, "mango"));
        sut.createOrUpdateReport(new PivotReport(otherProject, "should-not-appear"));

        var reports = sut.listReports(project);

        assertThat(reports) //
                .extracting(PivotReport::getName) //
                .containsExactly("apple", "mango", "zebra");
    }

    @Test
    void deleteReportRemovesIt()
    {
        var report = new PivotReport(project, "report1");
        sut.createOrUpdateReport(report);
        var id = report.getId();

        sut.deleteReport(report);
        testEntityManager.flush();

        assertThat(sut.getReport(id)).isEmpty();
    }

    @Test
    void uniqueConstraintOnNameAndProjectIsEnforced()
    {
        sut.createOrUpdateReport(new PivotReport(project, "duplicate"));
        testEntityManager.flush();

        assertThatThrownBy(() -> {
            sut.createOrUpdateReport(new PivotReport(project, "duplicate"));
            testEntityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void sameNameAllowedInDifferentProjects()
    {
        sut.createOrUpdateReport(new PivotReport(project, "shared"));
        sut.createOrUpdateReport(new PivotReport(otherProject, "shared"));
        testEntityManager.flush();

        assertThat(sut.listReports(project)).extracting(PivotReport::getName)
                .containsExactly("shared");
        assertThat(sut.listReports(otherProject)).extracting(PivotReport::getName)
                .containsExactly("shared");
    }

    @Test
    void writeAndReadDefRoundTrips()
    {
        var report = new PivotReport(project, "report1");
        var def = new ReportDef();
        def.setAggregator(new AggregatorDef("count"));
        def.setRowExtractors(asList(new ExtractorDef("featureValue", "Token", "pos")));

        sut.writeDef(report, def);
        sut.createOrUpdateReport(report);
        testEntityManager.flush();
        testEntityManager.clear();

        var reloaded = sut.getReport(report.getId()).orElseThrow();
        var loadedDef = sut.readDef(reloaded);

        assertThat(loadedDef).isEqualTo(def);
    }

    @Test
    void readDefReturnsNullForBlankDefinition()
    {
        var report = new PivotReport(project, "report1");
        sut.createOrUpdateReport(report);

        assertThat(sut.readDef(report)).isNull();
    }

    @Test
    void readDefThrowsOnMalformedJson()
    {
        var report = new PivotReport(project, "report1");
        report.setDefinition("{ this is not valid json");
        sut.createOrUpdateReport(report);

        assertThatThrownBy(() -> sut.readDef(report)) //
                .isInstanceOf(IllegalStateException.class) //
                .hasMessageContaining("report1");
    }

    private Project createProject(String aName)
    {
        var p = new Project();
        p.setName(aName);
        return testEntityManager.persist(p);
    }

    @SpringBootConfiguration
    @EntityScan(basePackages = { //
            "de.tudarmstadt.ukp.inception.pivot.api.model", //
            "de.tudarmstadt.ukp.clarin.webanno.security.model", //
            "de.tudarmstadt.ukp.clarin.webanno.model" })
    @EnableAutoConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
