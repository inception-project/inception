package de.tudarmstadt.ukp.inception.ui.kb;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;

public class AnnotatedListIdentifiers
    extends Panel
{
    private static final long serialVersionUID = -2431507947235476294L;
    private static final Logger LOG = LoggerFactory.getLogger(AnnotatedListIdentifiers.class);

    // TO replace with KB.identifier after subClass and instances change
    private String searchQuery = "<KB.Entity=\"XYZ\"/>";
    private @SpringBean DocumentService documentService;
    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBHandle> conceptModel;

    private Project currentProject;
    private User currentUser;

    private Model<String> targetQuery = Model.of("");

    public AnnotatedListIdentifiers(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> aConcept, IModel<KBHandle> aInstance)
    {
        super(aId, aConcept);
        setOutputMarkupId(true);
        kbModel = aKbModel;
        conceptModel = aConcept;
        currentUser = userRepository.getCurrentUser();
        System.out.println(aInstance.getObject());
        if (aInstance.getObject() == null) {
            String concept = aConcept.getObject().getUiLabel();
            targetQuery = Model.of(searchQuery.replace("XYZ", concept));
        }
        else {
            String instance = aInstance.getObject().getUiLabel();
            targetQuery = Model.of(searchQuery.replace("XYZ", instance));
        }
        LambdaModel<List<SearchResult>> searchResults = LambdaModel.of(this::getSearchResults);
        LOG.debug("SearchResult count : " + searchResults.getObject().size());
        OverviewListChoice<String> overviewList = new OverviewListChoice<String>(
                "annotatedResultGroups")
        {
            private static final long serialVersionUID = -122960232588575731L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(!searchResults.getObject().isEmpty());
            }
        };

        overviewList.setChoices(getSearchResultsFormatted(searchResults));
        add(overviewList);

        add(new Label("count", LambdaModel.of(() -> overviewList.getChoices().size())));

    }

    public List<String> getSearchResultsFormatted(LambdaModel<List<SearchResult>> searchResults)
    {
        List<String> searchResultList = new ArrayList<String>();
        for (SearchResult x : searchResults.getObject()) {
            String sentence = new String();
            sentence = sentence + x.getText();
            searchResultList.add(sentence);
            LOG.debug("Sentence search : " + sentence);
        }
        return searchResultList;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setVisible(conceptModel.getObject() != null
                && isNotEmpty(conceptModel.getObject().getIdentifier()));
    }

    private List<SearchResult> getSearchResults()
    {
        if (isBlank(targetQuery.getObject())) {
            return Collections.emptyList();
        }

        try {
            currentProject = kbModel.getObject().getProject();
            return searchService.query(currentUser, currentProject, targetQuery.getObject());
        }
        catch (Exception e) {
            error("Error in the query: " + e.getMessage());
            return Collections.emptyList();
        }
    }

}
