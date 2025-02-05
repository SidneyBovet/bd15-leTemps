package ch.epfl.bigdata15.ngrams.parsing;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

/**
 * This input format will give to the mapper the year of occurence in the key
 * and the file path in value. This means every article in the file given in value
 * is written the year given in key. Thus we do not need to read metadata
 * 
 * @author Zhivka Gucevska & Florian Junker
 */
public class XMLInputFormat extends FileInputFormat<Text, Path> {
    public RecordReader<Text, Path> createRecordReader(
            InputSplit split, TaskAttemptContext context) {
        return new XMLRecordReader();
    }

    public class XMLRecordReader extends RecordReader<Text, Path> {
        private Text key;
        private Path value;
        private FileStatus[] files;
        private int nextId = 0;

        @Override
        public void close() throws IOException {
        }

        @Override
        public Text getCurrentKey() throws IOException,
                InterruptedException {
            return key;
        }

        @Override
        public Path getCurrentValue() throws IOException, InterruptedException {
            return value;
        }

        @Override
        public float getProgress() throws IOException, InterruptedException {
            return files == null ? 1 : files.length > 0 ? ((float) nextId) / files.length : 1;
        }

        @Override
        public void initialize(InputSplit inputSplit, TaskAttemptContext context)
                throws IOException, InterruptedException {

        	//We read the input information
            FileSplit split = (FileSplit) inputSplit;
            Configuration conf = context.getConfiguration();
            Path path = split.getPath();
            FileSystem fs = path.getFileSystem(conf);
            files = fs.listStatus(path);
            fs.close();
        }

        @Override
        public boolean nextKeyValue() throws IOException, InterruptedException {
            if (files == null || nextId >= files.length) {
                return false;
            }

            // For the current input file set 
            // Key : Year of writting
            // Value : Path of file containning articles
            String name = files[nextId].getPath().getName();
            key = new Text(name.substring(name.length() - 8, name.length() - 4));
            value = files[nextId].getPath();
            nextId++;
            return true;
        }
    }
}
