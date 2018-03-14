package lucene1;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by bartek on 2017-05-24.
 */
public class CustomLanguageDetector {

    TextObjectFactory textObjectFactory;
    LanguageDetector languageDetector;

    public CustomLanguageDetector() throws IOException {

        //load all languages:
        List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();

        //build language detector:
        languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();

        //create a text object factory
        textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();


    }

    public String returnCustomLanguage(String text) {
        //query:
        TextObject textObject = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);

        if (lang.isPresent()) {
            System.out.println("Language: " + lang.get().toString());
            return lang.get().toString();
        } else {
            return "n/a";
        }


    }

    public static void main(String[] args) throws IOException {
        CustomLanguageDetector customLanguageDetector = new CustomLanguageDetector();
        //customLanguageDetector.returnCustomLanguage("This is English");
    }
}
