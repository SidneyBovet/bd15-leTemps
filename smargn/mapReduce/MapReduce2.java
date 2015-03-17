import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.GZIPInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;

public class MapReduce2 {
    //                                          Type the Map receive || Type output of Map        
    public static class Map extends Mapper<Object, Text, Text, Text> {

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      String keyIn =(value.toString()).split("\\s+")[0];
      
      String valueIn =(value.toString()).split("\\s+")[1];
      
      String keyInYear = keyIn.split("_")[0];
      
      String keyInWord = keyIn.split("_")[1];
      if(keyInWord.equals("coloriage")){
        System.out.println("keyIn => "+keyIn);
        System.out.println("valueIn => "+valueIn);
        System.out.println("keyInYear => "+keyInYear);
        System.out.println("keyInWord => "+keyInWord);
        System.out.println(":: FINALITY ::");
        System.out.println(keyInWord +" :: "+keyInYear+"_"+valueIn);
      }

      context.write(new Text(keyInWord), new Text(keyInYear+"_"+valueIn));
    }
    }


    public static class MyReducer
       extends Reducer<Text,Text,Text,Text> {

    public void reduce(Text key, Iterable<Text> values,
                       Context context
                       ) throws IOException, InterruptedException {
      ArrayList<String> yearCounter = new ArrayList<String>();
      String result = "";

      for (Text val : values) {
        yearCounter.add(val.toString());
      }
      Collections.sort(yearCounter);
      ArrayList<String> listFinal = new ArrayList<String>();
    
      int base = 1800;
      int rootYear = 1800;
      int lastyear = 2000;
      for (int i = 0; i < yearCounter.size(); i++) {
        int existingYear = Integer.parseInt(yearCounter.get(i).split("_")[0]); //1905
        int missingNumYear = existingYear - rootYear; // 3
        if(missingNumYear == 0){
          listFinal.add(rootYear-base,yearCounter.get(i));
          rootYear += 1; 
        }
        else{
          for(int j = 0; j<missingNumYear; j++){
            listFinal.add(rootYear-base, "0");
            rootYear += 1;
          }
          listFinal.add(rootYear-base,yearCounter.get(i));
          rootYear += 1;
        }
      }
      int tempsize = listFinal.size();
      for(int i = listFinal.size();i<= tempsize+lastyear-rootYear; i++){
        listFinal.add(i,"0");
      }


      for (String s : listFinal){
        if(!s.equals("0")){
          s = s.split("_")[1];
        }
        result += s + " ";
      }
      

      


      context.write(key, new Text(result));
    }
  }    

    public static void main(String[] args) throws Exception {

        // FIRST JOB
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "MapReduce2");
        job.setJarByClass(MapReduce2.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(MyReducer.class);


        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        //job.setInputFormatClass(TupleInputFormat2.class);
        //job.setOutputFormatClass(GZipFileOutputFormat.class);


        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);

    }


    /**
     * Extends the basic FileOutputFormat class provided by Apache Hadoop to
     * write the file in the wanted format.
     */
    public static class GZipFileOutputFormat extends FileOutputFormat<Text, BytesWritable> {
        @Override
        public RecordWriter<Text, BytesWritable> getRecordWriter(TaskAttemptContext job) {
             // create our record writer with the new file
            return new GZipRecordWriter(job);

        }
    }
    /*
     * Code based on the article http://johnnyprogrammer.blogspot.ch/2012/01/custom-file-output-in-hadoop.html
     */
    public static class GZipRecordWriter extends RecordWriter<Text, BytesWritable> {
        Path path = null;
        Path fullPath = null;
        FileSystem fs = null;
        FSDataOutputStream fileOut = null;
        DataOutputStream out = null;
        TaskAttemptContext job = null;

        public GZipRecordWriter(TaskAttemptContext job) {
            this.job = job;
            path = FileOutputFormat.getOutputPath(job);
        }

        @Override
        public void close(TaskAttemptContext job) throws IOException, InterruptedException {
            out.close();
        }

        @Override
        public void write(Text key, BytesWritable value) throws IOException, InterruptedException {
            if(key != null && value != null){
                String keyString = key.toString();
                // Remove .gz extension in file name
                String cleanedKeyString = keyString.substring(0,keyString.length()-3);
                fullPath = new Path(path,cleanedKeyString);
                // Create the file in the file system
                try {
                    fs = path.getFileSystem(job.getConfiguration());
                    fileOut = fs.create(fullPath, job);
                    out = fileOut;
                } catch (Exception ignore) {
                    System.out.println("Error");
                }
                // Write out our 'value'
                fileOut.write(value.getBytes(),0,value.getLength());
            }
        }
    }

}
