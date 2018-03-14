package lucene1;

import common.CommonStrings;
import database.DatabaseQueries;
import database.DatabaseRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import stanford_tagger.ParseTaggedText;
import stanford_tagger.TagText;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.lucene.search.highlight.TokenSources.getAnyTokenStream;

public class Searcher {

    IndexSearcher indexSearcher;
    IndexSearcher indexSearcher_en;
    IndexSearcher indexSearcher_sv;
    IndexReader indexReader;
    IndexReader indexReader_en;
    IndexReader indexReader_sv;
    QueryParser queryParser;
    Query query;
    //Query moreLikeThisQuery;
    Analyzer analyzer;
    TopDocs hits;
    Map<String, Long> hashTerms;
    int thisMltDoc;
    List<Object> filteredInterestingTerms;
    String filteredInterestingTermsString;
    DatabaseQueries databaseQueries;
    HelpFunctions helpFunctions;
    String lang;


    public Searcher() throws IOException{

        createHelpFunctions();
        createDatabseQueries();

        queryParser = helpFunctions.queryParser;

        filteredInterestingTerms = new ArrayList<>();
    }

    public void createHelpFunctions() throws IOException {
        // Create helpFunctions with IndexReader, IndexSearcher etc
        helpFunctions = new HelpFunctions(true);
    }

    public void createDatabseQueries() {
        databaseQueries = new DatabaseQueries();
    }

    public Analyzer setAnalyzer(String lang) {
        analyzer = new StandardAnalyzer(Version.LUCENE_46);
        /*
        if(lang.equals("en")) {
            analyzer = new EnglishAnalyzer(Version.LUCENE_46);
            System.out.println("Analyzer => en");
        }
        if(lang.equals("sv")) {
            analyzer = new SwedishAnalyzer(Version.LUCENE_46);
            System.out.println("Analyzer => sv");
        } else {
            analyzer = new StandardAnalyzer(Version.LUCENE_46);
            System.out.println("Analyzer => en");
        }
        */

        return analyzer;
    }

