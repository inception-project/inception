package de.tudarmstadt.ukp.clarin.ui.core.customui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.transaction.annotation.Transactional;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * This is used for selection of a custom UI
 */
//@MenuItem(icon = "images/information.png", label = "Custom UI Selection")
@MountPath("/UIScannerImpl.html")
public class UIScannerImpl
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
     * Implementation of the UIScanner interface methods
     */

    @Override
    @Transactional
    public File getCustomUIFolder() throws IOException
    {
        String path = documentService.getDir().getAbsolutePath() + "/" + CUSTOMUI_FOLDER;
        if (!(new File(path)).exists()) {
            FileUtils.forceMkdir(new File(path));
        }
        File customUIFolder = new File(path);
        return customUIFolder;
    }

    @Override
    @Transactional
    public ArrayList<String> getUINames(File folder)
    {
        return new ArrayList<String>(Arrays.asList(folder.list()));
    }

    /**
     * Inner class, which represents a form, holding the custom UIs
     * 
     * @author Zlatko Kolev
     *
     */
    private class RequestForm
        extends Form<String>
    {
        private static final long serialVersionUID = -1L;

        /**
         * Constructor
         * 
         * @param id
         * @param aModel
         */
        public RequestForm(String id, IModel<String> aModel)
        {
            super(id, new CompoundPropertyModel<>(aModel));

            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);

            // A list, which shows the custom UIs
            WebMarkupContainer cuilist = new WebMarkupContainer("cuilist");
            try {
                ArrayList<String> uiNames = getUINames(getCustomUIFolder());
                ArrayList<String> selected = (new ArrayList<String>());
                cuilist.add(new ListMultipleChoice<String>("cuis",
                        new Model<ArrayList<String>>(selected), uiNames,
                        new ChoiceRenderer<String>()));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            cuilist.setVisible(true);
            add(cuilist);
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
     * Standard constructor for the html page.
     */
    public UIScannerImpl()
    {
        selectedMsg = Model.of();
        requestForm = new RequestForm("requestForm", selectedMsg);

        // Show the form on the html page
        requestForm.setVisible(true);
        add(requestForm);
    }
}
