package de.tudarmstadt.ukp.inception.ui.core.footer;

import org.apache.wicket.Component;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.footer.FooterItem;

@Order(100)
@org.springframework.stereotype.Component
public class TutorialFooterItem
    implements FooterItem
{
    @Override
    public Component create(String aId)
    {
        return new TutorialFooterPanel(aId);
    }
}
