package de.tudarmstadt.ukp.clarin.webanno.tsv;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class SplitTSVFile
{

    public static void main(String[] args)
        throws IOException
    {
        File file = new File(args[0]);
        String[] tsvContent = FileUtils.readFileToString(file).split("\n\n");
        int i = 1;
        StringBuffer sb = new StringBuffer();
        for (String content : tsvContent) {
            sb.append(content + "\n\n");
            if (i % 50 == 0) {
                FileUtils.writeStringToFile(
                        new File(file.getAbsolutePath() + FilenameUtils.getBaseName(file.getName())
                                + i + ".tsv"), sb.toString());
                sb = new StringBuffer();
            }
            i++;
        }

    }

}
