package evaluation;


import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils
{

    public static Set<String> readFile(String location) {
        try {
            File f = new File(location);
            List<String> lines = FileUtils.readLines(f, "UTF-8");
            return new HashSet<>(lines);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
