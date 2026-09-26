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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.addeditor;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;

import java.util.stream.Stream;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.split.SplitEditorWorkspace;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorSetChangedEvent;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Splits the page into a second editor pane, and - at a cap of exactly two - unsplits it again.
 */
public class AddEditorSidebarFooterItem
    extends Panel
{
    private static final long serialVersionUID = 4034681080858148748L;

    private static final String MID_TOGGLE_EDITOR = "toggleEditor";

    private static final String MID_ICON = "icon";

    private static final IconType ICON_SPLIT = FontAwesome7IconType.table_columns_s;
    private static final IconType ICON_UNSPLIT = FontAwesome7IconType.square_r;

    private final AnnotationPageBase2 page;

    public AddEditorSidebarFooterItem(String aId, AnnotationPageBase2 aPage)
    {
        super(aId);

        page = aPage;

        setOutputMarkupId(true);

        var toggleEditor = new LambdaAjaxLink(MID_TOGGLE_EDITOR, this::actionToggleEditor);
        toggleEditor.setOutputMarkupPlaceholderTag(true);
        toggleEditor.add(enabledWhen(() -> canAddEditor() || canRemoveEditor()));
        toggleEditor.add(new AttributeModifier("title",
                () -> getString(showsUnsplit() ? "unsplitEditor" : "splitEditor")));
        toggleEditor.add(new Icon(MID_ICON,
                LambdaModel.of(() -> showsUnsplit() ? ICON_UNSPLIT : ICON_SPLIT)));
        add(toggleEditor);
    }

    private boolean canAddEditor()
    {
        return workspace() != null && workspace().canAddEditor();
    }

    private boolean showsUnsplit()
    {
        return isToggle() && canRemoveEditor();
    }

    private boolean isToggle()
    {
        return workspace() != null && workspace().getMaxEditors() == 2;
    }

    private boolean canRemoveEditor()
    {
        return removableEditor() != null;
    }

    private DocumentEditorPanel removableEditor()
    {
        var workspace = workspace();
        if (workspace == null) {
            return null;
        }

        if (!isToggle()) {
            // Editors cannot be closed by this button if max editors != 2
            return null;
        }

        var panels = workspace.getEditorPanels();
        if (panels.size() != 2) {
            // Editors cannot be closed via the button if there are not exactly 2
            return null;
        }

        // Prefer closing the non-active editor. Written for arbitrary cap... just in case
        var active = workspace.getActiveEditor().orElse(null);
        var preferred = panels.stream().filter(panel -> panel != active).toList();
        var fallback = panels.stream().filter(panel -> panel == active).toList();
        return Stream.concat(preferred.stream(), fallback.stream()) //
                .filter(workspace::canCloseEditor) //
                .findFirst() //
                .orElse(null);
    }

    private SplitEditorWorkspace workspace()
    {
        return page.getWorkspace() instanceof SplitEditorWorkspace workspace ? workspace : null;
    }

    private void actionToggleEditor(AjaxRequestTarget aTarget)
    {
        var workspace = workspace();
        if (workspace == null) {
            return;
        }

        if (showsUnsplit()) {
            workspace.closeEditor(aTarget, removableEditor());
            return;
        }

        if (workspace.canAddEditor()) {
            workspace.addEmptyEditor(aTarget);
        }
    }

    @Override
    public void onEvent(IEvent<?> aEvent)
    {
        super.onEvent(aEvent);

        if (aEvent.getPayload() instanceof EditorSetChangedEvent event
                && event.getRequestHandler() != null) {
            event.getRequestHandler().add(this);
        }
    }
}
