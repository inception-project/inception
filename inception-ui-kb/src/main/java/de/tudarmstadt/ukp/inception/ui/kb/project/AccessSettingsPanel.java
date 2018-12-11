package de.tudarmstadt.ukp.inception.ui.kb.project;

import java.util.Arrays;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;

public class AccessSettingsPanel
    extends Panel
{
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    public AccessSettingsPanel(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);
        setOutputMarkupId(true);

        kbModel = aModel;

        add(repositoryTypeRadioButtons("type", "kb.type"));
        add(createCheckbox("writeprotection", "kb.readOnly"));
    }

    private BootstrapRadioGroup<RepositoryType> repositoryTypeRadioButtons(String id,
        String property) {
        // subclassing is necessary for setting this form input as required
        return new BootstrapRadioGroup<RepositoryType>(id, kbModel.bind(property),
            Arrays.asList(RepositoryType.values()),
            new EnumRadioChoiceRenderer<>(Buttons.Type.Default, this)) {

            private static final long serialVersionUID = -3015289695381851498L;

            @Override
            protected RadioGroup<RepositoryType> newRadioGroup(String aId,
                IModel<RepositoryType> aModel)
            {
                RadioGroup<RepositoryType> group = super.newRadioGroup(aId, aModel);
                group.setRequired(true);
                group.add(new AttributeAppender("class", " btn-group-justified"));
                return group;
            }
        };
    }

    private CheckBox createCheckbox(String aId, String aProperty) {
        return new CheckBox(aId, kbModel.bind(aProperty));
    }
}
