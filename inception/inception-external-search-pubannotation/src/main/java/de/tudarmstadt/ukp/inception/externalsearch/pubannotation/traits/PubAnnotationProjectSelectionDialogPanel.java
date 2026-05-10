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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static java.util.Comparator.comparing;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;
import org.danekja.java.util.function.serializable.SerializableBiConsumer;

import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationProject;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class PubAnnotationProjectSelectionDialogPanel
    extends Panel
{
    private static final long serialVersionUID = -8623123519728988443L;

    private static final int ITEMS_PER_PAGE = 12;

    private final List<PubAnnotationProject> allProjects;
    private final SerializableBiConsumer<AjaxRequestTarget, PubAnnotationProject> onSelect;
    private String filter = "";

    public PubAnnotationProjectSelectionDialogPanel(String aId,
            List<PubAnnotationProject> aProjects,
            SerializableBiConsumer<AjaxRequestTarget, PubAnnotationProject> aOnSelect)
    {
        super(aId);
        allProjects = aProjects;
        onSelect = aOnSelect;

        var resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupId(true);
        queue(resultsContainer);

        var resultCount = new Label("resultCount", LambdaModel
                .of(() -> filteredProjects().size() + " of " + allProjects.size() + " projects"));
        resultCount.setOutputMarkupId(true);
        queue(resultCount);

        var filterField = new TextField<>("filter", PropertyModel.of(this, "filter"));
        filterField.setOutputMarkupId(true);
        filterField.add(new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, t -> {
            t.add(resultsContainer);
            t.add(resultCount);
        }));
        queue(filterField);

        var listing = new PageableListView<PubAnnotationProject>("projects",
                LambdaModel.of(this::filteredProjects), ITEMS_PER_PAGE)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<PubAnnotationProject> aItem)
            {
                var p = aItem.getModelObject();
                aItem.queue(
                        new LambdaAjaxLink("select", t -> actionSelect(t, aItem.getModelObject())));
                aItem.queue(new Label("name", p.getName()));
                aItem.queue(new Label("maintainer", emptyToDash(p.getMaintainer())));
                aItem.queue(new Label("author", emptyToDash(p.getAuthor())));
                aItem.queue(new Label("license", emptyToDash(p.getLicense())));
                aItem.queue(new Label("updated", emptyToDash(p.getUpdatedAt())));
            }
        };
        listing.setOutputMarkupId(true);
        resultsContainer.add(listing);

        resultsContainer.add(new PagingNavigator("navigator", listing));

        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));
    }

    private List<PubAnnotationProject> filteredProjects()
    {
        if (filter == null || filter.isBlank()) {
            return sortedByName(allProjects);
        }
        var needle = filter.toLowerCase(Locale.ROOT);
        return sortedByName(allProjects.stream() //
                .filter(p -> matches(p, needle)) //
                .toList());
    }

    private static boolean matches(PubAnnotationProject aProject, String aNeedle)
    {
        return contains(aProject.getName(), aNeedle) //
                || contains(aProject.getMaintainer(), aNeedle) //
                || contains(aProject.getAuthor(), aNeedle) //
                || contains(aProject.getLicense(), aNeedle);
    }

    private static boolean contains(String aHaystack, String aNeedle)
    {
        return aHaystack != null && aHaystack.toLowerCase(Locale.ROOT).contains(aNeedle);
    }

    private static List<PubAnnotationProject> sortedByName(List<PubAnnotationProject> aProjects)
    {
        return aProjects.stream()
                .sorted(comparing(
                        p -> p.getName() == null ? "" : p.getName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static String emptyToDash(String aValue)
    {
        return aValue == null || aValue.isBlank() ? "—" : aValue;
    }

    private void actionSelect(AjaxRequestTarget aTarget, PubAnnotationProject aProject)
    {
        onSelect.accept(aTarget, aProject);
        findParent(ModalDialog.class).close(aTarget);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    public String getFilter()
    {
        return filter;
    }

    public void setFilter(String aFilter)
    {
        filter = aFilter;
    }
}
