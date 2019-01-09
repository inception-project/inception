package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchServiceImpl;
import org.apache.wicket.MetaDataKey;

public final class ExternalSearchUserStateMetaData
{
    public static final MetaDataKey<ExternalSearchAnnotationSidebar.ExternalSearchUserState>
        CURRENT_ES_USER_STATE = new MetaDataKey<ExternalSearchAnnotationSidebar.ExternalSearchUserState>()
    {
        private static final long serialVersionUID = 1L;
    };
}
