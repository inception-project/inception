package de.tudarmstadt.ukp.inception.externalsearch.log;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;

@Component public class ExternalSearchQueryEventAdapter
        implements EventLoggingAdapter<ExternalSearchQueryEvent>
{

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override public boolean accepts(Object aEvent)
    {
        return aEvent instanceof ExternalSearchQueryEvent;
    }

    @Override public long getProject(ExternalSearchQueryEvent aEvent)
    {
        return aEvent.getProject().getId();
    }

    @Override public String getAnnotator(ExternalSearchQueryEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override public String getUser(ExternalSearchQueryEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override public String getDetails(ExternalSearchQueryEvent aEvent)
    {
        try {
            Details details = new Details();

            details.query = aEvent.getQuery();

            return JSONUtil.toJsonString(details);
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
    }

    public static class Details
    {
        public String query;
    }
}
