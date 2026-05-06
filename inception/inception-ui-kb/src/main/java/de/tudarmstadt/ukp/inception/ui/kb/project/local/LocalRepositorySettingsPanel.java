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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.rio.RDFFormat.NTRIPLES;
import static org.eclipse.rdf4j.rio.RDFFormat.RDFXML;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.support.io.FileUploadDownloadHelper;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseWrapper;

public class LocalRepositorySettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 866658729983211740L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    private final Map<String, File> uploadedFiles = new HashMap<>();

    private final Label repositorySize;
    private final Label indexSize;
    private final Label statementCount;

    private FileUploadField fileUpload;

    public LocalRepositorySettingsPanel(String aId, IModel<KnowledgeBaseWrapper> aModel,
            Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        queue(new WebMarkupContainer("wizardDescription")
                .add(visibleWhenNot(aModel.map(KnowledgeBaseWrapper::isKbSaved))));

        queue(uploadForm("uploadForm", "uploadField"));

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

    private IResourceStream actionExport(String rdfFormatFileExt)
    {
        var kb = getModel().getObject().getKb();
        var format = getRdfFormatForFileExt(rdfFormatFileExt);
        return new PipedStreamResource((os) -> kbService.exportData(kb, format, os),
                GZIPOutputStream::new);
    }
}
