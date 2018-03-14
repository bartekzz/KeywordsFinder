package lucene1;

import common.SiteInfo;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.util.Version;
import org.w3c.dom.*;
import org.w3c.tidy.Tidy;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class JTidyHTMLHandler {

    Indexer indexer;

    public JTidyHTMLHandler(Indexer indexer) {
        this.indexer = indexer;
    }

    public SiteInfo getDocument(SiteInfo siteInfo, InputStream is, URL url, String rawUrl) throws IOException {

        // Init help fcuntion
        HelpFunctions helpFunctions = new HelpFunctions();

        // Tidy up
        Tidy tidy = new Tidy();

        Properties oProps = new Properties();
        oProps.setProperty("new-blocklevel-tags", "header hgroup article footer nav");
        tidy.setConfigurationFromProps(oProps);

        tidy.setMakeClean(true);
        tidy.setShowErrors(0);

        tidy.setInputEncoding("UTF-8");
        tidy.setOutputEncoding("UTF-8");

        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        org.w3c.dom.Document root = tidy.parseDOM(is, null);
        Element rawDoc = root.getDocumentElement();

        org.apache.lucene.document.Document doc =
                new org.apache.lucene.document.Document();

        // Get body text
        String body = getBody(rawDoc);
        //Charset.forName("UTF-8").encode(body);
        System.out.println("Rawdoc body: " + body);



        long startTime = System.currentTimeMillis();
        // Get lang
        String lang = getLang(rawDoc);

        //String trueLang;

        // If lang is in HTML-tag
        if(lang != "") {
            // Set analyzer in Indexer (i.e SwedishAnalyzer)
            String trueLang = helpFunctions.getTrueLang(lang);
            // If lang cannot be determined then return null
            if(trueLang.equals("n/a")) {
                System.out.println("Setting lang to N/A");
                return null;
            }
            //Analyzer analyzer = helpFunctions.getAnalyzer(trueLang);
            //indexer.setAnalyzer(analyzer);
            // set lang
            lang = trueLang;

        // If language is not in HTML, then detect language

            long endTime = System.currentTimeMillis();
            System.out.println(" Language from HTML, time taken: "
                    +(endTime-startTime)+" ms");
        } else {

            System.out.println("Warn! No language detected. Setting custom lang..");
            CustomLanguageDetector customLanguageDetector = new CustomLanguageDetector();
            String customLang = customLanguageDetector.returnCustomLanguage(body);
            System.out.println("Custom lang: " + customLang);
            // Get true lang (means only accepting preset languages)
            String trueLang = helpFunctions.getTrueLang(customLang);
            // If lang cannot be determined then return null
            if(trueLang.equals("n/a")) {
                System.out.println("Setting lang to N/A");
                return null;
            }
            //Analyzer analyzer = helpFunctions.getAnalyzer(customLang);
            //indexer.setAnalyzer(analyzer);
            // set lang
            lang = trueLang;

            long endTime = System.currentTimeMillis();
            System.out.println(" Custom detected language, time taken: "
                    +(endTime-startTime)+" ms");
        }

        // Set indexer
        indexer.fetchIndexWriter(lang);

        System.out.println("Grabbing stop words 7");

        // Add custom stop word list from file
        //BufferedReader br = new BufferedReader(new FileReader("/Users/bartek/Github/LuceneIndexing1/luceneindexing1/data1/stopwords_" + lang + ".txt"));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("stopwords_" + lang + ".txt")));
        List stopWords = new ArrayList<>();
        try {

            String line = br.readLine();

            while (line != null) {
                stopWords.add(line);
                line = br.readLine();
            }

        } finally {
            br.close();
        }

        // Filter tokenstream with stop word list

        final CharArraySet stopSet = new CharArraySet(Version.LUCENE_46, stopWords, true);

        StandardTokenizer stdToken = new StandardTokenizer(Version.LUCENE_46, new StringReader(body));
        TokenStream tokenStream;

        tokenStream = new StopFilter(Version.LUCENE_46, new ClassicFilter(new LowerCaseFilter(Version.LUCENE_46, stdToken)), stopSet);
        tokenStream.reset();

        StringBuilder sb = new StringBuilder();

        CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);
        String term;

        while (tokenStream.incrementToken()) {
            term = token.toString();
            //System.out.println("Filtered (with stop filter) tokens: " + term);
            sb.append(term);
            if (sb.length() > 0) {
                sb.append(" ");
            }
        }
        tokenStream.close();

        System.out.println("Token stream: " + sb.toString());

        // Execute indexing
        FieldType type1 = new FieldType();
        type1.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        type1.setIndexed(true);
        type1.setStored(true);
        type1.setStoreTermVectors(true);
        type1.setTokenized(true);
        type1.setStoreTermVectorOffsets(true);

        FieldType type2 = new FieldType();
        type2.setIndexOptions(IndexOptions.DOCS_ONLY);
        type2.setStored(true);
        //type2.setStoreTermVectors(true);
        //type2.setTokenized(true);
        //type2.setStoreTermVectorOffsets(true);

        if ((body != null) && (!body.equals(""))) {
            doc.add(new StringField("url", rawUrl, Field.Store.YES));
            doc.add(new StringField("lang", lang, Field.Store.YES));
            doc.add(new Field("contents", sb.toString(), type1));
        }

        // Set lang to SiteInfo object
        siteInfo.setLang(lang);
        // Set document to SiteInfo object
        siteInfo.setDocument(doc);

        return siteInfo;
    }

    protected String getLang(Element rawDoc) {
        if (rawDoc == null) {
            return null;
        }

        String lang = "";

        NodeList children = rawDoc.getElementsByTagName("html");
        if (children.getLength() > 0) {
            Element htmlElement = ((Element) children.item(0));
            NamedNodeMap namedNodeMap = htmlElement.getAttributes();
            if (namedNodeMap != null && namedNodeMap.getNamedItem("lang") != null) {
                    lang = namedNodeMap.getNamedItem("lang").getNodeValue();
                    System.out.println("Node (lang): " + lang);
            }
        }
        return lang;
    }

    protected String getTitle(Element rawDoc) {
        if (rawDoc == null) {
            return null;
        }

        String title = "";

        NodeList children = rawDoc.getElementsByTagName("title");
        if (children.getLength() > 0) {
            Element titleElement = ((Element) children.item(0));
            Text text = (Text) titleElement.getFirstChild();
            if (text != null) {
                title = text.getData();
            }
        }
        return title;
    }

    protected String getBody(Element rawDoc) {
        if (rawDoc == null) {
            System.out.println("Rawdoc is null!!");
            return null;
        }

        String body = "";
        NodeList children = rawDoc.getElementsByTagName("body");
        //System.out.println("Children length: " + children.getLength());
        //System.out.println("Has child nodes?: " + children.item(0).hasChildNodes());
        if (children.getLength() > 0) {
            body = getText(children.item(0));
        }
        return body;
    }

    protected String getText(Node node) {
        NodeList children = node.getChildNodes();
        //System.out.println("GetChildNodes length: " + children.getLength());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            //System.out.println("Node name: " + child.getNodeName() + ", Parent name: " + child.getParentNode().getNodeName() + ", Value: " + child.getNodeValue());
            // Remove content from script tags
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    sb.append(getText(child));
                    sb.append(" ");
                    break;

                case Node.TEXT_NODE:
                    if (!child.getParentNode().getNodeName().equals("script")) {
                        sb.append(((Text) child).getData());
                        //System.out.println("Element text: " + sb.toString());
                        break;
                    }
            }
        }
        return sb.toString();
    }
}