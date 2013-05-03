/*******************************************************************************
 * Copyright 2013
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Expose some functions of WebAnno via a RESTful remote API.
 * 
 * @author Richard Eckart de Castilho
 */
@Controller
public class RemoteApiController
{
	public static final String MIME_TYPE_XML = "application/xml";
	public static final String PRODUCES_JSON = "application/json";
	public static final String PRODUCES_XML = "application/xml";
	public static final String CONSUMES_URLENCODED = "application/x-www-form-urlencoded";

	@Resource(name = "documentRepository")
	private RepositoryService projectRepository;

	@Resource(name = "annotationService")
	private AnnotationService annotationService;

	private final Log log = LogFactory.getLog(getClass());

	/**
	 * Create a new project.
	 * 
	 * To test, use the Linux "curl" command.
	 * 
	 * curl -v -F 'file=@test.zip' -F 'name=Test' -F 'filetype=tcf' 'http://USERNAME:PASSWORD@localhost:8080/de.tudarmstadt.ukp.clarin.webanno.webapp/api/project'
	 * 
	 * @param aName
	 *            the name of the project to create.
	 * @param aFileType
	 *            the type of the files contained in the ZIP. The possible file types are configured
	 *            in the formats.properties configuration file of WebAnno.
	 * @param aFile
	 *            a ZIP file containing the project data.
	 * @throws IOException
	 */
	@RequestMapping(value = "/project", method = RequestMethod.POST,
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public @ResponseStatus(HttpStatus.NO_CONTENT)
	void createProject(@RequestParam("file") MultipartFile aFile,
			@RequestParam("name") String aName, @RequestParam("filetype") String aFileType)
		throws IOException
	{
		log.info("Creating project [" + aName + "]");

		// Get current user
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = projectRepository.getUser(username);

		// Configure project
		Project project = new Project();
		project.setName(aName);

		// Create the project and initialize tags
		// projectRepository.createProject(project, user);
		// annotationService.initializeTypesForProject(project, user);
		
		// Iterate through all the files in the ZIP

		// If the current filename does not start with "." and is in the root folder of the ZIP,
		// import it as a source document
		// FIXME
		
		// IF the current filename is META-INF/webanno/source-meta-data.properties store it as 
		// project meta data
		// FIXME

		log.info("Successfully created project [" + aName + "] for user [" + username + "]");
	}
}
