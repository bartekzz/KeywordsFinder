package lucene1;

import common.CommonStrings;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import stanford_tagger.TagText;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by bartek on 2017-05-23.
 */
public class HelpFunctions {

    IndexReader indexReader;
    IndexReader indexReader_en;
    IndexReader indexReader_sv;
    IndexSearcher indexSearcher;
    IndexSearcher indexSearcher_en;
    IndexSearcher indexSearcher_sv;
    Directory indexDirectory_en;
    Directory indexDirectory_sv;
    QueryParser queryParser;

    public HelpFunctions() { }

    public HelpFunctions(boolean createAll) throws IOException {
        if(createAll) {
            setIndexDirectoryPath();
            createIndexReader();
            createIndexSearcher();
            createQueryParser();
        }
    }

    /*
    public void createIndexDirectory(String directoryName) {
        System.out.println("Testing 2");
        File classpathRoot = new File(this.getClass().getResource("").getPath());

        File file = new File(classpathRoot.getParentFile().getParentFile().getParentFile() + "/" + directoryName);
        System.out.println("Directory path: " + classpathRoot.getParent().toString() + "/" + directoryName);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
    }
    */
    static public String ExportResource(String optionalDir, String resourceName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        String jarFolder;
        try {
            stream = TagText.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            //jarFolder = new File(TagText.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            jarFolder = new String(CommonStrings.TMPDIR);
            resStreamOut = new FileOutputStream(jarFolder + "/" + optionalDir + resourceName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }
        System.out.println(jarFolder + "/" + optionalDir + resourceName);
        return jarFolder + "/" + optionalDir + resourceName;
    }

    public static void createIndexDirectoryCustom(String path, String directoryName) {
        File file = new File(path + "/" + directoryName);
        System.out.println("Directory path: " + path + "/" + directoryName);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
    }

    public void createIndexDirectoryTmp(String directoryName) {
        System.out.println("Testing tmp 13");
        File tmpDir = new File(CommonStrings.TMPDIR);

        File file = new File(tmpDir + "/" + directoryName);
        System.out.println("Directory path: " + tmpDir + "/" + directoryName);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
    }


    public void setIndexDirectoryPath() throws IOException {
        String indexDirectoryPath_en = CommonStrings.TMPDIR + "/index_en";
        String indexDirectoryPath_sv = CommonStrings.TMPDIR + "/index_sv";

        indexDirectory_en = FSDirectory.open(new File(indexDirectoryPath_en));
        indexDirectory_sv = FSDirectory.open(new File(indexDirectoryPath_sv));
    }

    public void createIndexReader() throws IOException {
        indexReader_en = DirectoryReader.open(indexDirectory_en);
        indexReader_sv = DirectoryReader.open(indexDirectory_sv);
    }

    public void createIndexSearcher() {
        indexSearcher_en = new IndexSearcher(indexReader_en);
        indexSearcher_en.setSimilarity(new BM25Similarity(1.2f, 0.75f));

        indexSearcher_sv = new IndexSearcher(indexReader_sv);
        indexSearcher_sv.setSimilarity(new BM25Similarity(1.2f, 0.75f));
    }

    public void createQueryParser() {
        queryParser = new QueryParser(Version.LUCENE_46, LuceneConstants.CONTENTS,
                new StandardAnalyzer(Version.LUCENE_46));
    }

    public IndexReader setIndexReader(String lang) {
        if(lang.equals("en")) {
            indexReader = indexReader_en;
            System.out.println("IndexReader => en");
            return indexReader;
        }
        else if (lang.equals("sv")) {
            indexReader = indexReader_sv;
            System.out.println("IndexReader => sv");
            return indexReader;
        }
        else {
            indexReader = indexReader_en;
            System.out.println("IndexReader => en (not found");
            return indexReader;
        }

    }

    public IndexSearcher setIndexSearcher(String lang) {
        if(lang.equals("en")) {
            indexSearcher = indexSearcher_en;
            System.out.println("IndexSearcher => en");
            return indexSearcher;
        }
        else if (lang.equals("sv")) {
            indexSearcher = indexSearcher_sv;
            System.out.println("IndexSearcher => sv");
            return indexSearcher;
        }
        else {
            indexSearcher = indexSearcher_en;
            System.out.println("IndexSearcher => en (not found)");
            return indexSearcher;
        }

    }

    public URL normalizeURL(String urlString) throws MalformedURLException {
        System.out.println("urlString: " + urlString);
        URL newUrl;
        if (!urlString.toLowerCase().matches("^\\w+://.*")) {
            urlString = "http://" + urlString;
            newUrl = new URL(urlString);
        }
        else {
            newUrl = new URL(urlString);
        }
        return newUrl;
    }

    public String getTrueLang(String lang) {

        if(lang.contains("en")) {
            lang = "en";
            System.out.println("True lang EN");
        }
        /*
        else if(lang.contains("sv")) {
            lang = "sv";
        }
        */
        else {
            lang = "n/a";
            System.out.println("True lang N/A");
        }

        System.out.println("Returning lang: " + lang);
        return lang;
    }

    public Analyzer getAnalyzer(String lang) {
        Analyzer analyzer;
        if(lang.equals("en")) {
            analyzer = new EnglishAnalyzer(Version.LUCENE_46);
            System.out.println("Analyzer: English");
        }
        else if(lang.equals("sv")) {
            analyzer = new SwedishAnalyzer(Version.LUCENE_46);
            System.out.println("Analyzer: Swedish");
        }
        else {
            analyzer = new EnglishAnalyzer(Version.LUCENE_46);
            System.out.println("Analyzer: English");
        }

        return analyzer;
    }

    // Search docs for url (return TopDocs)
    public TopDocs searchDoc(String fieldName, String searchQuery)
            throws IOException, ParseException{

        TermQuery query = new TermQuery(new Term(fieldName, searchQuery));
        System.out.println("Query: " + query);
        TopDocs topDocs = indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
        System.out.println("Topdoc with matching url: " + topDocs);
        System.out.println("Topdoc, totalhits: " + topDocs.totalHits);
        return topDocs;
    }

    public int getDocInt(String url) {
        TopDocs topDocs;
        int thisDoc = 0;
        try {
            topDocs = searchDoc("url", url);
            ScoreDoc scoreDoc[] = topDocs.scoreDocs;
            thisDoc = scoreDoc[0].doc;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return thisDoc;
    }


    public String getFieldContentsFromIndex(String url, String fieldName) {

        int thisMltDoc = getDocInt(url);
        // Print what is stored in index as content for the given document
        Document thisDoc = null;
        try {
            thisDoc = indexReader.document(thisMltDoc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String docString = thisDoc.get(fieldName);

        System.out.println("DocString: " + docString);

        return docString;
    }

}
