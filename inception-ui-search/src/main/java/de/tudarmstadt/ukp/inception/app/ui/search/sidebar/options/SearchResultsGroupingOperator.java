package de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options;

import java.util.function.Function;

import de.tudarmstadt.ukp.inception.search.SearchResult;

public enum SearchResultsGroupingOperator
{
    DOCUMENTTITLE("document title", SearchResult::getDocumentTitle);
    /*FEATUREVALUE("feature value", sr-> {Type type = CasUtil.getAnnotationType(jCas.getCas(), aAdapter.getAnnotationTypeName());
        AnnotationFS annoFS = selectSingleFsAt(jCas, type, searchResult.getOffsetStart(),
            searchResult.getOffsetEnd());});*/

    private final String name;
    private final Function<SearchResult, String> function;

    SearchResultsGroupingOperator(String aName, Function<SearchResult, String> aFunction) {
        name = aName;
        function = aFunction;
    }

    public String getName()
    {
        return name;
    }

    public Function<SearchResult, String> getFunction()
    {
        return function;
    }
}
