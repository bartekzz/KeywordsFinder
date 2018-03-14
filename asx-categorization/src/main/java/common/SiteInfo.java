package common;

import org.apache.lucene.document.Document;

/**
 * Created by bartek on 2017-06-27.
 */
public class SiteInfo {

    private String lang;
    private boolean indexed;
    private Document document;

    public SiteInfo() {

    }

    public SiteInfo(String lang, boolean indexed, Document document) {
        this.lang = lang;
        this.indexed = indexed;
        this.document = document;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public boolean getIndexed() {
        return indexed;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

}
