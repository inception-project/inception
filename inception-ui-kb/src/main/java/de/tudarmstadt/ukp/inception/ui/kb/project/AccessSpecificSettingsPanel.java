/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.ui.kb.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.TempFileResource;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.SchemaProfile;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.io.FileUploadDownloadHelper;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseInfo;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.ui.kb.project.validators.Validators;

public class AccessSpecificSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = -7834443872889805698L;

    private static final Logger log = LoggerFactory.getLogger(AccessSpecificSettingsPanel.class);

    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    // Remote
    private final Map<String, KnowledgeBaseProfile> knowledgeBaseProfiles;
    private static final int MAXIMUM_REMOTE_REPO_SUGGESTIONS = 10;
    private WebMarkupContainer infoContainerRemote;

    // Local
    private FileUploadField fileUpload;
    private WebMarkupContainer listViewContainer;
    private KnowledgeBaseProfile selectedKnowledgeBaseProfile;
    private static final String CLASSPATH_PREFIX = "classpath:";
    private final Map<String, KnowledgeBaseProfile> downloadedProfiles;
    private final Map<String, File> uploadedFiles;
    private WebMarkupContainer infoContainerLocal;

    //Both
    private CompoundPropertyModel<KnowledgeBaseInfo> kbInfoModel;


    /**
     * Given the default file extension of an RDF format, returns the corresponding
     * {@link RDFFormat}. This factory method detour is necessary because {@link RDFFormat} should
     * be used as a model, but is not serializable.
     *
     * @return an {@link RDFFormat}
     */
    private static final RDFFormat getRdfFormatForFileExt(String fileExt)
    {
        return EXPORT_FORMATS.stream().filter(f -> f.getDefaultFileExtension().equals(fileExt))
            .findAny().get();
    }
    private static final List<RDFFormat> EXPORT_FORMATS = Arrays
        .asList(RDFFormat.RDFXML, RDFFormat.NTRIPLES, RDFFormat.TURTLE);
    private static final List<String> EXPORT_FORMAT_FILE_EXTENSIONS = EXPORT_FORMATS.stream()
        .map(f -> f.getDefaultFileExtension()).collect(Collectors.toList());

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbproperties;

    private TextField<String> urlField;
    private TextField<String> defaultDatasetField;
    
    public AccessSpecificSettingsPanel(String id,
        CompoundPropertyModel<KnowledgeBaseWrapper> aModel,
        Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(id);
        setOutputMarkupId(true);

        kbModel = aModel;
        knowledgeBaseProfiles = aKnowledgeBaseProfiles;
        downloadedProfiles = new HashMap<>();
        uploadedFiles = new HashMap<>();
        kbModel.getObject().clearFiles();
        kbInfoModel = CompoundPropertyModel.of(Model.of());

        boolean isHandlingLocalRepository =
            kbModel.getObject().getKb().getType() == RepositoryType.LOCAL;

        // container for form components related to local KBs
        WebMarkupContainer local = new WebMarkupContainer("localSpecificSettings");
        add(local);
        local.setVisibilityAllowed(isHandlingLocalRepository);
        setUpLocalSpecificSettings(local);

        // container for form components related to remote KBs
        WebMarkupContainer remote = new WebMarkupContainer("remoteSpecificSettings");
        add(remote);
        remote.setVisibilityAllowed(!isHandlingLocalRepository);
        setUpRemoteSpecificSettings(remote);
    }

    private void setUpRemoteSpecificSettings(WebMarkupContainer wmc) {
        urlField = new RequiredTextField<>("url");
        urlField.add(Validators.URL_VALIDATOR);
        wmc.add(urlField);

        // for up to MAXIMUM_REMOTE_REPO_SUGGESTIONS of knowledge bases, create a link which
        // directly fills in the URL field (convenient for both developers AND users :))
        List<KnowledgeBaseProfile> suggestions = new ArrayList<>(
            knowledgeBaseProfiles.values().stream()
                .filter(kb -> RepositoryType.REMOTE.equals(kb.getType()))
                .collect(Collectors.toList()));
        suggestions = suggestions.subList(0,
            Math.min(suggestions.size(), MAXIMUM_REMOTE_REPO_SUGGESTIONS));

        infoContainerRemote = createKbInfoContainer("infoContainer");
        infoContainerRemote.setOutputMarkupId(true);
        wmc.add(infoContainerRemote);
        wmc.add(remoteSuggestionsList("suggestions", suggestions));

        defaultDatasetField = defaultDataset("defaultDataset",
                kbModel.bind("kb.defaultDatasetIri"));
        wmc.add(defaultDatasetField);
    }

    private TextField<String> defaultDataset(String aId, IModel<IRI> model)
    {
        IModel<String> adapter = new LambdaModelAdapter<String>(() -> {
            return model.getObject() != null ? model.getObject().stringValue() : null;
        }, str -> {
            model.setObject(str != null ? SimpleValueFactory.getInstance().createIRI(str) : null);
        });
        TextField<String> defaultDataset = new TextField<>(aId, adapter);
        defaultDataset.add(Validators.IRI_VALIDATOR);
        return defaultDataset;
    }

    private ListView<KnowledgeBaseProfile> remoteSuggestionsList(String aId,
        List<KnowledgeBaseProfile> aSuggestions)
    {
        return new ListView<KnowledgeBaseProfile>(aId, aSuggestions)
        {
            private static final long serialVersionUID = 4179629475064638272L;

            @Override protected void populateItem(ListItem<KnowledgeBaseProfile> item)
            {
                // add a link for one knowledge base with proper label
                LambdaAjaxLink link = new LambdaAjaxLink("suggestionLink", t -> {
                    // set all the fields according to the chosen profile
                    kbModel.getObject().setUrl(item.getModelObject().getAccess().getAccessUrl());
                    // sets root concepts list - if null then an empty list otherwise change the
                    // values to IRI and populate the list
                    kbModel.getObject().getKb().applyRootConcepts(item.getModelObject());
                    kbModel.getObject().getKb().applyMapping(item.getModelObject().getMapping());
                    kbInfoModel.setObject(item.getModelObject().getInfo());
                    kbModel.getObject().getKb().setFullTextSearchIri(
                        item.getModelObject().getAccess().getFullTextSearchIri());
                    kbModel.getObject().getKb()
                        .setDefaultLanguage(item.getModelObject().getDefaultLanguage());
                    kbModel.getObject().getKb()
                        .setDefaultDatasetIri(item.getModelObject().getDefaultDataset());
                    kbModel.getObject().getKb()
                        .setReification(item.getModelObject().getReification());
                    t.add(urlField, defaultDatasetField, infoContainerRemote);
                });
                link.add(new Label("suggestionLabel", item.getModelObject().getName()));
                item.add(link);
            }
        };
    }

    private WebMarkupContainer createKbInfoContainer(String aId)
    {
        WebMarkupContainer wmc = new WebMarkupContainer(aId);
        wmc.add(new Label("description", kbInfoModel.bind("description"))
            .add(LambdaBehavior.visibleWhen(() -> kbInfoModel.getObject() != null)));
        wmc.add(new Label("hostInstitutionName", kbInfoModel.bind("hostInstitutionName"))
            .add(LambdaBehavior.visibleWhen(() -> kbInfoModel.getObject() != null)));
        wmc.add(new Label("authorName", kbInfoModel.bind("authorName"))
            .add(LambdaBehavior.visibleWhen(() -> kbInfoModel.getObject() != null)));
        wmc.add(new ExternalLink("websiteURL", kbInfoModel.bind("websiteURL"),
            kbInfoModel.bind("websiteURL"))
            .add(LambdaBehavior.visibleWhen(() -> kbInfoModel.getObject() != null)));
        return wmc;
    }

    private void setUpLocalSpecificSettings(WebMarkupContainer wmc)
    {
        wmc.add(uploadForm("uploadForm", "uploadField"));

        // add link for clearing the knowledge base contents, enabled only, if there is
        // something to clear
        AjaxLink<Void> clearLink = clearLink("clear");
        wmc.add(clearLink);

        wmc.add(fileExtensionsExportList("exportButtons"));

        List<KnowledgeBaseProfile> localKBs = knowledgeBaseProfiles.values().stream()
            .filter(kb -> RepositoryType.LOCAL.equals(kb.getType()))
            .collect(Collectors.toList());

        listViewContainer = new WebMarkupContainer("listViewContainer");
        ListView<KnowledgeBaseProfile> suggestions = localSuggestionsList("localKBs", localKBs);
        listViewContainer.add(suggestions);
        listViewContainer.setOutputMarkupPlaceholderTag(true);

        LambdaAjaxLink addKbButton = new LambdaAjaxLink("addKbButton",
            this::actionDownloadKbAndSetIRIs);
        addKbButton.add(new Label("addKbLabel", new ResourceModel("kb.wizard.steps.local.addKb")));
        listViewContainer.add(addKbButton);

        infoContainerLocal = createKbInfoContainer("infoContainer");
        infoContainerLocal.setOutputMarkupId(true);
        wmc.add(infoContainerLocal);

        wmc.add(listViewContainer);
    }

    private Form<Void> uploadForm(String aFormId, String aFieldId) {
        Form<Void> importProjectForm = new Form<Void>(aFormId) {
            private static final long serialVersionUID = -8284858297362896476L;
            
            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                handleUploadedFiles();
            }
        };

        FileInputConfig config = new FileInputConfig();
        config.initialCaption("Import project archives ...");
        config.showPreview(false);
        config.showUpload(false);
        config.removeIcon("<i class=\"fa fa-remove\"></i>");
