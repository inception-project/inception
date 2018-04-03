package de.tudarmstadt.ukp.inception.ui.kb.stmt;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.graph.KBModifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

public class ModifierEditor extends Panel
{
    private static final long serialVersionUID = -4152363403483032196L;

    private static final String CONTENT_MARKUP_ID = "content";

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBModifier> modifier;
    private Component content;

    public ModifierEditor(String id, IModel<KnowledgeBase> aKbModel,
        IModel<KBModifier> aModifier)
    {
        super(id, aModifier);
        setOutputMarkupId(true);

        kbModel = aKbModel;
        modifier = aModifier;

        boolean isNewModifier = (modifier.getObject().getKbProperty()==null);
        if(isNewModifier) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID, modifier, true);
        }
    }

    private class EditMode extends Fragment
        implements Focusable {
        private static final long serialVersionUID = 2333017379066971404L;
        private Component initialFocusComponent;

        /**
         * Creates a new fragement for editing a statement.<br>
         * The editor has two slightly different behaviors, depending on the value of
         * {@code isNewStatement}:
         * <ul>
         * <li>{@code !isNewStatement}: Save button commits changes, cancel button discards unsaved
         * changes, delete button removes the statement from the KB.</li>
         * <li>{@code isNewStatement}: Save button commits changes (creates a new statement in the
         * KB), cancel button removes the statement from the UI, delete button is not visible.</li>
         * </ul>
         *
         * @param aId
         *            markup ID
         * @param aModifier
         *            modifier model
         * @param isNewModifier
         *            whether the modifier being edited is new, meaning it has no corresponding
         *            modifier in the KB backend
         */
        public EditMode(String aId, IModel<KBModifier> aModifier, boolean isNewModifier) {
            super(aId, "editMode", ModifierEditor.this, aModifier);

            Form<KBModifier> form = new Form<>("form", CompoundPropertyModel.of(aModifier));

            // text area for the statement value should receive focus
            Component valueTextArea = new TextArea<String>("value").add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", t -> t.add(getParent())));
            initialFocusComponent = valueTextArea;
            form.add(valueTextArea);

            // FIXME This field should only be visible if the selected datatype is
            // langString
            form.add(new TextField<>("language"));

            // FIXME Selection of the data type should only be possible if it is not
            // restricted to a single type in the property definition - take into account
            // inheritance?
            //form.add(new TextField<>("datatype"));

            // We do not allow the user to change the property

            // FIXME should offer different editors depending on the data type
            // in particular when the datatype is a concept type, then
            // it should be possible to select an instance of that concept using some
            // auto-completing dropdown box

//            form.add(new LambdaAjaxButton<>("save", ModifierEditor.this::actionSave));
//            form.add(new LambdaAjaxLink("cancel", t -> {
//                if (isNewModifier) {
//                    ModifierEditor.this.actionCancelNewStatement(t);
//                } else {
//                    ModifierEditor.this.actionCancelExistingStatement(t);
//                }
//            }));
//            form.add(new LambdaAjaxLink("delete", ModifierEditor.this::actionDelete)
//                .setVisibilityAllowed(!isNewStatement));
            add(form);
        }

        @Override
        public Component getFocusComponent() {
            return initialFocusComponent;
        }
    }


}
