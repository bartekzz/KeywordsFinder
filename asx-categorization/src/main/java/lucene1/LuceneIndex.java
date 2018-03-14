package lucene1;

import common.SiteInfo;
import java.io.IOException;

public class LuceneIndex {

    Indexer indexer;
    private SiteInfo siteInfo;

    public LuceneIndex() {
        /*LuceneIndex luceneIndex;
        try {
            luceneIndex = new LuceneIndex();
            luceneIndex.createIndex(url);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }

    /*public static void main(String[] args) {
        LuceneIndex luceneIndex;
        try {
            luceneIndex = new LuceneIndex();
            luceneIndex.createIndex(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public SiteInfo getSiteInfo() {
        return siteInfo;
    }


    public void createIndex(String url) throws IOException{

        indexer = new Indexer();
        long startTime = System.currentTimeMillis();
        siteInfo = indexer.createIndex(url);
        System.out.println("LuceneIndex lang: " + siteInfo.getLang());
        long endTime = System.currentTimeMillis();
        indexer.close();
        System.out.println("File indexed, time taken: "
                +(endTime-startTime)+" ms");
    }

    public void close() throws IOException {
        indexer.close();
    }
}