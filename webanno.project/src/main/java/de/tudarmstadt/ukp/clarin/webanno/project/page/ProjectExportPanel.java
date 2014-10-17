/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.project.page;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DaoUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.export.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.export.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoCustomTsvWriter;

/**
 * A Panel used to add Project Guidelines in a selected {@link Project}
 *
 * @author Seid Muhie Yimam
 *
 */
@SuppressWarnings("deprecation")
public class ProjectExportPanel extends Panel {
	private static final long serialVersionUID = 2116717853865353733L;

	private static final String META_INF = "/META-INF";
	public static final String EXPORTED_PROJECT = "exportedproject";
	private static final String SOURCE = "/source";
	private static final String CURATION_AS_SERIALISED_CAS = "/curation_ser/";
	private static final String CURATION = "/curation/";
	private static final String LOG = "/log";
	private static final String GUIDELINE = "/guideline";
	private static final String ANNOTATION_AS_SERIALISED_CAS = "/annotation_ser/";
	private static final String ANNOTATION = "/annotation/";

	private static final String CURATION_USER = "CURATION_USER";
	private static final String CORRECTION_USER = "CORRECTION_USER";

	@SpringBean(name = "annotationService")
	private AnnotationService annotationService;

	@SpringBean(name = "documentRepository")
	private RepositoryService repository;

	@SpringBean(name = "userRepository")
	private UserDao userRepository;

	private int progress = 0;
	private ProgressBar fileGenerationProgress;
	@SuppressWarnings("unused")
	private AjaxLink<Void> exportProjectLink;

	private String fileName;
	private String downloadedFile;
	@SuppressWarnings("unused")
	private String projectName;

	private transient Thread thread = null;
	private transient FileGenerator runnable = null;

	private boolean enabled = true;
	private boolean canceled = false;

