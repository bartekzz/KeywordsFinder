package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// JDK 1.7 and above
public class DatabaseQueries {    // Save as "JdbcUpdateTest.java"

    static String tableName = DatabaseFields.TABLE_NAME;

    public DatabaseQueries() {
        //insertRecords(144, 106, 1);
    }

    public static int selectLock() {
        int lock = 0;
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {

            // Issue a SELECT to check the changes
            String strSelect = "select * from urls_lock";
            System.out.println("The SQL query is: " + strSelect);  // Echo For debugging
            ResultSet rset = stmt.executeQuery(strSelect);


            while (rset.next()) {   // Move the cursor to the next row
                /*
                System.out.println(rset.getInt("id") + ", "
                        + rset.getString("url") + ", "
                        + rset.getString("indexed"));
                */
                lock = rset.getInt("lock");
                System.out.println(lock);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return lock;
    }

    public static void updateSetLock(int lock) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {

            // INSERT a partial record
            String sqlInsert = "update urls_lock set `lock` = " + lock + " where `id` = 1";
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateSetInProcess(int id, int inProcess) {
        if(inProcess != 0 && inProcess != 1 ) {
            System.out.println("Invalid value. Must be 0 or 1.");
            System.exit(1);
        }
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {
            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set in_process = " + inProcess + " where id = " + id;
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateFinished(int id, int indexed, int analyzed) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set indexed = " + indexed + ", analyzed = " + analyzed + " where id = " + id;
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static List<Map<String, DatabaseRecord>> selectRecords(int indexed, int analyzed, int limit) {
        List<Map<String, DatabaseRecord>> urlList = null;
        Map<String, DatabaseRecord> urlMap;

        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {
            // Add a loop with OFFSET (if indexed = 0) for chosen record(s)
            // Issue a SELECT to check the changes
            String strSelect = "select * from " + tableName + " where indexed = " + indexed + " and analyzed = " + analyzed + " and in_process = 0 and error is null limit " + limit;
            System.out.println("The SQL query is: " + strSelect);  // Echo For debugging
            ResultSet rset = stmt.executeQuery(strSelect);

            urlList = new ArrayList<>();

            DatabaseRecord databaseRecord = null;

            while (rset.next()) {   // Move the cursor to the next row

                if(indexed == 0) {
                    databaseRecord = new DatabaseRecord(rset.getInt("id"));
                }
                else if (indexed == 1) {
                    databaseRecord = new DatabaseRecord(rset.getInt("id"), rset.getString("lang"));
                }
                urlMap = new HashMap<>();
                urlMap.put(rset.getString("url"), databaseRecord);
                urlList.add(urlMap);
                //System.out.println(urlList);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return urlList;
    }

    public static Map<String, DatabaseRecord> getLangForUrl(String url, int indexed) {
        Map<String, DatabaseRecord> urlMap = null;

        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {
            // Add a loop with OFFSET (if indexed = 0) for chosen record(s)
            // Issue a SELECT to check the changes
            String strSelect = "select id, url, lang from " + tableName + " where indexed = " + indexed + " and url = '" + url + "' and in_process = 0 and error is null";
            System.out.println("The SQL query is: " + strSelect);  // Echo For debugging
            ResultSet rset = stmt.executeQuery(strSelect);

            DatabaseRecord databaseRecord = null;

            while (rset.next()) {   // Move the cursor to the next row

                if (indexed == 1) {
                    databaseRecord = new DatabaseRecord(rset.getInt("id"), rset.getString("lang"));
                    urlMap = new HashMap<>();
                    urlMap.put(rset.getString("url"), databaseRecord);
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return urlMap;
    }

    public static void updateKeywords(int id, String keywords) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set keywords = '" + keywords + "' where id = " + id;
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateCluster(String url, int cluster) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {

            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set cluster = '" + cluster + "' where url = '" + url + "' and indexed = 1 and cluster is null";
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateLang(int id, String lang) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set lang = '" + lang + "' where id = " + id;
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void insertUrls(String url) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {

            // INSERT a partial record
            String sqlInsert = "insert into " + tableName + " (url) values ('" + url + "')";
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records inserted.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateError(int id, int error) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {

            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set error = '" + error + "' where id = " + id;
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateRedirectUrl(int id, String url) {
        try (
                // Step 1: Allocate a database 'Connection' object
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/lucene?useSSL=false&serverTimezone=UTC", "root", "Bartek82"); // MySQL

                // Step 2: Allocate a 'Statement' object in the Connection
                Statement stmt = conn.createStatement();
        ) {

            // INSERT a partial record
            String sqlInsert = "update " + tableName + " set redirect_url = '" + url + "' where id = " + id;
            System.out.println("The SQL query is: " + sqlInsert);  // Echo for debugging
            int countInserted = stmt.executeUpdate(sqlInsert);
            System.out.println(countInserted + " records updated.\n");

        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void databaseSetLock() {
        if (selectLock() == 1) {
            try {
                System.out.println("Sleep ...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Trying again...");
            databaseSetLock();
        } else {
            System.out.println("Setting lock...");
            updateSetLock(1);
        }
    }

    public void databaseUnSetLock() {
        System.out.println("Unsetting lock...");
        updateSetLock(0);
    }

    public List<Map<String, DatabaseRecord>> createListFromSelectedRecords(int indexed, int analyzed, int limit) {
        //Lock table
        databaseSetLock();

        List<Map<String, DatabaseRecord>> urlList = selectRecords(indexed, analyzed, limit);
        System.out.println("urlList: " + urlList);

        for (int i = 0; i < urlList.size(); i++) {

            //System.out.println("/" + urlList.get(i) + " /");

            // Update database, set url to in_process
            for (Map.Entry<String, DatabaseRecord> entry : urlList.get(i).entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                DatabaseRecord record = (DatabaseRecord)value;
                int id = record.getId();

                updateSetInProcess(id, 1);
            }
        }

        databaseUnSetLock();

        return urlList;
    }


    public static void main(String[] args) {
        //DatabaseQueries conn = new DatabaseQueries();
    }
}
