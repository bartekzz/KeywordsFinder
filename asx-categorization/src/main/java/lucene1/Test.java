package lucene1;

import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import stanford_tagger.TagText;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bartek on 2017-09-18.
 */
public class Test {

    static public String ExportResource(String resourceName) throws Exception {
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
            jarFolder = new File(TagText.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            resStreamOut = new FileOutputStream(jarFolder + "/" + resourceName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }
        System.out.println(jarFolder + "/" + resourceName);
        return jarFolder + resourceName;
    }

    public Test() throws IOException, URISyntaxException {
        try {
            ExportResource("swedish.bartek.tagger");
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("stopwords_sv.txt")));

        List stopWords = new ArrayList<>();
        try

        {

            String line = br.readLine();

            while (line != null) {
                stopWords.add(line);
                line = br.readLine();
            }

        }

        finally

        {
            System.out.println(stopWords);
            br.close();
        }
        */

        /*
        // Read stream and build with StringBuilder
        BufferedReader rb = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("swedish.bartek.tagger"), StandardCharsets.UTF_8));
        StringBuilder model_file = new StringBuilder();

        try {
            String line = rb.readLine();

            while (line != null) {
                line = rb.readLine();
                model_file.append(line + "\n ");
            }
        } finally {
            rb.close();
        }
        System.out.println("model string: " + model_file.toString());
        */

        /*
        // Get stream from file with IOUTILS

        String text = IOUtils.toString(this.getClass().getResourceAsStream("stopwords_sv.txt"),
                "UTF-8");
        System.out.println("Stopwords text: " + text);

        */

        /*
        //Get tmp dir

        File classpathRoot = new File(System.getProperty("java.io.tmpdir"));
        System.out.println("ClassPathRoot: " + classpathRoot);

        File file = new File(classpathRoot + "/testdir");
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        } else {
            System.out.println("Directory exist!");
        }
        */

        /*
        //Get class path by resource
        File classpathRoot = new File(this.getClass().getResource(".").getPath());
        System.out.println("ClassPathRoot: " + classpathRoot.getParentFile().getParentFile().getParentFile());

        File file = new File(classpathRoot + "/testdir");
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        } else {
            System.out.println("Directory exist!");
        }
        */
    /*
    // getResource-style
    String stopwords = getClass().getResource("stopwords_sv.txt").getPath();
    System.out.println("Stopwords string: " + stopwords);

        BufferedReader br = new BufferedReader(new FileReader(stopwords));

        List stopWords = new ArrayList<>();
    try

    {

        String line = br.readLine();

        while (line != null) {
            stopWords.add(line);
            line = br.readLine();
        }

    }

    finally

    {
        System.out.println(stopWords);
        br.close();
    }
        */

        /*
        //Spring style

        final String newline = "\n";
        ApplicationContext ctx = new ClassPathXmlApplicationContext();
        Resource res = ctx.getResource("classpath:resources/lucene1/stopwords_sv.txt");
        System.out.println("File name (res): " + res);
        System.out.println("Filereader starting..");

        BufferedReader reader;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(res.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
                System.out.print("line: " + line);
                sb.append(newline);
            }
        } catch (IOException e) {
            //LOGGER.error(e);
        }

        System.out.println("Sb string:" + sb.toString());

        */

}

    public static void main(String[] args) throws IOException, URISyntaxException {
        Test test = new Test();
    }


}
