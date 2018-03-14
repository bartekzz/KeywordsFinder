package hello;

import common.SiteInfo;
import lucene1.LuceneIndex;
import lucene1.LuceneSearch;

import java.io.IOException;
import java.util.List;

class RunnableDemo implements Runnable {
    private Thread t;
    private String threadName;
    private Index index;

    RunnableDemo( String name, Index index) {
        threadName = name;
        System.out.println("Creating " +  threadName );

        this.index = index;

        start();
    }

    public void interrupt() {
        t.interrupt();
        System.out.println("Thread interrupted?...: " + t.isInterrupted());
    }

    public void run() {
        System.out.println("Running " +  threadName );
        try {
            System.out.println("Thread: " + threadName);

            System.out.println("Index object: " + index);
            String url = index.getUrl();
            List<String> topTenTList;

            LuceneIndex luceneIndex = new LuceneIndex();
            luceneIndex.createIndex(url);

            SiteInfo siteInfo = luceneIndex.getSiteInfo();
            String lang = siteInfo.getLang();
            boolean indexed = siteInfo.getIndexed();

            if (indexed) {

                LuceneSearch luceneSearch = new LuceneSearch();
                topTenTList = luceneSearch.getUrlsToAnalyze(url, lang);
                index.setKeywords(topTenTList);
                System.out.println("Keywords: " + index.getKeywords());

            } else {
                System.out.println("Not indexed!");
            }

        }catch (InterruptedException e) {
            System.out.println("Thread " +  threadName + " interrupted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread " +  threadName + " exiting.");
    }

    public void start () {
        System.out.println("Starting " +  threadName );
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}