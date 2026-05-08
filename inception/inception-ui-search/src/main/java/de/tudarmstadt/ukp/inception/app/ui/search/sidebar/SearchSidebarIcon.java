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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker.MATCH;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker.MATCH_FOCUS;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.SearchOptions;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.SearchQueryRequest;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;

public class SearchSidebarIcon
    extends Panel
{
    private static final long serialVersionUID = -1870047500327624860L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userService;
    private @SpringBean PreferencesService preferencesService;

    public SearchSidebarIcon(String aId, IModel<AnnotatorState> aState)
    {
        super(aId, aState);

        setOutputMarkupId(true);

        queue(new Icon("icon", FontAwesome5IconType.search_s));
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        var searchOptions = SearchOptionsMetaDataKey.get(getPage());

        var query = searchOptions.map(SearchOptions::getQuery);
        var selectedResult = searchOptions.map(SearchOptions::getSelectedResult).getObject();
        if (query.map(StringUtils::isNotBlank).orElse(false).getObject()) {
            try {
                var results = query(query.getObject());
                for (var result : results) {
                    if (result.equals(selectedResult)) {
                        // We render the selected result separately. Rendering it does not
                        // require a query. So if the query fails, we can still highlight
                        // the selected result.
                        continue;
                    }

                    if (overlapping(aEvent.getVDocument().getWindowBegin(),
                            aEvent.getVDocument().getWindowEnd(), result.getOffsetStart(),
                            result.getOffsetEnd())) {
                        var range = VRange.clippedRange(aEvent.getVDocument(),
                                result.getOffsetStart(), result.getOffsetEnd());

                        range.ifPresent(r -> aEvent.getVDocument().add(new VTextMarker(MATCH, r)));
                    }
                }
            }
            catch (IOException | ExecutionException e) {
                LOG.error("Cannot render match highlights", e);
            }
        }

        if (selectedResult != null) {
            var range = VRange.clippedRange(aEvent.getVDocument(), selectedResult.getOffsetStart(),
                    selectedResult.getOffsetEnd());

            range.ifPresent(r -> aEvent.getVDocument().add(new VTextMarker(MATCH_FOCUS, r)));
        }
    }

    private List<SearchResult> query(String aQuery) throws ExecutionException, IOException
    {
        var state = getModelObject();

        var groupedResults = searchService.query(SearchQueryRequest.builder() //
                .withProject(state.getProject()) //
                .withUser(state.getUser()) //
                .withQuery(aQuery) //
                .withLimitedToDocument(state.getDocument()) //
                // We don't want to wait for the latest index changes to be committed because
                // that may take too long for huge documents - yes, that means that we might
                // miss a highlight until the next rendering is done
                .withCommitRequired(false) //
                .build());

        return groupedResults.values().stream() //
                .flatMap(resultsGroup -> resultsGroup.stream()) //
                .distinct() //
                .toList();
    }
}
