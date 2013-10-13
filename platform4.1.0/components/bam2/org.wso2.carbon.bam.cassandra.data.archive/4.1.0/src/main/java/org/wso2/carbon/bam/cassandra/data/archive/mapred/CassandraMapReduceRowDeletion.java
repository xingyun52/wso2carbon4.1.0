package org.wso2.carbon.bam.cassandra.data.archive.mapred;


import me.prettyprint.cassandra.serializers.ObjectSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.hadoop.ColumnFamilyInputFormat;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveContext;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.thrift.TBaseHelper;
import org.wso2.carbon.bam.cassandra.data.archive.util.CassandraArchiveUtil;
import org.wso2.carbon.cassandra.dataaccess.ClusterInformation;
import org.wso2.carbon.databridge.persistence.cassandra.datastore.CassandraConnector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.SortedMap;

public class CassandraMapReduceRowDeletion extends Configured implements Tool {

    private static final String JOB_NAME = "PURGE_CASSANDRA_DATA";
    private static final String INPUT_PARTITIONER = "org.apache.cassandra.dht.RandomPartitioner";


    private static final String INPUT_KEYSPACE_USERNAME_CONFIG = "cassandra.input.keyspace.username";
    private static final String INPUT_KEYSPACE_PASSWD_CONFIG = "cassandra.input.keyspace.passwd";

    public int run(String[] strings) throws Exception {

        ClusterInformation clusterInformation = new ClusterInformation("admin",
                "admin");
        Cluster cluster = CassandraArchiveUtil.getDataAccessService().getCluster(clusterInformation);

        CassandraArchiveUtil.setCluster(cluster);

        HiveConf hiveConf = HiveContext.getCurrentContext().getConf();
        Job job = new Job(hiveConf, JOB_NAME);
        job.setJarByClass(CassandraMapReduceRowDeletion.class);
        job.setMapperClass(RowKeyMapper.class);

        job.setInputFormatClass(ColumnFamilyInputFormat.class);

        job.setNumReduceTasks(0);
        ConfigHelper.setRangeBatchSize(getConf(), 1000);


        SliceRange sliceRange = new SliceRange(ByteBuffer.wrap(new byte[0]),
                ByteBuffer.wrap(new byte[0]), true, 1000);

        SlicePredicate slicePredicate = new SlicePredicate();
        slicePredicate.setSlice_range(sliceRange);

        Configuration configuration = job.getConfiguration();
        configuration.set(INPUT_KEYSPACE_USERNAME_CONFIG, "admin");
        configuration.set(INPUT_KEYSPACE_PASSWD_CONFIG, "admin");
        String columnFamilyName = configuration.get(CassandraArchiveUtil.COLUMN_FAMILY_NAME);
        String cassandraPort = configuration.get(CassandraArchiveUtil.CASSANDRA_PORT);
        String cassandraHostIp = configuration.get(CassandraArchiveUtil.CASSANDRA_HOST_IP);
        ConfigHelper.setInputColumnFamily(configuration, CassandraConnector.BAM_EVENT_DATA_KEYSPACE, columnFamilyName);
        ConfigHelper.setInputRpcPort(configuration, cassandraPort);
        ConfigHelper.setInputInitialAddress(configuration, cassandraHostIp);
        ConfigHelper.setInputPartitioner(configuration, INPUT_PARTITIONER);
        ConfigHelper.setInputSlicePredicate(configuration, slicePredicate);

        FileOutputFormat.setOutputPath(job, new Path(strings[0]));


        job.waitForCompletion(true);

        cluster.dropColumnFamily(CassandraConnector.BAM_EVENT_DATA_KEYSPACE,columnFamilyName);

        return job.isSuccessful() ? 0 : 1;
    }


    public static class RowKeyMapper extends Mapper<ByteBuffer, SortedMap<ByteBuffer, IColumn>, Text, LongWritable> {

        private static StringSerializer stringSerializer = StringSerializer.get();
        private static int count=0;

        public void map(ByteBuffer key, SortedMap<ByteBuffer, IColumn> columns, Context context) throws IOException, InterruptedException {

            Cluster cluster = CassandraArchiveUtil.getCassandraCluster();
            Configuration configuration = context.getConfiguration();
            String columnFamilyName = configuration.get(CassandraArchiveUtil.CASSANDRA_ORIGINAL_CF);

            String rowkey = StringSerializer.get().fromByteBuffer(TBaseHelper.rightSize(key));
            Keyspace keyspace = HFactory.createKeyspace(CassandraConnector.BAM_EVENT_DATA_KEYSPACE, cluster);
            ColumnFamilyTemplate<String, String> template = new ThriftColumnFamilyTemplate<String, String>(keyspace,
                    columnFamilyName,
                    stringSerializer,
                    stringSerializer);
            template.deleteRow(rowkey);

        }
    }
}
