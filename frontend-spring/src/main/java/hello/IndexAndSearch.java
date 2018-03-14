package hello;

import common.SiteInfo;
import lucene1.LuceneIndex;
import lucene1.LuceneSearch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by bartek on 2017-07-03.
 */
public class IndexAndSearch implements Callable<List<String>> {

    private boolean processed;
    private Index index;
    private LuceneIndex luceneIndex;
    private List<String> list;

    public IndexAndSearch(Index index) {
        this.index = index;
    }

    @Override
    public List<String> call() throws IOException, InterruptedException {
        return process(index);
    }

    public List<String> process(Index index) throws IOException, InterruptedException {
        System.out.println("Index object: " + index);
        String url = index.getUrl();
        List<String> topTenTList;

        luceneIndex = new LuceneIndex();
        luceneIndex.createIndex(url);

        SiteInfo siteInfo = luceneIndex.getSiteInfo();
        String lang = siteInfo.getLang();
        Boolean indexed = siteInfo.getIndexed();

        if (indexed) {

            System.out.println("Created index!");
            LuceneSearch luceneSearch = new LuceneSearch();
            topTenTList = luceneSearch.getUrlsToAnalyze(url, lang);

        } else {
            System.out.println("Not indexed!");
            topTenTList = null;
        }

        return topTenTList;
    }

    public LuceneIndex getLuceneIndex() {
        return luceneIndex;
    }
}
