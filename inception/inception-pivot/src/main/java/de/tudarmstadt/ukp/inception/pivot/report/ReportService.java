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

import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.pivot.api.model.PivotReport;
import de.tudarmstadt.ukp.inception.pivot.api.report.ReportDef;

public interface ReportService
{
    void createOrUpdateReport(PivotReport aReport);

    void deleteReport(PivotReport aReport);

    Optional<PivotReport> getReport(long aId);

    List<PivotReport> listReports(Project aProject);

    ReportDef readDef(PivotReport aReport);

    void writeDef(PivotReport aReport, ReportDef aDef);

    ReportDef toDef(ReportDecl aDecl);

    ResolvedReport resolve(ReportDef aDef, Project aProject);

    List<AnnotationSet> listDataOwners(Project aProject);
}
