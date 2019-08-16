package de.tudarmstadt.ukp.inception.search.config;

import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("inception.search")
public class SearchPropertiesImpl implements SearchProperties
{
    long[] pagesSizes = {10, 20, 50, 100, 500, 1000};

    @Override
    public long[] getPageSizes()
    {
        return pagesSizes;
    }

    @Override
    public void setPageSizes(String[] aPageSizes)
    {
        for (int i = 0; i < aPageSizes.length; i++) {
            if (aPageSizes[i] == "all") {
                aPageSizes[i] = Long.toString(Long.MAX_VALUE);
            }
        }
        this.pagesSizes = Arrays.stream(aPageSizes).mapToLong(Long::parseLong).toArray();
    }
}
