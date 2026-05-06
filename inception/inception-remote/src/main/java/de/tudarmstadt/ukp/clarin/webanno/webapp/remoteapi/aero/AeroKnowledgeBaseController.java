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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.ObjectNotFoundException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RKnowledgeBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroAnnotationController}.
 * </p>
 */
@Tag(name = "Knowledge Bases (non-AERO)", description = "Knowledge base management")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroProjectController.API_BASE)
public class AeroKnowledgeBaseController
    extends Controller_ImplBase
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired KnowledgeBaseService knowledgeBaseService;

    @Operation(summary = "List knowledge bases in a project")
    @GetMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/"
            + KNOWLEDGE_BASES, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RKnowledgeBase>>> list( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission("User [" + sessionOwner.getUsername()
                + "] is not allowed to access knowledge bases in project [" + aProjectId + "]",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var kbs = knowledgeBaseService.getEnabledKnowledgeBases(project);

        var kbList = new ArrayList<RKnowledgeBase>();
        for (var kb : kbs) {
            kbList.add(new RKnowledgeBase(kb));
        }

        return ResponseEntity.ok(new RResponse<>(kbList));
    }

    @Operation(summary = "Run a SPARQL query on the given knowledge base")
    @PostMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + KNOWLEDGE_BASES + "/{"
            + PARAM_KNOWLEDGE_BASE_ID + "}/sparql", produces = "application/sparql-results+xml")
    public ResponseEntity<String> sparqlOne( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @PathVariable(PARAM_KNOWLEDGE_BASE_ID) //
            @Schema(description = """
                    Knowledge base identifier.
                    """) //
            String aKbId, //
            @RequestBody //
            @Schema(description = """
                    SPARQL query.
                    """) //
            String aQuery)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission("User [" + sessionOwner.getUsername()
                + "] is not allowed to access knowledge bases in project [" + aProjectId + "]",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var maybeKb = knowledgeBaseService.getKnowledgeBaseById(project, aKbId);
        if (!maybeKb.map(KnowledgeBase::isEnabled).orElse(false)) {
            throw new ObjectNotFoundException("Knowledge base [" + aKbId + "] not found.");
        }

        var kb = maybeKb.get();

        var response = knowledgeBaseService.read(kb, conn -> {
            var query = conn.prepareTupleQuery(SPARQL, aQuery);
            query.setMaxExecutionTime(30);

            var out = new ByteArrayOutputStream();
            var writer = new SPARQLResultsXMLWriter(out);

            // Evaluate and write directly to XML
            query.evaluate(writer);

            return out.toString();
        });

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Run a SPARQL query over all knowledge bases in the project")
    @PostMapping(value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID
            + "}/sparql", produces = "application/sparql-results+xml")
    public ResponseEntity<String> sparqlAll( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @RequestBody //
            @Schema(description = """
                    SPARQL query.
                    """) //
            String aQuery)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission("User [" + sessionOwner.getUsername()
                + "] is not allowed to access knowledge bases in project [" + aProjectId + "]",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var bindingNames = new HashSet<String>();
        var combined = new ArrayList<BindingSet>();
        for (var kb : knowledgeBaseService.getEnabledKnowledgeBases(project)) {
            knowledgeBaseService.read(kb, conn -> {
                var query = conn.prepareTupleQuery(SPARQL, aQuery);
                query.setMaxExecutionTime(30);

                var result = query.evaluate();
                bindingNames.addAll(result.getBindingNames());
                while (result.hasNext()) {
                    combined.add(result.next());
                }
                return null;
            });
        }

        var out = new ByteArrayOutputStream();
        var writer = QueryResultIO.createTupleWriter(TupleQueryResultFormat.SPARQL, out);

        // Start the XML output
        writer.startQueryResult(bindingNames.stream().toList());

        for (var bs : combined) {
            writer.handleSolution(bs);
        }

        writer.endQueryResult();

        return ResponseEntity.ok(out.toString());
    }
}
