package de.tudarmstadt.ukp.inception.search.config;

import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("inception.search")
public class SearchPropertiesImpl implements SearchProperties
{
    int[] pagesSizes = {10, 20, 50, 100, 500, 1000};

    @Override
    public int[] getPagesSizes()
    {
        return pagesSizes;
    }

    @Override
    public void setPagesSizes(String[] aPagesSizes)
    {
        this.pagesSizes = Arrays.stream(aPagesSizes).mapToInt(Integer::parseInt).toArray();
    }
}
