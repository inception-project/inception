package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap;

import static de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapFileInputField.initConfig;

import java.util.List;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;

public class BootstrapFileInput
    extends de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInput
{
    private static final long serialVersionUID = 7152684712265476472L;

    public BootstrapFileInput(String aId, IModel<List<FileUpload>> aModel, FileInputConfig aConfig)
    {
        super(aId, aModel, aConfig);
        initConfig(getConfig());
    }

    public BootstrapFileInput(String aId, IModel<List<FileUpload>> aModel)
    {
        super(aId, aModel);
        initConfig(getConfig());
    }

    public BootstrapFileInput(String aId)
    {
        super(aId);
        initConfig(getConfig());
    }

    @Override
    public void renderHead(final IHeaderResponse response)
    {
        // Workaround https://github.com/l0rdn1kk0n/wicket-bootstrap/issues/957
        IBootstrapSettings settings = Bootstrap.getSettings(getApplication());
        response.render(
                JavaScriptReferenceHeaderItem.forReference(settings.getJsResourceReference()));
        super.renderHead(response);
    }
}