    public TopDocs search( String searchQuery)
            throws IOException, ParseException{
        query = queryParser.parse(searchQuery);
        return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    public Document getDocument(ScoreDoc scoreDoc)
            throws CorruptIndexException, IOException{
        return indexSearcher.doc(scoreDoc.doc);
    }

    public void close() throws IOException{
        indexReader.close();
    }

    public int setMaxList(List list, int max){
        // Set max amount of interesting terms
        int maxLength = 0;

        if (list.size() > max) {
            maxLength = max;
        } else {
            maxLength = list.size();
        }

        return maxLength;
    }

    public List<String> getUrlsToAnalyze(String url, String lang) throws IOException, InterruptedException {

        // Set lang
        this.lang = lang;

        return getMoreLikeThis(url);

        /*
        List<Map<String, DatabaseRecord>> urlList = databaseQueries.createListFromSelectedRecords(1, 0, 1);
        System.out.println("urlList: " + urlList);

        for(int i = 0; i < urlList.size(); i++) {

            // Index every url
            for(Map.Entry<String, DatabaseRecord> entry : urlList.get(i).entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                DatabaseRecord record = (DatabaseRecord)value;
                int id = record.getId();
                String lang = record.getLang();
                this.lang = lang;
                System.out.println("Setting lang from db: " + this.lang);

                try {
                    getMoreLikeThis((String)key);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                databaseQueries.updateKeywords(id, filteredInterestingTermsString);
                databaseQueries.updateFinished(id, 1, 1);
                databaseQueries.updateSetInProcess(id, 0);
            }
        }
        */
    }


    public List<String> getMoreLikeThis(String url) throws IOException, InterruptedException {

        // Set indexReader
        indexReader = helpFunctions.setIndexReader(lang);
        indexSearcher = helpFunctions.setIndexSearcher(lang);

        System.out.println("Number of docs in index: " + indexReader.numDocs());

        MoreLikeThis mlt = new MoreLikeThis(indexReader);

        mlt.setAnalyzer(setAnalyzer(lang));

        System.out.println("Total sum of term frequence: " + indexReader.getSumTotalTermFreq("contents"));
        long totalSumTermFreq = indexReader.getSumTotalTermFreq("contents");

        //lower some settings to MoreLikeThis will work with very short
        //quotations
        mlt.setMinTermFreq(1);
        mlt.setMinDocFreq(1);
        mlt.setMaxQueryTerms((int)(0.02 * totalSumTermFreq));
        mlt.setMinWordLen(2);

        System.out.println("Max query terms: " + (int)(0.02 * totalSumTermFreq));

        //Create the query that we can then use to search the index
        //moreLikeThisQuery = null;
        try {
            thisMltDoc = helpFunctions.getDocInt(url);

            // Print what is stored in index as content for the given document
            String docString = helpFunctions.getFieldContentsFromIndex(url, LuceneConstants.CONTENTS);

            System.out.println("ScoreDoc nr: " + thisMltDoc);
            //moreLikeThisQuery = mlt.like(thisMltDoc);

            String[] interestingTerms = mlt.retrieveInterestingTerms(thisMltDoc);

            //String[] filteredForStopWordsInterestingTerms = filterStopWords(interestingTerms, lang);

            // Map, sort and filter out Pos from Interesting Terms
            //filterOutAcceptedPos(filteredForStopWordsInterestingTerms, 100);

            // Map, sort and filter out Pos from Interesting Terms
            filterOutAcceptedPos(interestingTerms, 100);

            // Convert interesting terms array to string
            termsArrayToString(10);


        } catch (Exception e) {
            e.printStackTrace();
        }

        // Convert interesting terms array to string and return
        return termsArrayToString(10);
    }

    public String[] filterStopWords(String[] interestingTerms, String lang) throws IOException {

        //BufferedReader br = new BufferedReader(new FileReader(CommonStrings.ROOTDIR + "/stopwords/stopwords_" + lang + ".txt"));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("stopwords_" + lang + ".txt")));
        List stopWords = new ArrayList<>();
        List removedStopWordsInterestingTerms = new ArrayList<>();
        try {

            String line = br.readLine();

            while (line != null) {
                stopWords.add(line);
                line = br.readLine();
            }

        } finally {
            br.close();
        }

        // Add interesting terms from Array to List
        for(int i = 0; i < interestingTerms.length; i++) {
            removedStopWordsInterestingTerms.add(interestingTerms[i]);
        }

        System.out.println("All interesting terms (before removed stop words): " + removedStopWordsInterestingTerms);
        System.out.println("All interesting terms (before removed stop words) length: " + removedStopWordsInterestingTerms.size());

        int stopWordsCount = 0;
        // Why do i need to remove last term (-1) in loop from removedStopWordsInterestingTerms???
        for(int i = 0; i < removedStopWordsInterestingTerms.size() - 1; i++) {
            for (Object stopWord : stopWords) {
                if(removedStopWordsInterestingTerms.get(i).equals(stopWord)) {
                    removedStopWordsInterestingTerms.remove(i);
                    System.out.println("*** WARN!!! Removed stop words: " + stopWord);
                    stopWordsCount++;
                }
            }
        }
        System.out.println("Count removed words: " + stopWordsCount);

        // Initialize array
        String[] removedStopWordsInterestingTermsArray = new String[removedStopWordsInterestingTerms.size()];

        if(removedStopWordsInterestingTerms.size() != 0) {
            int size = removedStopWordsInterestingTerms.size();
            removedStopWordsInterestingTermsArray = new String[size];
            for(int i = 0; i < removedStopWordsInterestingTerms.size(); i++) {
                removedStopWordsInterestingTermsArray[i] = (String)removedStopWordsInterestingTerms.get(i);
            }
        }
        return removedStopWordsInterestingTermsArray;

    }

    public void filterOutAcceptedPos(String[] interestingTerms, int maxTerms) throws Exception {

        // Clear filteredInterestingTerms list
        //filteredInterestingTerms.clear();
        System.out.println("CLEARED! Filtered terms: " + filteredInterestingTerms);

        // Map and sort interesting terms
        List<String> interestingTermsList = sortInterestingTerms(interestingTerms);

        // Set max amount of interesting terms to filter (on POS)
        int maxInterestingTerms = setMaxList(interestingTermsList, maxTerms);

        filteredInterestingTerms.clear();
        //Filter out accepted POS
        for(int i = 0; i < maxInterestingTerms; i ++) {
            System.out.println("Interesting term to analyze: " + interestingTermsList.get(i));
            filterInterestingTerms(interestingTermsList.get(i));
        }

        System.out.println("Filtered terms: " + filteredInterestingTerms);
    }

    public List<String> sortInterestingTerms(String[] interestingTerms) throws Exception {

        Map<String, Long> interestingTermsMap = new HashMap<>();

        System.out.println("Interesting terms length: " + interestingTerms.length);
        // Get term's frequency and add term and frequency to map
        for(String interestingTerm : interestingTerms) {
            //long wTermScore = hashTerms.get(interestingTerm);
            long wTermScore = mTermFreq(interestingTerm);
            //System.out.println(interestingTerm + ", " + wTermScore);
            interestingTermsMap.put(interestingTerm, wTermScore);
        }
        System.out.println("");

        Map<String, Long> linkedMap = sortByValue(interestingTermsMap);

        System.out.println("--- // Print out sorted intersting terms // ---");
        List<String> interestingTermsList = new ArrayList<>();

        for (Map.Entry<String, Long> entry : linkedMap.entrySet()) {
            String key = entry.getKey().toString();
            Long value = entry.getValue();
            System.out.println(key + ", " + value);
            interestingTermsList.add(key);

        }

        return interestingTermsList;
    }

    public void filterInterestingTerms(String term) {

        // Get highlighted string
        String[] fragString = DoQuery2(new TermQuery(new Term(LuceneConstants.CONTENTS, term)));

        // Tag highlighted string with Pos
        TagText tagText = null;
        try {
            tagText = new TagText(lang);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String taggedText = tagText.tagStringUp(fragString[0]);

        // Parse tagged text (with Pos) and output accepted Pos
        ParseTaggedText parseTaggedText = new ParseTaggedText();
        List wordPosList = parseTaggedText.getPosFromSentence(taggedText, term);

        if(parseTaggedText.acceptedPos(wordPosList)) {
            System.out.println("Adding: " + wordPosList.get(0) + " ...");
            // Add word to list
            filteredInterestingTerms.add(wordPosList.get(0));

        }
    }

    public List<String> termsArrayToString(int maxTerms) throws InterruptedException {
        //Empty string
        ArrayList<String> filteredInterestingTermsList = new ArrayList<>();

        // Set max amount of filtered terms
        int maxFilteredTerms = setMaxList(filteredInterestingTerms, maxTerms);

        for (int i = 0; i < maxFilteredTerms; i++) {
            filteredInterestingTermsList.add((String)filteredInterestingTerms.get(i));
        }

        //System.out.println("Top " + maxFilteredTerms + " filtered terms: " + filteredInterestingTermsString);
        //TimeUnit.SECONDS.sleep(10);

        return filteredInterestingTermsList;
    }

    public long mTermFreq(String interestingTerm) throws IOException {
        Bits liveDocs = MultiFields.getLiveDocs(indexReader);
        TermsEnum termEnum = MultiFields.getTerms(indexReader, "contents").iterator(null);
        BytesRef bytesRef;
        DocsEnum docsEnum = null;
        int termFreq = 0;
        while ((bytesRef = termEnum.next()) != null) {
            if (termEnum.seekExact(bytesRef) && bytesRef.utf8ToString().equals(interestingTerm)) {

                docsEnum = termEnum.docs(liveDocs, null);

                if (docsEnum != null) {
                    int doc;
                    while ((doc = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS && doc == thisMltDoc) {
                        termFreq = docsEnum.freq();
                        System.out.println(bytesRef.utf8ToString() + " in doc " + doc + ": " + termFreq);
                    }
                }
            }
        }
        return Long.valueOf(termFreq);
    }

    public String[] DoQuery2(Query query) {
        //Query queryToSearch = new QueryParser("asddf", analyzer).parse("cocaine and ambition");
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        QueryScorer queryScorer = new QueryScorer(query);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer, 100);
        Highlighter highlighter = new Highlighter(htmlFormatter,
                queryScorer);
        highlighter.setTextFragmenter(fragmenter);

        return highLight(indexSearcher, indexReader, LuceneConstants.CONTENTS, highlighter);
    }

    @SuppressWarnings("deprecation")
    public String[] highLight(IndexSearcher indexSearcher, IndexReader indexReader, String field, Highlighter highlighter) {
        String[] fragString = new String[10];
        try {

            Document doc = indexSearcher.doc(thisMltDoc);
            String text = doc.get(field);

            TokenStream tokenStream = getAnyTokenStream(indexReader, thisMltDoc, field, analyzer);
            TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
            System.out.println("Frag length: " + frag.length);
            for (int j = 0; j < 1; j++) {
                if ((frag[j] != null)) {
                    System.out.println("score: " + frag[j].getScore() + ", frag: " + (frag[j].toString()));
                    System.out.println("FragString: " + frag[j].toString().replace("<B>", "").replace("</B>", ""));
                    // Check if frag's first letter is uppercase, if yes lowercase all letter and then capitalize first letter
                    String rawFragString = frag[j].toString().replace("<B>", "").replace("</B>", "");
                    System.out.println("RawString: " + rawFragString);

                    String[] fragWords = StringUtils.split(rawFragString, " ");

                    String fragSentence = "";
                    for(String fragWord : fragWords) {

                        if (Character.isUpperCase(fragWord.charAt(0))) {
                            fragWord = fragWord.toLowerCase();
                            fragWord = fragWord.substring(0, 1).toUpperCase() + fragWord.substring(1);
                        }

                        System.out.println("Fragword: " + fragWord);

                        fragSentence = fragSentence.concat(" " + fragWord);

                    }
                    System.out.println("Sentence: " + fragSentence);
                    // Remove html formatting from word
                    fragString[j] = fragSentence.trim();
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidTokenOffsetsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return fragString;
    }


    public void getHighFreqTerms() throws Exception {
        HighFreqTerms.DocFreqComparator cmp = new HighFreqTerms.DocFreqComparator();
        TermStats[] highFreqTerms = HighFreqTerms.getHighFreqTerms(indexReader, 10000, LuceneConstants.CONTENTS, cmp);

        List<Long> terms = new ArrayList<>(highFreqTerms.length);
        hashTerms = new HashMap<>(highFreqTerms.length);
        int i = 0;
        for (TermStats ts : highFreqTerms) {
            terms.add(ts.totalTermFreq);
            //System.out.println("Term, " + ts.termtext.utf8ToString() + " , has frequency of, " + ts.totalTermFreq);
            hashTerms.put(ts.termtext.utf8ToString(), ts.totalTermFreq);
        }

        Map<String, Long> linkedMap = sortByValue(hashTerms);

        for (Map.Entry<String, Long> entry : linkedMap.entrySet()) {
            String key = entry.getKey().toString();
            Long value = entry.getValue();
            System.out.println(key + ", " + value);
        }
    }


    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ) {
        List<Map.Entry<K, V>> list =
                new LinkedList<>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }


}