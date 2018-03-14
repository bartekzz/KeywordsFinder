
// redirect 301/302

package lucene1;

import common.CommonStrings;
import common.SiteInfo;
import database.DatabaseQueries;
import database.DatabaseRecord;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Indexer {

    IndexWriter writer;
    IndexWriter writer_en;
    IndexWriter writer_sv;
    Path path;
    String indexDirectoryString_en;
    String indexDirectoryString_sv;
    Directory indexDirectory_en;
    Directory indexDirectory_sv;
    HelpFunctions helpFunctions;
    DatabaseQueries databaseQueries;
    IndexWriterConfig conf_en;
    IndexWriterConfig conf_sv;
    Analyzer analyzer;
    String lang;


    public Indexer() throws IOException {
        //this directory will contain the indexes
        /*
        Directory indexDirectory =
                FSDirectory.open(new File(indexDirectoryPath));
        */

        databaseQueries = new DatabaseQueries();
        helpFunctions = new HelpFunctions();

        System.out.println("Creating index files!");

        //indexDirectoryString_en = CommonStrings.ROOTDIR + "/index_en";
        //indexDirectoryString_sv = CommonStrings.ROOTDIR + "/index_sv";

        // Create indexDirectories
        helpFunctions.createIndexDirectoryTmp("index_sv");
        helpFunctions.createIndexDirectoryTmp("index_en");

        indexDirectoryString_en = CommonStrings.TMPDIR + "/index_en";
        indexDirectoryString_sv = CommonStrings.TMPDIR + "/index_sv";

        //path = FileSystems.getDefault().getPath(indexDirectoryString);

        indexDirectory_en = FSDirectory.open(new File(indexDirectoryString_en));
        indexDirectory_sv = FSDirectory.open(new File(indexDirectoryString_sv));

        unlockIndex();

        //create the IndexWriter

        analyzer = new StandardAnalyzer(Version.LUCENE_46);

        conf_en = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        conf_en.setSimilarity(new BM25Similarity(1.2f, 0.75f));

        conf_sv = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        conf_sv.setSimilarity(new BM25Similarity(1.2f, 0.75f));

        writer_en = new IndexWriter(indexDirectory_en, conf_en);
        writer_sv = new IndexWriter(indexDirectory_sv, conf_sv);

    }

    public void fetchIndexWriter(String lang) throws IOException {
        if(lang.equals("en")) {
            writer = writer_en;
        }
        else if (lang.equals("sv")) {
            writer = writer_sv;
        }
        else {
            writer = writer_en;
        }

        // Set lang as instance variable
        this.lang = lang;
    }

    public void setAnalyzer(Analyzer analyzer) throws IOException {

        this.analyzer = analyzer;

    }

    public void close() throws CorruptIndexException, IOException{
        writer_en.close();
        writer_sv.close();
    }

    private SiteInfo indexHTML(String rawUrl, URL url) throws IOException{
        boolean indexed = false;

        System.out.println("Indexing HTML, " + url.toString());
        JTidyHTMLHandler jTidy = new JTidyHTMLHandler(this);

        InputStream stream = null;
        // Redirect if needed
        boolean redirect = false;
        String redirectUrl = null;

        // New SiteInfo object
        SiteInfo siteInfo = new SiteInfo();

        // Establish connection for url and set user agent
        try {
            System.out.println("Trying to connect");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
            connection.addRequestProperty("User-Agent", userAgent);
            System.out.println("Added user agent");
            String cookies = "userId=qwerty";
            //String cookies = connection.getHeaderField("Set-Cookie");
            connection.setRequestProperty("Cookie", cookies);
            System.out.println("Set cookies");
            connection.setConnectTimeout(5000);
            System.out.println("Set connection timeout");
            connection.setReadTimeout(5000);
            System.out.println("Set read timeout");
            //System.out.println("Headers: " + connection.getHeaderFields().toString());
            connection.connect();
            System.out.println("Connected");

            // normally, 3xx is redirect
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                    System.out.println("Redirect 30x ");
                    redirect = true;
            }

            System.out.println("Response Code ... " + status);

            // Redirect if needed
            if (redirect) {

                // get redirect url from "location" header field
                redirectUrl = connection.getHeaderField("Location");

                // open the new connnection again
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                connection.setRequestProperty("Cookie", cookies);
                connection.addRequestProperty("User-Agent", userAgent);

                System.out.println("Redirect to URL : " + redirectUrl);

            }

            stream = connection.getInputStream();
            siteInfo = jTidy.getDocument(siteInfo, stream, url, rawUrl);

        // Catch faulty response codes (i.e 403 etc) and UnknowHostException (url no accessible) or connection timeout/ read timeout (SocketTimeoutException)
        } catch (IOException e) {
            System.out.println(e.getMessage());
            // indexed = false as default
            System.out.println("Not valid url!");
            siteInfo.setIndexed(indexed);

            return siteInfo;
        }

        //siteInfo = jTidy.getDocument(siteInfo, stream, url, rawUrl);

        // If document is not null, then index
        if(siteInfo.getDocument() != null) {
            writer.addDocument(siteInfo.getDocument(), analyzer);

            /*
            // If redirected and indexed, save redirect url to database
            if (redirect) {
                databaseQueries.updateRedirectUrl(id, redirectUrl);
            }
            */

            indexed = true;
            // Set indexed to SiteInfo object
            siteInfo.setIndexed(indexed);

            return siteInfo;
        // If document is null, dont index (case1: lang cannot be determined, case2: cannot scrape text in body of url)
        } else {
            // Set indexed to SiteInfo object
            siteInfo.setIndexed(indexed);

            return siteInfo;
        }
    }

    public void unlockIndex() throws IOException {
        File en = new File(indexDirectoryString_en);
        if(en.exists() && !en.isDirectory()) {
            indexDirectory_en.deleteFile(IndexWriter.WRITE_LOCK_NAME);
            //log.warn("Existing write.lock at [" + folder.getAbsolutePath() + "] has been found and removed. This is a likely result of non-gracefully terminated server. Check for index discrepancies!");
            System.out.println("Deleted english lock file");
        }
        File sv = new File(indexDirectoryString_sv);
        if(sv.exists() && !sv.isDirectory()) {
            indexDirectory_sv.deleteFile(IndexWriter.WRITE_LOCK_NAME);
            //log.warn("Existing write.lock at [" + folder.getAbsolutePath() + "] has been found and removed. This is a likely result of non-gracefully terminated server. Check for index discrepancies!");
            System.out.println("Deleted english lock file");
        }
        //indexDirectory.close();
    }

    public SiteInfo createIndex(String url)
            throws IOException {

        HelpFunctions helpFunctions = new HelpFunctions();
        URL normalizedUrl = helpFunctions.normalizeURL(url);

        SiteInfo siteInfo = indexHTML(url, normalizedUrl);
        System.out.println("SiteInfo: " + siteInfo.getDocument() + ", " + siteInfo.getLang() + ", " + siteInfo.getIndexed());

        return siteInfo;

        /*

        // Index with input from database

        List<Map<String, DatabaseRecord>> urlList = databaseQueries.createListFromSelectedRecords(0, 0, 1);
        System.out.println("urlList: " + urlList);


        for(int i = 0; i < urlList.size(); i++) {

            // Index every url
            for(Map.Entry<String, DatabaseRecord> entry : urlList.get(i).entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                DatabaseRecord record = (DatabaseRecord) value;
                int id = record.getId();

                HelpFunctions helpFunctions = new HelpFunctions();
                URL normalizedUrl = helpFunctions.normalizeURL((String)key);

                boolean indexed = indexHTML((String)key, normalizedUrl, id);

                if(indexed) {
                    databaseQueries.updateLang(id, lang);
                    databaseQueries.updateFinished(id, 1, 0);
                } else {
                    databaseQueries.updateError(id, 1);
                }

                databaseQueries.updateSetInProcess(id, 0);
            }
        }

        return writer.numDocs();
        */
    }
}