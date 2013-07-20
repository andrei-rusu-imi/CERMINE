package pl.edu.icm.cermine.pubmed.importer;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;

public class SequenceFileSplitter {

    public static void main(String[] args) throws IOException {

        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: <in_file> <out_dir> X");
            System.exit(1);
        }

        String inputSequenceFile = args[0];
        String outputSequenceFileDirectory = args[1];
        Integer maximumPairs = Integer.valueOf(args[2]);
        checkPaths(inputSequenceFile, outputSequenceFileDirectory);
        splitSequenceFile(inputSequenceFile, outputSequenceFileDirectory, maximumPairs);

    }

    private static void checkPaths(String inputSequenceFile, String outputSequenceFileDirectory) throws IOException {
        File input = new File(inputSequenceFile);
        if (!input.exists()) {
            throw new IOException("<Input file> does not exist: " + inputSequenceFile);
        }
        File outf = new File(outputSequenceFileDirectory);
        if (!outf.getParentFile().exists()) {
            boolean made = outf.getParentFile().mkdirs();
            if (!made) {
                throw new IOException("Cannot make directories!");
            }
        }
        if (!outf.exists()) {
        	boolean made = outf.mkdir();
            if (!made) {
                throw new IOException("Cannot make directories!");
            }
        } else if (!outf.isDirectory()) {
            throw new IOException("<Output dir> is not a directory:" + outputSequenceFileDirectory);
        }
    }

    private static void splitSequenceFile(String inputSequenceFile, String outputSequenceFileDirectory, Integer maximumPairs) throws IOException {
    	System.out.println("MAX " + maximumPairs);
    	SequenceFile.Writer sequenceFileWriter;
    	Integer allRecordCounter = 0;
    	Integer currentRecordCounter = 0;
    	Integer outputFileCounter = 0;
    	String inputFileName = new File(inputSequenceFile).getName();
		String outputFileName = inputFileName+"_%04d";
		StringBuilder stringBuilder = new StringBuilder();
    	Formatter formatter = new Formatter(stringBuilder, Locale.US);
		formatter.format(outputFileName, outputFileCounter);
		File outputFile = new File(outputSequenceFileDirectory, stringBuilder.toString());
		sequenceFileWriter = createSequenceFileWriter(outputFile.getPath(), BytesWritable.class, BytesWritable.class);
		++outputFileCounter;
    	
		BytesWritable key = new BytesWritable();
		BytesWritable value = new BytesWritable();
    	try {
    		Configuration conf = new Configuration();
    		FileSystem fs = FileSystem.get(conf);
    		@SuppressWarnings("deprecation")
    		SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(inputSequenceFile), conf);
    		while(reader.next(key, value)) {
    			if(currentRecordCounter.equals(maximumPairs)) {
    				if(sequenceFileWriter != null) {
    					sequenceFileWriter.close();
    				}
    				stringBuilder.delete(0, stringBuilder.length());
    				outputFileName = inputFileName+"_%04d";
    				formatter.format(outputFileName, outputFileCounter);
    				outputFile = new File(outputSequenceFileDirectory, stringBuilder.toString());
    				System.out.println("NEW FILE " + stringBuilder.toString());
    				sequenceFileWriter = createSequenceFileWriter(outputFile.getPath(), BytesWritable.class, BytesWritable.class);
    				currentRecordCounter = 0;
    				++outputFileCounter;
    			}
    			System.out.println("FILE " + outputFileCounter + " RECORD " + currentRecordCounter + " (" +allRecordCounter+" TOTAL)");
    			sequenceFileWriter.append(key, value);
    			++currentRecordCounter;
    			++allRecordCounter;
    		}
    	} finally {
    		sequenceFileWriter.close();
    	}
    }

    private static <T1, T2> SequenceFile.Writer createSequenceFileWriter(String uri, Class<T1> keyClass, Class<T2> valueClass) throws IOException {
        Configuration conf = new Configuration();
        Path path = new Path(uri);
        SequenceFile.Writer writer = SequenceFile.createWriter(conf,
                SequenceFile.Writer.file(path),
                SequenceFile.Writer.keyClass(keyClass),
                SequenceFile.Writer.valueClass(valueClass),
                SequenceFile.Writer.bufferSize(1<<25));
//        FileSystem fs = FileSystem.get(URI.create(uri), conf);
//        SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, path, keyClass, valueClass);
        return writer;
    }
}