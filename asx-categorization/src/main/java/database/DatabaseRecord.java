package database;

/**
 * Created by bartek on 2017-06-05.
 */
public class DatabaseRecord {

    private int id;
    private String lang;

    DatabaseRecord(int id) {
        this.id = id;
    }

    DatabaseRecord(int id, String lang) {
        this.id = id;
        this.lang = lang;
    }

    public int getId() {
        return id;
    }
    public String getLang() {
        return lang;
    }
}
