package clustering;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import database.DatabaseQueries;
import database.DatabaseRecord;
import lucene1.HelpFunctions;
import lucene1.LuceneConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.naivebayes.BayesUtils;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.TFIDF;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by bartek on 2017-06-14.
 */
public class Classifier {

    public static Map<String, Integer> readDictionnary(Configuration conf, Path dictionnaryPath) {
        Map<String, Integer> dictionnary = new HashMap<>();
        for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(dictionnaryPath, true, conf)) {
            dictionnary.put(pair.getFirst().toString(), pair.getSecond().get());
            //System.out.println("dictionnary: " + pair.getFirst().toString() + ", " + pair.getSecond().get());
        }
        return dictionnary;
    }

    public static Map<Integer, Long> readDocumentFrequency(Configuration conf, Path documentFrequencyPath) {
        Map<Integer, Long> documentFrequency = new HashMap<>();
        //int index = 0;
        for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(documentFrequencyPath, true, conf)) {
            documentFrequency.put(pair.getFirst().get(), pair.getSecond().get());
            /*
            if(index > 74720 && index < 74730) {
                System.out.println("documentFrequency: " + pair.getFirst().toString() + ", " + pair.getSecond().get());
            }
            index += 1;
            */
        }
        return documentFrequency;
    }

    public static void main(String[] args) throws Exception {

        HelpFunctions helpFunctions = new HelpFunctions(true);
        DatabaseQueries databaseQueries = new DatabaseQueries();

        String modelPath = "/tmp/mahout-work-bartek/model";
        String labelIndexPath = "/tmp/mahout-work-bartek/labelindex";
        String dictionaryPath = "/tmp/mahout-work-bartek/20news-vectors/dictionary.file-0";
        String documentFrequencyPath = "/tmp/mahout-work-bartek/20news-vectors/frequency.file-0";
        //String urlContents = "/tmp/mahout-work-bartek/74724";

        String url = "https://en.wikipedia.org/wiki/List_of_National_Hockey_League_players_born_in_the_United_Kingdom";
        Map<String, DatabaseRecord> dbRecord = databaseQueries.getLangForUrl(url, 1);
        String langForUrl = dbRecord.get(url).getLang();
        helpFunctions.setIndexReader(langForUrl);
        helpFunctions.setIndexSearcher(langForUrl);
        String urlContents = helpFunctions.getFieldContentsFromIndex(url, LuceneConstants.CONTENTS);


        Configuration configuration = new Configuration();

        // model is a matrix (wordId, labelId) => probability score
        NaiveBayesModel model = NaiveBayesModel.materialize(new Path(modelPath), configuration);

        StandardNaiveBayesClassifier classifier = new StandardNaiveBayesClassifier(model);

        // labels is a map label => classId
        Map<Integer, String> labels = BayesUtils.readLabelIndex(configuration, new Path(labelIndexPath));
        Map<String, Integer> dictionary = readDictionnary(configuration, new Path(dictionaryPath));
        Map<Integer, Long> documentFrequency = readDocumentFrequency(configuration, new Path(documentFrequencyPath));

        // analyzer used to extract word from tweet
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);

        int labelCount = labels.size();
        //int documentCount = documentFrequency.get(documentFrequency.size() - 1).intValue();
        int documentCount = documentFrequency.size();

        System.out.println("Number of labels: " + labelCount);
        System.out.println("Number of documents in training set: " + documentCount);

        /*
        BufferedReader reader = new BufferedReader(new FileReader(urlContents));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }

            stringBuilder.append(line);
            //stringBuilder.append(ls);

        }

        urlContents = stringBuilder.toString();

        System.out.println("Text: " + urlContents);
        */

        Multiset<String> words = ConcurrentHashMultiset.create();

        // extract words from tweet
        TokenStream ts = analyzer.tokenStream("text", new StringReader(urlContents));
        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        int wordCount = 0;
        while (ts.incrementToken()) {
            if (termAtt.length() > 0) {
                String word = ts.getAttribute(CharTermAttribute.class).toString();
                Integer wordId = dictionary.get(word);
                // if the word is not in the dictionary, skip it
                if (wordId != null) {
                    words.add(word);
                    wordCount++;
                }
            }
        }
        ts.end();
        ts.close();

        // create vector wordId => weight using tfidf
        Vector vector = new RandomAccessSparseVector(10000);
        TFIDF tfidf = new TFIDF();
        for (Multiset.Entry<String> entry : words.entrySet()) {
            String word = entry.getElement();
            int count = entry.getCount();
            Integer wordId = dictionary.get(word);
            Long freq = documentFrequency.get(wordId);
            double tfIdfValue = tfidf.calculate(count, freq.intValue(), wordCount, documentCount);
            vector.setQuick(wordId, tfIdfValue);
        }
        // With the classifier, we get one score for each label
        // The label with the highest score is the one the tweet is more likely to
        // be associated to
        Vector resultVector = classifier.classifyFull(vector);
        double bestScore = -Double.MAX_VALUE;
        int bestCategoryId = -1;
        for (Vector.Element element : resultVector.all()) {
            int categoryId = element.index();
            double score = element.get();
            if (score > bestScore) {
                bestScore = score;
                bestCategoryId = categoryId;
            }
            System.out.println("  " + labels.get(categoryId) + ": " + score);
        }
        System.out.println(" => " + labels.get(bestCategoryId));

        analyzer.close();
        //reader.close();
    }
}
