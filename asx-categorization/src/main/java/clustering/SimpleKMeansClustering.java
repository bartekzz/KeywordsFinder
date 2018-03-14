package clustering;

import com.google.common.collect.Lists;
import common.CommonStrings;
import database.DatabaseQueries;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.canopy.CanopyDriver;
import org.apache.mahout.clustering.classify.WeightedPropertyVectorWritable;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.common.distance.TanimotoDistanceMeasure;
import org.apache.mahout.text.LuceneStorageConfiguration;
import org.apache.mahout.text.SequenceFilesFromLuceneStorage;
import org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleKMeansClustering {


    public SimpleKMeansClustering() throws Exception {

        //Delete clustering dierctory
        File file = new File(CommonStrings.ROOTDIR + "/clustering");
        FileUtils.deleteDirectory(file);

        Map<String, Path> paths = new HashMap<>();
        Path indexFilesPath_en = new Path(CommonStrings.ROOTDIR + "/index_en");
        Path indexFilesPath_sv = new Path(CommonStrings.ROOTDIR + "/index_sv");
        paths.put("en", indexFilesPath_en);
        paths.put("sv", indexFilesPath_sv);

        for (Map.Entry<String, Path> entry : paths.entrySet()) {
            String lang = entry.getKey();
            Path path = entry.getValue();

            long startTime = System.currentTimeMillis();

            // Execute clustering
            Configuration conf = new Configuration();
            FileSystem fs = FileSystem.get(conf);

            Path sequenceFilesPath = new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/testdata/sequencefiles/");
            Path sparseVectorsPath = new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/testdata/sparsevectors/");
            Path tfVectorsPath = new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/testdata/sparsevectors/tf-vectors");
            Path inputClustersPath = new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/testdata/input-clusters");
            Path finishedInputClustersPath = new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/testdata/input-clusters/clusters-0-final");
            Path finalClustersPath = new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/output");

            //Create sequence files from Index
            LuceneStorageConfiguration luceneStorageConf = new LuceneStorageConfiguration(conf,
                    Arrays.asList(path), sequenceFilesPath, "url",
                    Arrays.asList("contents"));

            long endTimeluceneStorageConf = System.currentTimeMillis();
            System.out.println("luceneStorageConf processed, time taken: "
                    +(endTimeluceneStorageConf-startTime)+" ms");

            SequenceFilesFromLuceneStorage sequenceFilefromLuceneStorage = new SequenceFilesFromLuceneStorage();
            sequenceFilefromLuceneStorage.run(luceneStorageConf);

            long endTimesequenceFilefromLuceneStorage = System.currentTimeMillis();
            System.out.println("sequenceFilefromLuceneStorage processed, time taken: "
                    +(endTimesequenceFilefromLuceneStorage-startTime)+" ms");

            //Generate Sparse vectors from sequence files
            generateSparseVectors(true,
                    true,
                    true,
                    100,
                    4,
                    sequenceFilesPath,
                    sparseVectorsPath);

            long endTimegenerateSparseVectors = System.currentTimeMillis();
            System.out.println("endTimegenerateSparseVectors processed, time taken: "
                    +(endTimegenerateSparseVectors-startTime)+" ms");

            //Generate input clusters for K-means (instead of have K randomly initiated)
            TanimotoDistanceMeasure tanimoDistance = new TanimotoDistanceMeasure();
            CanopyDriver.run(tfVectorsPath,
                    inputClustersPath,
                    tanimoDistance,
                    //(float) 3.1,
                    (float) 1.5,
                    //(float) 2.1,
                    (float) 0.3,
                    false,
                    // float 0.2
                    (float) 0.2,
                    true);

            long endTimegenerateTanimotoDistanceMeasure = System.currentTimeMillis();
            System.out.println("endTimegenerateTanimotoDistanceMeasure processed, time taken: "
                    +(endTimegenerateTanimotoDistanceMeasure-startTime)+" ms");

            //Generate K-Means clusters
            KMeansDriver.run(conf,
                    tfVectorsPath,
                    finishedInputClustersPath,
                    finalClustersPath,
                    0.001,
                    10,
                    true,
                    0,
                    true);

            long endTimeKMeansDriver = System.currentTimeMillis();
            System.out.println("endTimegenerateSparseVectors processed, time taken: "
                    +(endTimeKMeansDriver-startTime)+" ms");


            //Read and print out the clusters in the console
            SequenceFile.Reader reader = new SequenceFile.Reader(fs,
                    new Path(CommonStrings.ROOTDIR + "/clustering/" + lang + "/output/" + Cluster.CLUSTERED_POINTS_DIR + "/part-m-0"),
                    conf);


            long endTimeSequenceFileReader = System.currentTimeMillis();
            System.out.println(" endTimeSequenceFileReader processed, time taken: "
                    +(endTimeSequenceFileReader-startTime)+" ms");

            IntWritable key = new IntWritable();
            WeightedPropertyVectorWritable value = new WeightedPropertyVectorWritable();
            Pattern p = Pattern.compile("(.*)(\\:\\{)");
            while (reader.next(key, value)) {
                System.out.println("Value get vector:" + value.getVector().toString());
                Matcher m = p.matcher(value.getVector().toString());

                if (m.find( )) {
                    System.out.println("Found value: " + m.group(1) );
                }else {
                    System.out.println("NO MATCH");
                }

                //System.out.println(value.toString() + " belongs to cluster " + key.toString());
                System.out.println(value.toString());
                System.out.println("Belongs to cluster " + key.toString());

                // Set cluster number in database
                DatabaseQueries.updateCluster( m.group(1), Integer.parseInt(key.toString()) );

                long endTimewriteToDB = System.currentTimeMillis();
                System.out.println("endTimewriteToDB processed, time taken: "
                        +(endTimewriteToDB-startTime)+" ms");
            }
            reader.close();

            long endTime = System.currentTimeMillis();
            System.out.println("Clustering processed, time taken: "
                    +(endTime-startTime)+" ms");

            }
    }

    public static void generateSparseVectors (boolean tfWeighting, boolean sequential, boolean named, double maxDFSigma, int numDocs, Path inputPath, Path outputPath) throws Exception {

        List argList = Lists.newLinkedList();
        argList.add("-i");
        argList.add(inputPath.toString());
        argList.add("-o");
        argList.add(outputPath.toString());

        if (sequential) {
            argList.add("-seq");
        }

        if (named) {
            argList.add("-nv");
        }

        if (maxDFSigma >= 0) {
            argList.add("--maxDFSigma");
            argList.add(String.valueOf(maxDFSigma));
        }

        if (tfWeighting) {
            argList.add("--weight");
            argList.add("tf");
        }

        String[] args = (String[])argList.toArray(new String[argList.size()]);

            ToolRunner.run(new SparseVectorsFromSequenceFiles(), args);
    }

    public static void main(String args[]) throws Exception {

        SimpleKMeansClustering simpleKMeansClustering = new SimpleKMeansClustering();

    }

}