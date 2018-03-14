package stanford_tagger;

import common.CommonStrings;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;
import edu.stanford.nlp.util.PropertiesUtils;
import lucene1.HelpFunctions;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TagText {

    MaxentTagger tagger;

    public TagText(String lang) throws IOException,
            ClassNotFoundException {
        // Initialize the tagger
        if(lang.equals("sv")) {

            // Properties and TaggerConfig (loads model) feels redudant (?)
            /*
            Properties properties = new Properties();
            properties.setProperty("model", "tmp/models/swedish.bartek.tagger");
            TaggerConfig taggerConfig = new TaggerConfig(properties);
            //tagger = new MaxentTagger(CommonStrings.ROOTDIR + "/stanford-postagger-full-2016-10-31/models/swedish.bartek.tagger", taggerConfig);
            */

            //HelpFunctions.createIndexDirectoryCustom("/var/lib/tomcat8/webapps/asx/WEB-INF/lib", "models");
            HelpFunctions.createIndexDirectoryCustom("/tmp", "models");

            /*
            try {
                String model_file_path = HelpFunctions.ExportResource("models/", "swedish.bartek.tagger");
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

            //tagger = new MaxentTagger(model_file_path);
            tagger = new MaxentTagger("/tmp/models/swedish.tagger");
            System.out.println("Tagger: sv");
        } else {
            /*
            Properties properties = new Properties();
            properties.setProperty("model", CommonStrings.ROOTDIR + "/stanford-postagger-full-2016-10-31/models/wsj-0-18-left3words-distsim.tagger");
            TaggerConfig taggerConfig = new TaggerConfig(properties);
            */
            //tagger = new MaxentTagger(CommonStrings.ROOTDIR + "/stanford-postagger-full-2016-10-31/models/wsj-0-18-left3words-distsim.tagger", taggerConfig);

            //tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
            tagger = new MaxentTagger("/tmp/models/english-left3words-distsim.tagger");
            System.out.println("Tagger: en");
        }

    }

    public String tagStringUp(String text) {
        // The tagged string
        String tagged = tagger.tagString(text);
        System.out.println("Tagged: " + tagged);

        return tagged;
    }

    public static void main(String[] args)  {

    }
}
