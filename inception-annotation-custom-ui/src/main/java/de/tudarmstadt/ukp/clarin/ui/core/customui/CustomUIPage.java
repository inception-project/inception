package de.tudarmstadt.ukp.clarin.ui.core.customui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.include.Include;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.transaction.annotation.Transactional;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * This is used for a Document Test
 */
@MountPath("/CustomUIPage.html")
public class CustomUIPage
    extends ApplicationPageBase
    implements UIScanner
{
    /**
     * 
     */
    private static final long serialVersionUID = -5972434095216188594L;

    private @SpringBean DocumentService documentService;

    private String CUSTOMUI_FOLDER = "customUI";

    /**
     * A form, which will contain the selected custom UI
     * 
     * @author Zlatko Kolev
     *
     */
    private class RequestForm
        extends Form<String>
    {
        private static final long serialVersionUID = -1L;

        /**
         * Constructor, which is called on page creation
         * 
         * @param id
         * @param aModel
         * @param parameters
         */
        public RequestForm(String id, IModel<String> aModel, final PageParameters parameters)
        {
            super(id, new CompoundPropertyModel<>(aModel));

            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);

            // Retrieve index of selected UI
            int index = Integer.parseInt(parameters.get("ui").toString());

            // Read custom UI name into String and add the UI to the markup
            try {
                String customUIName = getUINames(getCustomUIFolder()).get(index);

                // Add custom UI to the container
                String path = "file:///" + documentService.getDir().getAbsolutePath() + "/"
                        + CUSTOMUI_FOLDER + "/" + customUIName;
                this.add(new Include("customUi", path));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            setVisible(true);
        }
    }

    private RequestForm requestForm;

    private IModel<String> selectedMsg;

    /**
     * Constructor for the actual html page
     * 
     * @param parameters
     *            The url parameters
     */
    public CustomUIPage(final PageParameters parameters)
    {
        selectedMsg = Model.of();
        requestForm = new RequestForm("requestForm", selectedMsg, parameters);

        // Show the form on the html page
        requestForm.setVisible(true);
        add(requestForm);
    }

    /**
     * Implementation of the UIScanner interface methods
     */

    @Override
    @Transactional
    public File getCustomUIFolder() throws IOException
    {
        String path = documentService.getDir().getAbsolutePath() + "/" + CUSTOMUI_FOLDER;
        if (!(new File(path)).exists())
            FileUtils.forceMkdir(new File(path));
        File customUIFolder = new File(path);
        return customUIFolder;
    }

    @Override
    @Transactional
    public ArrayList<String> getUINames(File folder)
    {
        return new ArrayList<String>(Arrays.asList(folder.list()));
    }
}