	public ProjectExportPanel(String id, final Model<Project> aProjectModel) {
		super(id);

		final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
		add(feedbackPanel);
		feedbackPanel.setOutputMarkupId(true);
		feedbackPanel.add(new AttributeModifier("class", "info"));
		feedbackPanel.add(new AttributeModifier("class", "error"));

		add(new Button("send", new ResourceModel("label")) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return repository.isRemoteProject(aProjectModel.getObject());

			}

			@Override
			public boolean isEnabled() {
				return enabled;

			}

			@Override
			public void onSubmit() {

				@SuppressWarnings({ "resource" })
				HttpClient httpclient = new DefaultHttpClient();
				try {
					HttpPost httppost = new HttpPost(
							"http://aspra11.informatik.uni-leipzig.de:8080/"
									+ "TEI-Integration/collection/addAnnotations?user=pws&pass=showcase");

					File exportTempDir = File.createTempFile("webanno",
							"export");
					exportTempDir.delete();
					exportTempDir.mkdirs();

					File metaInfDir = new File(exportTempDir + META_INF);
					FileUtils.forceMkdir(metaInfDir);

					boolean curationDocumentExist = existsCurationDocument(aProjectModel
							.getObject());

					if (!curationDocumentExist) {
						error("No curation document created yet for this document");
						return;
					}
					// copy curated documents into the export folder
					exportCuratedDocuments(aProjectModel.getObject(),
							exportTempDir);
					// copy META_INF contents from the project directory to the
					// export folder
					FileUtils.copyDirectory(new File(repository.getDir(),
							"/project/" + aProjectModel.getObject().getId()
									+ META_INF), metaInfDir);

					DaoUtils.zipFolder(exportTempDir,
							new File(exportTempDir.getAbsolutePath() + ".zip"));

					FileEntity reqEntity = new FileEntity(new File(
							exportTempDir.getAbsolutePath() + ".zip"),
							"application/octet-stream");

					httppost.setEntity(reqEntity);

					HttpResponse response = httpclient.execute(httppost);
					HttpEntity resEntity = response.getEntity();

					info(response.getStatusLine().toString());
					EntityUtils.consume(resEntity);
				} catch (ClientProtocolException e) {
					error(ExceptionUtils.getRootCause(e));
				} catch (IOException e) {
					error(e.getMessage());
				} catch (UIMAException e) {
					error(ExceptionUtils.getRootCause(e));
				} catch (ClassNotFoundException e) {
					error(ExceptionUtils.getRootCause(e));
				} catch (Exception e) {
					error(ExceptionUtils.getRootCause(e));
				} finally {
					try {
						httpclient.getConnectionManager().shutdown();
					} catch (Exception e) {
						error(ExceptionUtils.getRootCause(e));
					}
				}

			}
		}).setOutputMarkupId(true);

		add(new DownloadLink("export", new LoadableDetachableModel<File>() {
			private static final long serialVersionUID = 840863954694163375L;

			@Override
			protected File load() {
				File exportFile = null;
				File exportTempDir = null;
				try {
					exportTempDir = File.createTempFile("webanno", "export");
					exportTempDir.delete();
					exportTempDir.mkdirs();

					boolean curationDocumentExist = existsCurationDocument(aProjectModel
							.getObject());

					if (!curationDocumentExist) {
						error("No curation document created yet for this document");
					} else {
						exportCuratedDocuments(aProjectModel.getObject(),
								exportTempDir);
						DaoUtils.zipFolder(exportTempDir, new File(
								exportTempDir.getAbsolutePath() + ".zip"));
						exportFile = new File(exportTempDir.getAbsolutePath()
								+ ".zip");

					}
				} catch (IOException e) {
					error(e.getMessage());
				} catch (Exception e) {
					error(e.getMessage());
				} finally {
					try {
						FileUtils.forceDelete(exportTempDir);
					} catch (IOException e) {
						error("Unable to delete temp file");
					}
				}

				return exportFile;
			}
		}) {
			private static final long serialVersionUID = 5630612543039605914L;

			@Override
			public boolean isVisible() {
				return existsCurationDocument(aProjectModel.getObject());

			}

			@Override
			public boolean isEnabled() {
				return enabled;

			}
		}.setDeleteAfterDownload(true)).setOutputMarkupId(true);

		final AJAXDownload exportProject = new AJAXDownload();

		fileGenerationProgress = new ProgressBar("progress",
				new ProgressionModel() {
					private static final long serialVersionUID = 1971929040248482474L;

					@Override
					protected Progression getProgression() {
						return new Progression(progress);
					}
				}) {
			private static final long serialVersionUID = -6599620911784164177L;

			@Override
			protected void onFinished(AjaxRequestTarget target) {

				if (!canceled && !fileName.equals(downloadedFile)) {
					exportProject.initiate(target, fileName);
					downloadedFile = fileName;

					enabled = true;
					ProjectPage.visible = true;
					target.add(ProjectPage.projectSelectionForm
							.setEnabled(true));
					target.add(ProjectPage.projectDetailForm);
					target.add(feedbackPanel);
					info("project export processing done");
				} else if (canceled) {
					enabled = true;
					ProjectPage.visible = true;
					target.add(ProjectPage.projectSelectionForm
							.setEnabled(true));
					target.add(ProjectPage.projectDetailForm);
					target.add(feedbackPanel);
					info("project export processing canceled");
				}

			}
		};

		fileGenerationProgress.add(exportProject);
		add(fileGenerationProgress);

		add(exportProjectLink = new AjaxLink<Void>("exportProject") {
			private static final long serialVersionUID = -5758406309688341664L;

			@Override
			public boolean isEnabled() {
				return enabled;

			}

			@Override
			public void onClick(final AjaxRequestTarget target) {
				enabled = false;
				canceled = true;
				progress = 0;
				ProjectPage.projectSelectionForm.setEnabled(false);
				ProjectPage.visible = false;
				target.add(ProjectExportPanel.this.getPage());
				fileGenerationProgress.start(target);
				runnable = new FileGenerator(aProjectModel, target);
				thread = new Thread(runnable);
				thread.start();
			}

		});

		add(new AjaxLink<Void>("cancel") {
			private static final long serialVersionUID = 5856284172060991446L;

			@Override
			public void onClick(final AjaxRequestTarget target) {
				if (thread != null) {
					progress = 100;
					thread.interrupt();
				}
			}

		});

	}

	public File generateZipFile(final Model<Project> aProjectModel,
			AjaxRequestTarget target) throws IOException, UIMAException,
			ClassNotFoundException, ZippingException,
			InterruptedException, ProjectExportException {
		File exportTempDir = null;
		// all metadata and project settings data from the database as JSON file
		File projectSettings = null;
		projectSettings = File.createTempFile(EXPORTED_PROJECT, ".json");
		// Directory to store source documents and annotation documents
		exportTempDir = File.createTempFile("webanno-project", "export");
		exportTempDir.delete();
		exportTempDir.mkdirs();

		File projectZipFile = new File(exportTempDir.getAbsolutePath() + ".zip");
		if (aProjectModel.getObject().getId() == 0) {
			throw new ProjectExportException(
					"Project not yet created. Please save project details first!");
		}

		exportProjectSettings(aProjectModel.getObject(), projectSettings,
				exportTempDir);
		progress = 9;
		exportSourceDocuments(aProjectModel.getObject(), exportTempDir);
		exportAnnotationDocuments(aProjectModel.getObject(), exportTempDir);
		exportProjectLog(aProjectModel.getObject(), exportTempDir);
		exportGuideLine(aProjectModel.getObject(), exportTempDir);
		exportProjectMetaInf(aProjectModel.getObject(), exportTempDir);
		progress = 90;
		exportCuratedDocuments(aProjectModel.getObject(), exportTempDir);
		try {
			DaoUtils.zipFolder(exportTempDir, projectZipFile);
		} catch (Exception e) {
			throw new ZippingException("Unable to Zipp the file");
		} finally {
			FileUtils.forceDelete(projectSettings);
			System.gc();
			FileUtils.forceDelete(exportTempDir);
		}
		progress = 100;

		return projectZipFile;
	}

	/**
	 * Copy source documents from the file system of this project to the export
	 * folder
	 */
	private void exportSourceDocuments(Project aProject, File aCopyDir)
			throws IOException {
		File sourceDocumentDir = new File(aCopyDir + SOURCE);
		FileUtils.forceMkdir(sourceDocumentDir);
		// Get all the source documents from the project
		List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = repository
				.listSourceDocuments(aProject);
		documents.addAll(repository.listTabSepDocuments(aProject));
		int i = 1;
		for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
			FileUtils.copyFileToDirectory(
					repository.exportSourceDocument(sourceDocument),
					sourceDocumentDir);
			progress = (int) Math.ceil(((double) i)/documents.size()*10.0);
			        i++;
		}
	}

	/**
	 * Copy, if exists, curation documents to a folder that will be exported as
	 * Zip file
	 *
	 * @param aProject
	 *            The {@link Project}
	 * @param aCurationDocumentExist
	 *            Check if Curation document exists
	 * @param aCopyDir
	 *            The folder where curated documents are copied to be exported
	 *            as Zip File
	 */
	private void exportCuratedDocuments(Project aProject, File aCopyDir)
			throws FileNotFoundException, UIMAException, IOException,
			ClassNotFoundException {

		// Get all the source documents from the project
		List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = repository
				.listSourceDocuments(aProject);

		int initProgress = progress-1;
		int i = 1;
		for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

			File curationCasDir = new File(aCopyDir
					+ CURATION_AS_SERIALISED_CAS + sourceDocument.getName());
			FileUtils.forceMkdir(curationCasDir);

			File curationDir = new File(aCopyDir + CURATION
					+ sourceDocument.getName());
			FileUtils.forceMkdir(curationDir);

			// If the curation document is exist (either finished or in progress
			if (sourceDocument.getState().equals(
					SourceDocumentState.CURATION_FINISHED)
					|| sourceDocument.getState().equals(
							SourceDocumentState.CURATION_IN_PROGRESS)) {

				File CurationFileAsSerialisedCas = repository
						.exportserializedCas(sourceDocument, CURATION_USER);
				File curationFile = null;
				if (CurationFileAsSerialisedCas.exists()) {
					curationFile = repository.exportAnnotationDocument(
							sourceDocument, CURATION_USER, WebannoCustomTsvWriter.class,
							sourceDocument.getName(), Mode.CURATION);
				}
				// in Case they didn't exist
				if (CurationFileAsSerialisedCas.exists()) {
					FileUtils.copyFileToDirectory(curationFile, curationDir);
					FileUtils.copyFileToDirectory(CurationFileAsSerialisedCas,
							curationCasDir);
					FileUtils.forceDelete(curationFile);
				}
			}

			// If this project is a correction project, add the auto-annotated
			// CAS to same folder as
			// CURATION
			if (aProject.getMode().equals(Mode.AUTOMATION)
					|| aProject.getMode().equals(Mode.CORRECTION)) {
				File CorrectionFileAsSerialisedCas = repository
						.exportserializedCas(sourceDocument, CORRECTION_USER);
				File correctionFile = null;
				if (CorrectionFileAsSerialisedCas.exists()) {
					correctionFile = repository.exportAnnotationDocument(
							sourceDocument, CORRECTION_USER, WebannoCustomTsvWriter.class,
							sourceDocument.getName(), Mode.CORRECTION);
				}
				// in Case they didn't exist
				if (CorrectionFileAsSerialisedCas.exists()) {
					FileUtils.copyFileToDirectory(correctionFile, curationDir);
					FileUtils.copyFileToDirectory(
							CorrectionFileAsSerialisedCas, curationCasDir);
					FileUtils.forceDelete(correctionFile);
				}
			}
			progress = initProgress+ (int) Math.ceil(((double) i)/documents.size()*10.0);
			i++;
		}
	}

	/**
	 * Copy Project logs from the file system of this project to the export
	 * folder
	 */
	private void exportProjectLog(Project aProject, File aCopyDir)
			throws IOException {
		File logDir = new File(aCopyDir + LOG);
		FileUtils.forceMkdir(logDir);
		if (repository.exportProjectLog(aProject).exists()) {
			FileUtils.copyFileToDirectory(
					repository.exportProjectLog(aProject), logDir);

		}
	}

	/**
	 * Copy Project guidelines from the file system of this project to the
	 * export folder
	 */
	private void exportGuideLine(Project aProject, File aCopyDir)
			throws IOException {
		File guidelineDir = new File(aCopyDir + GUIDELINE);
		FileUtils.forceMkdir(guidelineDir);
		File annotationGuidlines = repository.exportGuidelines(aProject);
		if (annotationGuidlines.exists()) {
			for (File annotationGuideline : annotationGuidlines.listFiles()) {
				FileUtils
						.copyFileToDirectory(annotationGuideline, guidelineDir);
			}
		}
	}

	/**
	 * Copy Project guidelines from the file system of this project to the
	 * export folder
	 */
	private void exportProjectMetaInf(Project aProject, File aCopyDir)
			throws IOException {
		File metaInfDir = new File(aCopyDir + META_INF);
		FileUtils.forceMkdir(metaInfDir);
		File metaInf = repository.exportProjectMetaInf(aProject);
		if (metaInf.exists()) {
			FileUtils.copyDirectory(metaInf, metaInfDir);
		}

	}

	/**
	 * Copy annotation document as Serialized CAS from the file system of this
	 * project to the export folder
	 *
	 * @throws ClassNotFoundException
	 * @throws UIMAException
	 */
	private void exportAnnotationDocuments(Project aProject, File aCopyDir)
			throws IOException, UIMAException,
			ClassNotFoundException {
		List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = repository
				.listSourceDocuments(aProject);
		int i = 1;
		int initProgress = progress;
		for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
			for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : repository
					.listAnnotationDocuments(sourceDocument)) {

				// copy annotation document only for ACTIVE users and the state
				// of the annotation
				// document
				// is not NEW/IGNOR
				if (userRepository.get(annotationDocument.getUser()) != null
						&& !annotationDocument.getState().equals(
								AnnotationDocumentState.NEW)
						&& !annotationDocument.getState().equals(
								AnnotationDocumentState.IGNORE)) {
					File annotationDocumentAsSerialisedCasDir = new File(
							aCopyDir.getAbsolutePath()
									+ ANNOTATION_AS_SERIALISED_CAS
									+ sourceDocument.getName());
					File annotationDocumentDir = new File(
							aCopyDir.getAbsolutePath() + ANNOTATION
									+ sourceDocument.getName());

					FileUtils.forceMkdir(annotationDocumentAsSerialisedCasDir);
					FileUtils.forceMkdir(annotationDocumentDir);

					File annotationFileAsSerialisedCas = repository
							.exportserializedCas(sourceDocument,
									annotationDocument.getUser());

					File annotationFile = null;
                    Class<?> writer = repository.getWritableFormats().get(
                            sourceDocument.getFormat());
                    if (writer == null) {
                        warn("No writer found for source document format ["
                                + sourceDocument.getFormat()
                                + "] - export will only contain serialized CAS files.");
                    }
                    if (annotationFileAsSerialisedCas.exists() && writer != null) {
						annotationFile = repository.exportAnnotationDocument(
								sourceDocument, annotationDocument.getUser(),
								writer, annotationDocument.getUser(),
								Mode.ANNOTATION, false);
					}
					if (annotationFileAsSerialisedCas.exists()) {
						FileUtils.copyFileToDirectory(
								annotationFileAsSerialisedCas,
								annotationDocumentAsSerialisedCasDir);
						if (writer != null) {
							FileUtils.copyFileToDirectory(annotationFile,
									annotationDocumentDir);
							FileUtils.forceDelete(annotationFile);
						}

					}
				}
			}
			progress = initProgress + (int) Math.ceil(((double) i)/documents.size()*80.0);
			i++;
		}
	}

	private boolean existsCurationDocument(Project aProject) {
		boolean curationDocumentExist = false;
		List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = repository
				.listSourceDocuments(aProject);

		for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

			// If the curation document is exist (either finished or in progress
			if (sourceDocument.getState().equals(
					SourceDocumentState.CURATION_FINISHED)
					|| sourceDocument.getState().equals(
							SourceDocumentState.CURATION_IN_PROGRESS)) {
				curationDocumentExist = true;
				break;
			}
		}
		return curationDocumentExist;
	}

	private void exportProjectSettings(Project aProject, File aProjectSettings,
			File aExportTempDir) {
		de.tudarmstadt.ukp.clarin.webanno.model.export.Project exProjekt = new de.tudarmstadt.ukp.clarin.webanno.model.export.Project();
		exProjekt.setDescription(aProject.getDescription());
		exProjekt.setName(aProject.getName());
		exProjekt.setMode(aProject.getMode());
		exProjekt.setVersion(aProject.getVersion());

		List<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer> exLayers = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer>();
		// Store map of layer and its equivalent exLayer so that the attach type
		// is attached later
		Map<AnnotationLayer, de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer> layerToExLayers = new HashMap<AnnotationLayer, de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer>();
		// Store map of feature and its equivalent exFeature so that the attach
		// feature is attached
		// later
		Map<AnnotationFeature, de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature> featureToExFeatures = new HashMap<AnnotationFeature, de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature>();
		for (AnnotationLayer layer : annotationService
				.listAnnotationLayer(aProject)) {
			exLayers.add(ProjectUtil.exportLayerDetails(layerToExLayers,
					featureToExFeatures, layer, annotationService));
		}

		// add the attach type and attache feature to the exported layer and
		// exported feature
		for (AnnotationLayer layer : layerToExLayers.keySet()) {
			if (layer.getAttachType() != null) {
				layerToExLayers.get(layer).setAttachType(
						layerToExLayers.get(layer.getAttachType()));
			}
			if (layer.getAttachFeature() != null) {
				layerToExLayers.get(layer).setAttachFeature(
						featureToExFeatures.get(layer.getAttachFeature()));
			}
		}
		exProjekt.setLayers(exLayers);

		List<de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet> extTagSets = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet>();
		for (TagSet tagSet : annotationService.listTagSets(aProject)) {
			de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet exTagSet = new de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet();
			exTagSet.setCreateTag(tagSet.isCreateTag());
			exTagSet.setDescription(tagSet.getDescription());
			exTagSet.setLanguage(tagSet.getLanguage());
			exTagSet.setName(tagSet.getName());
			List<de.tudarmstadt.ukp.clarin.webanno.model.export.Tag> exTags = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.Tag>();
			for (Tag tag : annotationService.listTags(tagSet)) {
				de.tudarmstadt.ukp.clarin.webanno.model.export.Tag exTag = new de.tudarmstadt.ukp.clarin.webanno.model.export.Tag();
				exTag.setDescription(tag.getDescription());
				exTag.setName(tag.getName());
				exTags.add(exTag);
			}
			exTagSet.setTags(exTags);
			extTagSets.add(exTagSet);
		}

		exProjekt.setTagSets(extTagSets);
		List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>();
		List<AnnotationDocument> annotationDocuments = new ArrayList<AnnotationDocument>();

		// Store map of source document and exSourceDocument
		Map<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument, SourceDocument> exDocuments = new HashMap<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument, SourceDocument>();
		// add source documents to a project
		List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = repository
				.listSourceDocuments(aProject);
		documents.addAll(repository.listTabSepDocuments(aProject));
		for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {

			SourceDocument exDocument = new SourceDocument();
			exDocument.setFormat(sourceDocument.getFormat());
			exDocument.setName(sourceDocument.getName());
			exDocument.setState(sourceDocument.getState());
			exDocument.setProcessed(sourceDocument.isProcessed());
			exDocument.setTimestamp(sourceDocument.getTimestamp());
			exDocument.setTrainingDocument(sourceDocument.isTrainingDocument());
			exDocument
					.setSentenceAccessed(sourceDocument.getSentenceAccessed());
			exDocument.setProcessed(false);

			if (sourceDocument.getFeature() != null) {
				exDocument.setFeature(featureToExFeatures.get(sourceDocument
						.getFeature()));
			}

			// add annotation document to Project
			for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : repository
					.listAnnotationDocuments(sourceDocument)) {
				AnnotationDocument annotationDocumentToExport = new AnnotationDocument();
				annotationDocumentToExport
						.setName(annotationDocument.getName());
				annotationDocumentToExport.setState(annotationDocument
						.getState());
				annotationDocumentToExport
						.setUser(annotationDocument.getUser());
				annotationDocuments.add(annotationDocumentToExport);
			}
			sourceDocuments.add(exDocument);
			exDocuments.put(sourceDocument, exDocument);
		}

		exProjekt.setSourceDocuments(sourceDocuments);
		exProjekt.setAnnotationDocuments(annotationDocuments);

		List<de.tudarmstadt.ukp.clarin.webanno.model.export.CrowdJob> exCrowdJobs = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.CrowdJob>();
		for (CrowdJob crowdJob : repository.listCrowdJobs(aProject)) {

			de.tudarmstadt.ukp.clarin.webanno.model.export.CrowdJob exCrowdJob = new de.tudarmstadt.ukp.clarin.webanno.model.export.CrowdJob();
			exCrowdJob.setApiKey(crowdJob.getApiKey());
			exCrowdJob.setLink(crowdJob.getLink());
			exCrowdJob.setName(crowdJob.getName());
			exCrowdJob.setStatus(crowdJob.getStatus());
			exCrowdJob.setTask1Id(crowdJob.getTask1Id());
			exCrowdJob.setTask2Id(crowdJob.getTask2Id());
			exCrowdJob.setUseGoldSents(crowdJob.getUseGoldSents());
			exCrowdJob.setUseSents(crowdJob.getUseSents());

			Set<SourceDocument> docs = new HashSet<SourceDocument>();

			for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument document : crowdJob
					.getDocuments()) {
				docs.add(exDocuments.get(document));
			}

			Set<SourceDocument> goldDocs = new HashSet<SourceDocument>();
			for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument document : crowdJob
					.getGoldDocuments()) {
				goldDocs.add(exDocuments.get(document));
			}
			exCrowdJob.setDocuments(docs);
			exCrowdJob.setGoldDocuments(goldDocs);
			exCrowdJobs.add(exCrowdJob);
		}
		exProjekt.setCrowdJobs(exCrowdJobs);

		List<ProjectPermission> projectPermissions = new ArrayList<ProjectPermission>();

		// add project permissions to the project
		for (User user : repository.listProjectUsersWithPermissions(aProject)) {
			for (de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission : repository
					.listProjectPermisionLevel(user, aProject)) {
				ProjectPermission permissionToExport = new ProjectPermission();
				permissionToExport.setLevel(permission.getLevel());
				permissionToExport.setUser(user.getUsername());
				projectPermissions.add(permissionToExport);
			}
		}

		exProjekt.setProjectPermissions(projectPermissions);

		// export automation Mira template
		List<de.tudarmstadt.ukp.clarin.webanno.model.export.MiraTemplate> exTemplates = new ArrayList<de.tudarmstadt.ukp.clarin.webanno.model.export.MiraTemplate>();
		for (MiraTemplate template : repository.listMiraTemplates(aProject)) {
			de.tudarmstadt.ukp.clarin.webanno.model.export.MiraTemplate exTemplate = new de.tudarmstadt.ukp.clarin.webanno.model.export.MiraTemplate();
			exTemplate.setAnnotateAndPredict(template.isAnnotateAndPredict());
			exTemplate.setAutomationStarted(template.isAutomationStarted());
			exTemplate.setCurrentLayer(template.isCurrentLayer());
			exTemplate.setResult(template.getResult());
			exTemplate.setTrainFeature(featureToExFeatures.get(template
					.getTrainFeature()));

			if (template.getOtherFeatures().size() > 0) {
				Set<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature> exOtherFeatures = new HashSet<de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature>();
				for (AnnotationFeature feature : template.getOtherFeatures()) {
					exOtherFeatures.add(featureToExFeatures.get(feature));
				}
				exTemplate.setOtherFeatures(exOtherFeatures);
			}
			exTemplates.add(exTemplate);
		}

		exProjekt.setMiraTemplates(exTemplates);
		MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
		ProjectUtil.setJsonConverter(jsonConverter);

		try {
			ProjectUtil.generateJson(exProjekt, aProjectSettings);
			FileUtils.copyFileToDirectory(aProjectSettings, aExportTempDir);
		} catch (IOException e) {
			error("File Path not found or No permision to save the file!");
		}
	}

	public class FileGenerator implements Runnable {

		Model<Project> projectModel;
		AjaxRequestTarget target;

		public FileGenerator(Model<Project> aProjectModel,
				AjaxRequestTarget aTarget) {
			this.projectModel = aProjectModel;
			this.target = aTarget;
		}

		@Override
		public void run() {

			File file;
			try {
				Thread.sleep(100);
				file = generateZipFile(projectModel, target);
				fileName = file.getAbsolutePath();
				projectName = projectModel.getObject().getName();
				canceled = false;
			}

			catch (UIMAException e) {
				canceled = true;
			} catch (ClassNotFoundException e) {
				canceled = true;
			} catch (IOException e) {
				canceled = true;
			} catch (ZippingException e) {
				canceled = true;
			} catch (InterruptedException e) {
				canceled = true;
			} catch (ProjectExportException e) {
				canceled = true;
			}
		}
	}
}
