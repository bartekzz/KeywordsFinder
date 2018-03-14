package urlparsing;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import database.DatabaseQueries;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

// Parse urls from ASX-json file
public class ParseAsxUrls {

    public static void main (String[] args) {

        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(new FileReader("/Users/bartek/asx-urls.txt"));

            JSONObject jsonObject =  (JSONObject) obj;

            JSONArray sites = (JSONArray) jsonObject.get("rtbSites");

            Iterator iterator = sites.iterator();
            while (iterator.hasNext()) {
                //System.out.println(iterator.next());
                JSONObject site = (JSONObject) iterator.next();
                String siteURL = (String)site.get("siteURL");
                System.out.println(siteURL);
                DatabaseQueries.insertUrls(siteURL);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
