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
package de.tudarmstadt.ukp.inception.ui.kb.project.local;

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.LOCAL;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.rio.RDFFormat.NTRIPLES;
import static org.eclipse.rdf4j.rio.RDFFormat.RDFXML;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.SchemaProfile;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseInfo;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.support.io.FileUploadDownloadHelper;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;
import de.tudarmstadt.ukp.inception.ui.kb.project.AccessSpecificSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseInfoPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseWrapper;

public class LocalRepositorySettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 866658729983211740L;

    private static final String CID_CLEAR = "clear";

    private static final String CLASSPATH_PREFIX = "classpath:";

    private static final Logger LOG = LoggerFactory.getLogger(AccessSpecificSettingsPanel.class);

    /**
     * Given the default file extension of an RDF format, returns the corresponding
     * {@link RDFFormat}. This factory method detour is necessary because {@link RDFFormat} should
     * be used as a model, but is not serializable.
     *
     * @return an {@link RDFFormat}
     */
    private static final RDFFormat getRdfFormatForFileExt(String fileExt)
    {
        return EXPORT_FORMATS.stream() //
                .filter(f -> f.getDefaultFileExtension().equals(fileExt)) //
                .findAny().get();
    }

    private static final List<RDFFormat> EXPORT_FORMATS = asList(RDFXML, NTRIPLES, TURTLE);
    private static final List<String> EXPORT_FORMAT_FILE_EXTENSIONS = EXPORT_FORMATS.stream()
            .map(f -> f.getDefaultFileExtension()).collect(toList());

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbproperties;

    private final Map<String, KnowledgeBaseProfile> knowledgeBaseProfiles;
    private final Map<String, KnowledgeBaseProfile> downloadedProfiles = new HashMap<>();
    private final Map<String, File> uploadedFiles = new HashMap<>();

    private final WebMarkupContainer listViewContainer;
    private final WebMarkupContainer infoContainerLocal;
    private final CompoundPropertyModel<KnowledgeBaseInfo> kbInfoModel = CompoundPropertyModel
            .of(Model.of());

    private final Label repositorySize;
    private final Label indexSize;
    private final Label statementCount;

    private FileUploadField fileUpload;

    private KnowledgeBaseProfile selectedKnowledgeBaseProfile;

    public LocalRepositorySettingsPanel(String aId, IModel<KnowledgeBaseWrapper> aModel,
            Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        knowledgeBaseProfiles = aKnowledgeBaseProfiles;

        queue(uploadForm("uploadForm", "uploadField"));

        // add link for clearing the knowledge base contents, enabled only, if there is
        // something to clear
        queue(clearLink(CID_CLEAR));

        queue(fileExtensionsExportList("exportButtons"));

        var kbModel = getModel().map(KnowledgeBaseWrapper::getKb);
        var repoSizeModel = kbModel.map(kbService::getRepositorySize).orElse(0l);

        repositorySize = new Label("repositorySize", LoadableDetachableModel
                .of(() -> repoSizeModel.map(FileUtils::byteCountToDisplaySize).getObject()));
        repositorySize.add(visibleWhen(getModel().map(KnowledgeBaseWrapper::isKbSaved)));
        queue(repositorySize);

        indexSize = new Label("indexSize",
                LoadableDetachableModel.of(() -> kbModel.map(kbService::getIndexSize).orElse(0l)
                        .map(FileUtils::byteCountToDisplaySize).getObject()));
        indexSize.add(visibleWhen(getModel().map(KnowledgeBaseWrapper::isKbSaved)));
        queue(indexSize);

        statementCount = new Label("statementCount", LoadableDetachableModel.of(() -> {
            var repoSize = repoSizeModel.getObject();
            if (repoSize > 25_000_000) {
                var avgTripleSize = 140;
                return "~"
                        + NumberFormat.getCompactNumberInstance().format(repoSize / avgTripleSize);
                // return "~" + (((repoSize / avgTripleSize) / 1000) * 1000);
            }

            return kbModel.map(kbService::getStatementCount).orElse(0l).getObject();
        }));
        statementCount.add(visibleWhen(getModel().map(KnowledgeBaseWrapper::isKbSaved)));
        queue(statementCount);

        var localKBs = knowledgeBaseProfiles.values().stream() //
                .filter(kb -> LOCAL == kb.getType()) //
                .collect(Collectors.toList());

        listViewContainer = new WebMarkupContainer("listViewContainer");
        listViewContainer.add(localSuggestionsList("localKBs", localKBs));
        listViewContainer.setOutputMarkupPlaceholderTag(true);
        listViewContainer.add(visibleWhenNot(getModel().map(KnowledgeBaseWrapper::isKbSaved)));

        var addKbButton = new LambdaAjaxLink("addKbButton", this::actionDownloadKbAndSetIRIs);
        addKbButton.add(new Label("addKbLabel", new ResourceModel("kb.wizard.steps.local.addKb")));
        listViewContainer.add(addKbButton);

        infoContainerLocal = new KnowledgeBaseInfoPanel("infoContainer", kbInfoModel);
        infoContainerLocal.setOutputMarkupId(true);
        queue(infoContainerLocal);

        queue(listViewContainer);
    }

    @SuppressWarnings("unchecked")
    public IModel<KnowledgeBaseWrapper> getModel()
    {
        return (IModel<KnowledgeBaseWrapper>) getDefaultModel();
    }

    private Form<Void> uploadForm(String aFormId, String aFieldId)
    {
        var form = new Form<Void>(aFormId);

        var config = new FileInputConfig();
        config.initialCaption("Import knowledge base ...");
        config.showPreview(false);
        config.showUpload(false);
        config.removeIcon("<i class=\"fas fa-times\"></i>");
        config.browseIcon("<i class=\"fa fa-folder-open\"></i>");
        form.add(fileUpload = new BootstrapFileInputField(aFieldId, new ListModel<>(), config));

        return form;
    }

    public void handleUploadedFiles()
    {
        try {
            for (var fu : fileUpload.getFileUploads()) {
                var tmp = uploadFile(fu);
                getModel().getObject().putFile(fu.getClientFileName(), tmp);
            }
        }
        catch (Exception e) {
            LOG.error("Error while uploading files", e);
            error("Could not upload files");
        }
    }

    private AjaxLink<Void> clearLink(String aId)
    {
        var clearLink = new LambdaAjaxLink(aId, this::actionClear);
        clearLink.add(visibleWhen(getModel().map(KnowledgeBaseWrapper::getKb) //
                .map(kb -> kb.getRepositoryId() != null) //
                .orElse(false)));
        clearLink.add(enabledWhen(getModel().map(KnowledgeBaseWrapper::getKb) //
                .map(kb -> kb.getRepositoryId() != null && !kbService.isEmpty(kb)) //
                .orElse(false)));
        return clearLink;

    }

    private ListView<String> fileExtensionsExportList(String aId)
    {
        var fileExListView = new ListView<String>(aId, EXPORT_FORMAT_FILE_EXTENSIONS)
        {
            private static final long serialVersionUID = -1869762759620557362L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                // creates an appropriately labeled {@link AjaxDownloadLink} which triggers the
                // download of the contents of the current KB in the given format
                var fileExtension = item.getModelObject();
                var kb = LocalRepositorySettingsPanel.this.getModel().getObject().getKb();
                var exportFileNameModel = Model.of(kb.getName() + "." + fileExtension + ".gz");
                var exportLink = new AjaxDownloadLink("link", exportFileNameModel,
                        LoadableDetachableModel.of(() -> actionExport(fileExtension)));
                exportLink.add(new Label("label", new ResourceModel("kb.export." + fileExtension)));
                item.add(exportLink);
            }
        };
        fileExListView.add(visibleWhen(getModel().map(KnowledgeBaseWrapper::getKb) //
                .map(kb -> kb.getRepositoryId() != null) //
                .orElse(false)));
        return fileExListView;
    }

    private ListView<KnowledgeBaseProfile> localSuggestionsList(String aId,
            List<KnowledgeBaseProfile> localKBs)
    {
        var suggestions = new ListView<KnowledgeBaseProfile>(aId, localKBs)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<KnowledgeBaseProfile> item)
            {
                var link = new LambdaAjaxLink("suggestionLink",
                        _target -> actionSelectPredefinedKB(_target, item.getModel()));

                // Can not import the same KB more than once
                var isImported = downloadedProfiles.containsKey(item.getModelObject().getName());
                link.setEnabled(!isImported);

                var itemLabel = item.getModelObject().getName();
                // Adjust label to indicate whether the KB has already been downloaded
                if (isImported) {
                    // \u2714 is the checkmark symbol
                    itemLabel = itemLabel + "  \u2714";
                }
                link.add(new Label("suggestionLabel", itemLabel));

                link.add(new ClassAttributeModifier()
                {
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
                link.add(
                        AttributeModifier
                                .append("title",
                                        new StringResourceModel(
                                                "kb.wizard.steps.local.schemaOnMouseOver", this)
                                                        .setParameters(
                                                                SchemaProfile
                                                                        .checkSchemaProfile(item
                                                                                .getModelObject())
                                                                        .getUiLabel(),
                                                                getAccessTypeLabel(
                                                                        item.getModelObject()))));

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
        var fileName = fu.getClientFileName();
        if (!uploadedFiles.containsKey(fileName)) {
            var fileUploadDownloadHelper = new FileUploadDownloadHelper(getApplication());
            var tmpFile = fileUploadDownloadHelper.writeFileUploadToTemporaryFile(fu, getModel());
            uploadedFiles.put(fileName, tmpFile);
        }
        else {
            LOG.debug("File [{}] already downloaded, skipping!", fileName);
        }

        return uploadedFiles.get(fileName);
    }

    private void actionClear(AjaxRequestTarget aTarget)
    {
        try {
            kbService.clear(getModel().getObject().getKb());
            info(getString("kb.details.local.contents.clear.feedback",
                    getModel().map(KnowledgeBaseWrapper::getKb).map(KnowledgeBase::getName)));
            aTarget.add(this);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        catch (RepositoryException e) {
            error("Error clearing KB: " + e.getMessage());
            LOG.error("Error clearing KB", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private IResourceStream actionExport(String rdfFormatFileExt)
    {
        var kb = getModel().getObject().getKb();
        var format = getRdfFormatForFileExt(rdfFormatFileExt);
        return new PipedStreamResource((os) -> kbService.exportData(kb, format, os),
                GZIPOutputStream::new);
    }

    private void actionDownloadKbAndSetIRIs(AjaxRequestTarget aTarget)
    {
        try {
            if (selectedKnowledgeBaseProfile != null) {
                var accessUrl = selectedKnowledgeBaseProfile.getAccess().getAccessUrl();

                var fileUploadDownloadHelper = new FileUploadDownloadHelper(getApplication());

                if (accessUrl == null) {
                    // Nothing to do
                }
                else if (accessUrl.startsWith(CLASSPATH_PREFIX)) {
                    // import from classpath
                    var kbFile = fileUploadDownloadHelper
                            .writeClasspathResourceToTemporaryFile(accessUrl, getModel());
                    getModel().getObject().putFile(selectedKnowledgeBaseProfile.getName(), kbFile);
                }
                else {
                    var tmpFile = fileUploadDownloadHelper
                            .writeFileDownloadToTemporaryFile(accessUrl, getModel());
                    getModel().getObject().putFile(selectedKnowledgeBaseProfile.getName(), tmpFile);
                }

                var kb = getModel().getObject().getKb();
                kb.applyRootConcepts(selectedKnowledgeBaseProfile);
                kb.applyAdditionalMatchingProperties(selectedKnowledgeBaseProfile);
                kb.applyMapping(selectedKnowledgeBaseProfile.getMapping());
                kb.setFullTextSearchIri(
                        selectedKnowledgeBaseProfile.getAccess().getFullTextSearchIri());
                kb.setDefaultLanguage(selectedKnowledgeBaseProfile.getDefaultLanguage());
                kb.setReification(selectedKnowledgeBaseProfile.getReification());
                downloadedProfiles.put(selectedKnowledgeBaseProfile.getName(),
                        selectedKnowledgeBaseProfile);
                aTarget.add(this);

                selectedKnowledgeBaseProfile = null;
            }
        }
        catch (IOException e) {
            error("Unable to download or import knowledge base file " + e.getMessage());
            LOG.error("Unable to download or import knowledge base file ", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private String getAccessTypeLabel(KnowledgeBaseProfile aProfile)
    {
        if (aProfile.getAccess().getAccessUrl() == null) {
            return "MANUAL";
        }

        if (aProfile.getAccess().getAccessUrl().startsWith(CLASSPATH_PREFIX)) {
            return "CLASSPATH";
        }

        return "DOWNLOAD";
    }
}
