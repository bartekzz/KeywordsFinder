package common;

import lucene1.Indexer;

import java.io.File;

public class CommonStrings {
    static String root = System.getProperty("user.dir");
    //static String root = "/Users/bartek/Github/Bidtheatre_webapp/asx-categorization";
    public static final String ROOTDIR = root;

    static String tmpdir = System.getProperty("java.io.tmpdir");
    public static final String TMPDIR = tmpdir;

    //static String classPath = new File(Indexer.class.getClass().getResource("").getPath()).toString();
    //public static final String CLASSPATH = classPath;
}