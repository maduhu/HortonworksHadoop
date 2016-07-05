package hortonworks.mapreduce;

import java.io.IOException;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import hortonworks.mapreduce.WordCountNewAPI.MyMapper;
import hortonworks.mapreduce.WordCountNewAPI.MyReducer;

public class SortAscMonthDescWeek {
	
	public static class SortAscMonthDescWeekMapper extends 
		Mapper<LongWritable, Text, MonthDoWWritable, DelaysWritable>{
	
		public void map(LongWritable key, Text value, Context context) 
					throws IOException, InterruptedException {
			if(!AirlineDataUtils.isHeader(value)) {
				String[] contents = value.toString().split(",");
				String month = AirlineDataUtils.getMonth(contents);
				String dow = AirlineDataUtils.getDayOfTheWeek(contents);
				MonthDoWWritable mw = new MonthDoWWritable();
				mw.month = new IntWritable(Integer.parseInt(month));
				mw.dayOfWeek = new IntWritable(Integer.parseInt(dow));
				DelaysWritable dw = AirlineDataUtils.parseDelaysWritable(value.toString());
				context.write(mw, dw);
			}
		}
	}

	public static class SortAscMonthDescWeekReducer extends
		Reducer<MonthDoWWritable, DelaysWritable, NullWritable, Text> {
		
		public void reduce(MonthDoWWritable key, Iterable<DelaysWritable> values, Context context)
			throws IOException, InterruptedException {
			for (DelaysWritable val: values) {
				Text t = AirlineDataUtils.parseDelaysWritableToText(val);
				context.write(NullWritable.get(), t);
			}
		}
	}
	
	public static class MonthDoWPartitioner extends 
		Partitioner<MonthDoWWritable, Text> implements Configurable {
		private Configuration conf = null;
		private int indexRange = 0;
		
		private int getDefaultRange() {
			int minIndex = 0;
			int maxIndex = 11 * 7 + 6;
			int range = (maxIndex - minIndex) + 1;
			return range;
		}

		public Configuration getConf() {
			return this.conf;
		}

		public void setConf(Configuration conf) {
			this.conf = conf;
			this.indexRange = conf.getInt("key.range", getDefaultRange());
		}

		@Override
		public int getPartition(MonthDoWWritable key, Text value, int numReduceTasks) {
			return AirlineDataUtils.getCustomPartition(key, indexRange, numReduceTasks);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Job job = Job.getInstance(new Configuration());
		job.setJarByClass(SortAscMonthDescWeek.class);
		job.setMapOutputKeyClass(MonthDoWWritable.class);
		job.setMapOutputValueClass(DelaysWritable.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setMapperClass(SortAscMonthDescWeekMapper.class);
		job.setReducerClass(SortAscMonthDescWeekReducer.class);
		job.setPartitionerClass(MonthDoWPartitioner.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		boolean status = job.waitForCompletion(true);
		if(status) {
			System.exit(0);
		}
		else {
			System.exit(1);
		}
	}
	
}
