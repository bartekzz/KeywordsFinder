package stanford_tagger;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by bartek on 2017-04-20.
 */

public class ParseTaggedText {

    public static Map<String, String> mapPos(String sentence) {

        String[] tagged_words = StringUtils.split(sentence);

        Map<String, String> posMap = new HashMap<>();

        for (String tagged_word : tagged_words) {
            String[] identifiedPos = StringUtils.split(tagged_word, "_");
            System.out.println("Split string: " + identifiedPos[0]);
            if(identifiedPos.length != 1) {
                posMap.put(identifiedPos[0], identifiedPos[1]);

                System.out.println("Tagged Word: " + tagged_word + ", Word: " + identifiedPos[0] + ", Pos: " + identifiedPos[1]);
            } else {
                System.out.println("No tagging! Tagged Word: " + tagged_word);
            }

        }

        return posMap;
    }

    public static String containsWord(Map mp, String word) {
        String pos = null;
        word.toLowerCase();
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            String key = (String) pair.getKey();
            key.toLowerCase();
            String value = (String) pair.getValue();

            if(key.contains(word)) {
                pos = value;
                System.out.println("Returning pos, alt 1: " + pos);
                break;
            }
            if(word.contains(key)) {
                pos = value;
                System.out.println("Returning pos, alt 2: " + pos);
                break;
            } else {
                pos = null;
                System.out.println("Coulnt match pos. NULL! ");
            }
            it.remove(); // avoids a ConcurrentModificationException
        }

        return pos;
    }

    public static List<String> getPosFromSentence(String sentence, String word) {
        System.out.println("Looking up: " + word);
        Map<String, String> posMap = mapPos(sentence);

        String pos = posMap.get(word);

        // If word not found, capitalize first letter in word

        if(pos == null) {
            System.out.println("WARNING! Word not found. Capitalizing ...");
            pos = posMap.get(word.substring(0, 1).toUpperCase() + word.substring(1));
        }

        if(pos == null) {
            System.out.println("WARNING! Checking if word is contained. containsWord() ...");
            pos = containsWord(posMap, word);
        }

        List<String> wordPosList = new ArrayList<>();

        if(pos != null) {

            wordPosList.add(word);
            wordPosList.add(pos);

            System.out.println("WordPosList: " + wordPosList);
        }

        return wordPosList;
    }

    public static boolean acceptedPos(List<String> wordPosList) {
        if(!wordPosList.isEmpty()) {
            switch (wordPosList.get(1)) {
                case "NN":
                    System.out.println("NN is accepted");
                    return true;
                case "NNS":
                    System.out.println("NNS is accepted");
                    return true;
                case "NNP":
                    System.out.println("NNP is accepted");
                    return true;
                case "NNPS":
                    System.out.println("NNPS is accepted");
                    return true;
                default:
                    System.out.println("Invalid pos");
                    return false;
            }
        } else {
            System.out.println("WARNING! wordPosList empty. Couln't find match!");
            return false;
        }
    }

    public static void main(String[] args) {
        String str = "This_DT is_VBZ a_DT sample_NN text_NN";
        //String[] text = new String[] {str};

        List wordPosList = getPosFromSentence(str, "is");

        if(acceptedPos(wordPosList)) {
            System.out.println("Adding: " + wordPosList.get(0) + " ...");
        }

    }

}
