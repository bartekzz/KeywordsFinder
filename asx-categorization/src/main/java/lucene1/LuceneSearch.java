// indexdir needs to be set in Searcher dictated by database lang
package lucene1;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.List;

public class LuceneSearch {


    Searcher searcher;

    /*
    public static void main(String[] args) {
        LuceneSearch luceneSearch;
        luceneSearch = new LuceneSearch();
        try {
            luceneSearch.search();
            luceneSearch.getUrlsToAnalyze(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        finally {
            luceneSearch.close();
        }
    }
    */

    public LuceneSearch() throws IOException {
        searcher = new Searcher();
    }


    public List<String> getUrlsToAnalyze(String url, String lang) throws IOException, InterruptedException {
        List<String> topTenTerms = searcher.getUrlsToAnalyze(url, lang);
        close();

        return topTenTerms;
    }

    private void search() throws IOException, ParseException{
        long startTime = System.currentTimeMillis();

        long endTime = System.currentTimeMillis();

        System.out.println("Init time:" + (endTime - startTime) +" ms");
    }

    private void close() {
        try {
            searcher.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}