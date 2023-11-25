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
package de.tudarmstadt.ukp.clarin.webanno.ui.tagsets;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenModelIsNotNull;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;

public class TagSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private OverviewListChoice<Tag> overviewList;
    private LambdaAjaxLink btnMoveUp;
    private LambdaAjaxLink btnMoveDown;
    private IModel<TagSet> selectedTagSet;

    public TagSelectionPanel(String id, IModel<TagSet> aTagset, IModel<Tag> aTag)
    {
        super(id, aTagset);

        setOutputMarkupPlaceholderTag(true);

        selectedTagSet = aTagset;

        overviewList = new OverviewListChoice<>("tag");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(aTag);
        overviewList.setChoices(LoadableDetachableModel.of(this::listTags));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        btnMoveUp = new LambdaAjaxLink("moveUp", this::moveTagUp);
        btnMoveUp.setOutputMarkupPlaceholderTag(true);
        btnMoveUp.add(visibleWhenModelIsNotNull(overviewList));
        add(btnMoveUp);

        btnMoveDown = new LambdaAjaxLink("moveDown", this::moveTagDown);
        btnMoveDown.setOutputMarkupPlaceholderTag(true);
        btnMoveDown.add(visibleWhenModelIsNotNull(overviewList));
        add(btnMoveDown);

        add(new LambdaAjaxLink("create", this::actionCreate));
    }

    private void moveTagUp(AjaxRequestTarget aTarget)
    {
        @SuppressWarnings("unchecked")
        List<Tag> tags = (List<Tag>) overviewList.getChoices();
        int i = tags.indexOf(overviewList.getModelObject());

        if (i < 1) {
            return;
        }

        Tag tag = tags.remove(i);
        tags.add(i - 1, tag);

        updateTagRanks(aTarget, tags);
    }

    private void moveTagDown(AjaxRequestTarget aTarget)
    {
        @SuppressWarnings("unchecked")
        List<Tag> tags = (List<Tag>) overviewList.getChoices();
        int i = tags.indexOf(overviewList.getModelObject());

        if (i >= tags.size() - 1) {
            return;
        }

        Tag tag = tags.remove(i);
        tags.add(i + 1, tag);

        updateTagRanks(aTarget, tags);
    }

    private void updateTagRanks(AjaxRequestTarget aTarget, List<Tag> tags)
    {
        annotationSchemaService.updateTagRanks(selectedTagSet.getObject(), tags);

        Tag selected = overviewList.getModelObject();
        for (Tag t : tags) {
            if (t.equals(selected)) {
                selected.setRank(t.getRank());
            }
        }

        aTarget.add(overviewList, btnMoveUp, btnMoveDown);
    }

    @Override
    protected void onChange(AjaxRequestTarget aTarget) throws Exception
    {
        aTarget.add(btnMoveUp, btnMoveDown);

        super.onChange(aTarget);
    }

    private List<Tag> listTags()
    {
        if (selectedTagSet.getObject() == null) {
            return Collections.emptyList();
        }

        return annotationSchemaService.listTags(selectedTagSet.getObject());
    }
}