//        config.uploadIcon("<i class=\"fa fa-upload\"></i>");
        config.browseIcon("<i class=\"fa fa-folder-open\"></i>");
        importProjectForm.add(fileUpload = new BootstrapFileInputField(aFieldId,
            new ListModel<>(), config));
        return importProjectForm;
    }
    
    public void handleUploadedFiles()
    {
        try {
            for (FileUpload fu : fileUpload.getFileUploads()) {
                File tmp = uploadFile(fu);
                kbModel.getObject().putFile(fu.getClientFileName(), tmp);
            }
        }
        catch (Exception e) {
            log.error("Error while uploading files", e);
            error("Could not upload files");
        }
    }

    private  AjaxLink<Void> clearLink(String aId) {
        AjaxLink<Void> clearLink = new LambdaAjaxLink(aId, this::actionClear)
        {

            private static final long serialVersionUID = -6272361381689154558L;

            @Override public boolean isEnabled()
            {
                return !kbService.isEmpty(kbModel.getObject().getKb());
            }
        };
        return clearLink;
    }

    private ListView<String> fileExtensionsExportList(String aId) {
        ListView<String> fileExListView = new ListView<String>(aId,
            EXPORT_FORMAT_FILE_EXTENSIONS)
        {

            private static final long serialVersionUID = -1869762759620557362L;

            @Override protected void populateItem(ListItem<String> item)
            {
                // creates an appropriately labeled {@link AjaxDownloadLink} which triggers the
                // download of the contents of the current KB in the given format
                String fileExtension = item.getModelObject();
                Model<String> exportFileNameModel = Model
                    .of(kbModel.getObject().getKb().getName() + "." + fileExtension);
                AjaxDownloadLink exportLink = new AjaxDownloadLink("link", exportFileNameModel,
                        LambdaModel.of(() -> actionExport(fileExtension)));
                exportLink
                    .add(new Label("label", new ResourceModel("kb.export." + fileExtension)));
                item.add(exportLink);
            }
        };
        return fileExListView;
    }

    private ListView<KnowledgeBaseProfile> localSuggestionsList(String aId,
        List<KnowledgeBaseProfile> localKBs)
    {
        ListView<KnowledgeBaseProfile> suggestions = new ListView<KnowledgeBaseProfile>(
            aId, localKBs)
        {
            private static final long serialVersionUID = 1L;

            @Override protected void populateItem(ListItem<KnowledgeBaseProfile> item)
            {
                LambdaAjaxLink link = new LambdaAjaxLink("suggestionLink", _target ->
                        actionSelectPredefinedKB(_target, item.getModel()));

                // Can not import the same KB more than once
                boolean isImported = downloadedProfiles
                    .containsKey(item.getModelObject().getName());
                link.setEnabled(!isImported);

                String itemLabel = item.getModelObject().getName();
                // Adjust label to indicate whether the KB has already been downloaded
                if (isImported) {
                    // \u2714 is the checkmark symbol
                    itemLabel = itemLabel + "  \u2714";
                }
                link.add(new Label("suggestionLabel", itemLabel));
                
                link.add(new ClassAttributeModifier() {
                    private static final long serialVersionUID = -3985182168502826951L;

                    @Override
                    protected Set<String> update(Set<String> aOldClasses)
                    {
                        if (Objects.equals(selectedKnowledgeBaseProfile, item.getModelObject())) {
                            aOldClasses.add("active");
                        }
                        else {
                            aOldClasses.remove("active");
                        }
                        return aOldClasses;
                    }
                });

                // Show schema type on mouseover
                link.add(AttributeModifier.append("title",
                    new StringResourceModel("kb.wizard.steps.local.schemaOnMouseOver", this)
                        .setParameters(
                            SchemaProfile.checkSchemaProfile(item.getModelObject()).getUiLabel(),
                            getAccessTypeLabel(item.getModelObject()))));

                item.add(link);
            }
        };
        suggestions.setOutputMarkupId(true);
        return suggestions;
    }
    
    private void actionSelectPredefinedKB(AjaxRequestTarget aTarget,
            IModel<KnowledgeBaseProfile> aModel)
    {
        if (Objects.equals(selectedKnowledgeBaseProfile, aModel.getObject())) {
            selectedKnowledgeBaseProfile = null;
        }
        else {
            selectedKnowledgeBaseProfile = aModel.getObject();
            kbInfoModel.setObject(aModel.getObject().getInfo());
        }
        aTarget.add(listViewContainer, infoContainerLocal);
    }

    private File uploadFile(FileUpload fu) throws IOException
    {
        String fileName = fu.getClientFileName();
        if (!uploadedFiles.containsKey(fileName)) {
            FileUploadDownloadHelper fileUploadDownloadHelper = new FileUploadDownloadHelper(
                getApplication());
            File tmpFile = fileUploadDownloadHelper.writeFileUploadToTemporaryFile(fu, kbModel);
            uploadedFiles.put(fileName, tmpFile);
        }
        else {
            log.debug("File [{}] already downloaded, skipping!", fileName);
        }
        return uploadedFiles.get(fileName);
    }

    private void actionClear(AjaxRequestTarget aTarget)
    {
        try {
            kbService.clear(kbModel.getObject().getKb());
            info(getString("kb.details.local.contents.clear.feedback", kbModel.bind("kb")));
            aTarget.add(this);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        catch (RepositoryException e) {
            error("Error clearing KB: " + e.getMessage());
            log.error("Error clearing KB", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private IResourceStream actionExport(String rdfFormatFileExt)
    {
        return new TempFileResource((os) -> kbService
            .exportData(kbModel.getObject().getKb(), getRdfFormatForFileExt(rdfFormatFileExt), os));
    }

    private void actionDownloadKbAndSetIRIs(AjaxRequestTarget aTarget)
    {
        try {
            if (selectedKnowledgeBaseProfile != null) {

                String accessUrl = selectedKnowledgeBaseProfile.getAccess().getAccessUrl();

                FileUploadDownloadHelper fileUploadDownloadHelper =
                    new FileUploadDownloadHelper(getApplication());

                if (!accessUrl.startsWith(CLASSPATH_PREFIX)) {

                    File tmpFile = fileUploadDownloadHelper
                        .writeFileDownloadToTemporaryFile(accessUrl, kbModel);
                    kbModel.getObject().putFile(selectedKnowledgeBaseProfile.getName(), tmpFile);
                }
                else {
                    // import from classpath
                    File kbFile = fileUploadDownloadHelper
                        .writeClasspathResourceToTemporaryFile(accessUrl, kbModel);
                    kbModel.getObject().putFile(selectedKnowledgeBaseProfile.getName(), kbFile);
                }

                kbModel.getObject().getKb().applyRootConcepts(selectedKnowledgeBaseProfile);
                kbModel.getObject().getKb().applyMapping(
                    selectedKnowledgeBaseProfile.getMapping());
                kbModel.getObject().getKb().setFullTextSearchIri(
                    selectedKnowledgeBaseProfile.getAccess().getFullTextSearchIri());
                kbModel.getObject().getKb()
                    .setDefaultLanguage(selectedKnowledgeBaseProfile.getDefaultLanguage());
                kbModel.getObject().getKb()
                    .setReification(selectedKnowledgeBaseProfile.getReification());
                downloadedProfiles
                    .put(selectedKnowledgeBaseProfile.getName(), selectedKnowledgeBaseProfile);
                aTarget.add(this);
                selectedKnowledgeBaseProfile = null;
            }
        }
        catch (IOException e) {
            error("Unable to download or import knowledge base file " + e.getMessage());
            log.error("Unable to download or import knowledge base file ", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private String getAccessTypeLabel(KnowledgeBaseProfile aProfile)
    {
        if (aProfile.getAccess().getAccessUrl().startsWith(CLASSPATH_PREFIX)) {
            return "CLASSPATH";
        }
        else {
            return "DOWNLOAD";
        }
    }
}
