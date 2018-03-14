package hello;

import java.util.List;

public class Index {

    private String url;
    private List keywords;
    private boolean running;

    public Index() {

    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setKeywords(List keywords) {
        this.keywords = keywords;
    }

    public List getKeywords() {
        return keywords;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean getRunning() {
        return running;
    }
}