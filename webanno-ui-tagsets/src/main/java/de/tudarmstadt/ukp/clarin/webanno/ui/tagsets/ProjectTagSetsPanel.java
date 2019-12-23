/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.tagsets;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel user to manage Tagsets.
 */
public class ProjectTagSetsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 7004037105647505760L;

    private TagSetSelectionPanel tagSetSelectionPanel;
    private TagSelectionPanel tagSelectionPanel;
    private TagSetEditorPanel tagSetEditorPanel;
    private TagEditorPanel tagEditorPanel;
    private TagSetImportPanel tagSetImportPanel;

    private IModel<Project> selectedProject;
    private IModel<TagSet> selectedTagSet;
    private IModel<Tag> selectedTag;
    
    public ProjectTagSetsPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        selectedProject = aProjectModel;
        selectedTagSet = Model.of();
        selectedTag = Model.of();
        
        tagSetImportPanel = new TagSetImportPanel("tagSetImport", selectedProject, selectedTagSet);
        tagSetImportPanel.setImportCompleteAction(target -> {
            selectedTag.setObject(null);
            target.add(tagSetSelectionPanel);
            target.add(tagSetEditorPanel);
            target.add(tagSelectionPanel);
            target.add(tagEditorPanel);
        });
        add(tagSetImportPanel);
        
        tagSetSelectionPanel = new TagSetSelectionPanel("tagSetSelector", selectedProject,
                selectedTagSet);
        tagSetSelectionPanel
                .onConfigure(_this -> _this.setVisible(selectedProject.getObject() != null));
        tagSetSelectionPanel.setCreateAction(target -> selectedTagSet.setObject(new TagSet()));
        tagSetSelectionPanel.setChangeAction(target -> { 
            selectedTag.setObject(null);
            target.add(tagSetEditorPanel);
            target.add(tagSelectionPanel);
            target.add(tagEditorPanel);
        });
        add(tagSetSelectionPanel);
        
        tagSelectionPanel = new TagSelectionPanel("tagSelector", selectedTagSet, selectedTag);
        tagSelectionPanel.onConfigure(_this -> _this.setVisible(
                selectedTagSet.getObject() != null && selectedTagSet.getObject().getId() != null));
        tagSelectionPanel.setCreateAction(target -> selectedTag.setObject(new Tag()));
        tagSelectionPanel.setChangeAction(target -> { 
            target.add(tagEditorPanel);
        });
        add(tagSelectionPanel);
        
        tagEditorPanel = new TagEditorPanel("tagEditor", selectedTagSet, selectedTag);
        tagEditorPanel.onConfigure(_this -> _this.setVisible(selectedTag.getObject() != null));
        add(tagEditorPanel);

        tagSetEditorPanel = new TagSetEditorPanel("tagSetEditor", selectedProject, selectedTagSet,
                selectedTag);
        tagSetEditorPanel
                .onConfigure(_this -> _this.setVisible(selectedTagSet.getObject() != null));
        add(tagSetEditorPanel);
    }
    
    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        selectedTagSet.setObject(null);
        selectedTag.setObject(null);
    }
}
