/*
 * Copyright 2012 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.databridge.persistence.cassandra.datastore;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.ColumnDefinition;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.persistence.cassandra.Utils.CassandraSDSUtils;
import org.wso2.carbon.databridge.persistence.cassandra.caches.CFCache;
import org.wso2.carbon.databridge.persistence.cassandra.exception.NullValueException;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.BoolInserter;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.DoubleInserter;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.FloatInserter;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.IntInserter;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.LongInserter;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.StringInserter;
import org.wso2.carbon.databridge.persistence.cassandra.inserter.TypeInserter;
import org.wso2.carbon.databridge.persistence.cassandra.internal.util.AppendUtils;
import org.wso2.carbon.databridge.persistence.cassandra.internal.util.ServiceHolder;
import org.wso2.carbon.databridge.persistence.cassandra.internal.util.Utils;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cassandra backend connector  and related operations
 */
public class CassandraConnector {

    private static final String STREAM_NAME_KEY = "Name";


    private static final String STREAM_VERSION_KEY = "Version";
    private static final String STREAM_NICK_NAME_KEY = "Nick_Name";
    private static final String STREAM_TIMESTAMP_KEY = "Timestamp";
    private static final String STREAM_DESCRIPTION_KEY = "Description";

    private static final String STREAM_ID_KEY = "StreamId";
    public static final String BAM_META_STREAM_DEF_CF = "STREAM_DEFINITION";

    public static final String BAM_META_KEYSPACE = "META_KS";

    public static final String BAM_EVENT_DATA_KEYSPACE = "EVENT_KS";

    private volatile AtomicInteger eventCounter = new AtomicInteger();

    private volatile AtomicLong totalEventCounter = new AtomicLong();

    private static final String STREAM_DEF = "STREAM_DEFINITION";
    private final static StringSerializer stringSerializer = StringSerializer.get();
    private final static LongSerializer longSerializer = LongSerializer.get();
    private final static ByteBufferSerializer byteBufferSerializer = ByteBufferSerializer.get();

    private AtomicInteger rowkeyCounter = new AtomicInteger();

    static Log log = LogFactory.getLog(CassandraConnector.class);

    private Map<AttributeType, TypeInserter> inserterMap = new ConcurrentHashMap<AttributeType, TypeInserter>();

    // Map to hold Cassandra comparator class names for each attribute type
    private static Map<AttributeType, String> attributeComparatorMap =
            new HashMap<AttributeType, String>();

    private static final String COMPARATOR_BOOL_TYPE = "org.apache.cassandra.db.marshal.BooleanType";
    private static final String COMPARATOR_DOUBLE_TYPE = "org.apache.cassandra.db.marshal.DoubleType";
    private static final String COMPARATOR_FLOAT_TYPE = "org.apache.cassandra.db.marshal.FloatType";

    private int port = 0;
    private String localAddress = null;
    private long startTime;

    private boolean IS_PERFORMANCE_MEASURED = false;

    static {
        attributeComparatorMap.put(AttributeType.STRING, ComparatorType.UTF8TYPE.getClassName());
        attributeComparatorMap.put(AttributeType.INT, ComparatorType.INTEGERTYPE.getClassName());
        attributeComparatorMap.put(AttributeType.LONG, ComparatorType.LONGTYPE.getClassName());
        attributeComparatorMap.put(AttributeType.FLOAT, COMPARATOR_FLOAT_TYPE);
        attributeComparatorMap.put(AttributeType.DOUBLE, COMPARATOR_DOUBLE_TYPE);
        attributeComparatorMap.put(AttributeType.BOOL, COMPARATOR_BOOL_TYPE);
    }


    public CassandraConnector() {

        if (System.getProperty("profile.receiver") != null) {
            IS_PERFORMANCE_MEASURED = System.getProperty("profile.receiver").equals("true");
        }
        try {
            AxisConfiguration axisConfiguration =
                    ServiceHolder.getConfigurationContextService().getServerConfigContext().getAxisConfiguration();

            String portOffset = CarbonUtils.getServerConfiguration().
                    getFirstProperty("Ports.Offset");
            port = CarbonUtils.getTransportPort(axisConfiguration, "https") +
                   Integer.parseInt(portOffset);

            localAddress = Utils.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Error when detecting Host/Port, using defaults");
            }
            localAddress = (localAddress == null) ? "127.0.0.1" : localAddress;
            port = (port == 0) ? 9443 : port;
        }

        createInserterMap();

    }

    void commit(Mutator mutator) throws StreamDefinitionStoreException {
        mutator.execute();
    }

    private void createInserterMap() {
        inserterMap.put(AttributeType.INT, new IntInserter());
        inserterMap.put(AttributeType.BOOL, new BoolInserter());
        inserterMap.put(AttributeType.LONG, new LongInserter());
        inserterMap.put(AttributeType.FLOAT, new FloatInserter());
        inserterMap.put(AttributeType.STRING, new StringInserter());
        inserterMap.put(AttributeType.DOUBLE, new DoubleInserter());
    }

    public ColumnFamilyDefinition getColumnFamily(Cluster cluster, String keyspaceName,
                                                  String columnFamilyName) {

        Keyspace keyspace = getKeyspace(keyspaceName, cluster);
        KeyspaceDefinition keyspaceDef =
                cluster.describeKeyspace(keyspace.getKeyspaceName());
        List<ColumnFamilyDefinition> cfDef = keyspaceDef.getCfDefs();
        for (ColumnFamilyDefinition cfdef : cfDef) {
            if (cfdef.getName().equals(columnFamilyName)) {
                return cfdef;
            }
        }

        return null;
    }

    public ColumnFamilyDefinition createColumnFamily(Cluster cluster, String keyspaceName,
                                                     String columnFamilyName,
                                                     StreamDefinition streamDefinition) {
        Keyspace keyspace = getKeyspace(keyspaceName, cluster);
        KeyspaceDefinition keyspaceDef =
                cluster.describeKeyspace(keyspace.getKeyspaceName());
        List<ColumnFamilyDefinition> cfDef = keyspaceDef.getCfDefs();
        for (ColumnFamilyDefinition cfdef : cfDef) {
            if (cfdef.getName().equals(columnFamilyName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Column Family " + columnFamilyName + " already exists.");
                }
                CFCache.putCF(cluster, keyspaceName, columnFamilyName, true);
                return cfdef;
            }
        }
        ColumnFamilyDefinition columnFamilyDefinition = new BasicColumnFamilyDefinition();
        columnFamilyDefinition.setKeyspaceName(keyspaceName);
        columnFamilyDefinition.setName(columnFamilyName);
        columnFamilyDefinition.setKeyValidationClass(ComparatorType.UTF8TYPE.getClassName());
        columnFamilyDefinition.setComparatorType(ComparatorType.UTF8TYPE);

        Map<String, String> compressionOptions = new HashMap<String, String>();
        compressionOptions.put("sstable_compression", "SnappyCompressor");
        compressionOptions.put("chunk_length_kb", "128");
        columnFamilyDefinition.setCompressionOptions(compressionOptions);

        addMetaColumnDefinitionsToColumnFamily(columnFamilyDefinition);

        if (streamDefinition != null) {
            addColumnDefinitionsToColumnFamily(streamDefinition.getPayloadData(),
                                               DataType.payload, columnFamilyDefinition);
            addColumnDefinitionsToColumnFamily(streamDefinition.getMetaData(),
                                               DataType.meta, columnFamilyDefinition);
            addColumnDefinitionsToColumnFamily(streamDefinition.getCorrelationData(),
                                               DataType.correlation, columnFamilyDefinition);
        }

        cluster.addColumnFamily(new ThriftCfDef(columnFamilyDefinition), true);

        // give some time to propogate changes
        keyspaceDef =
                cluster.describeKeyspace(keyspace.getKeyspaceName());
        int retryCount = 0;
        while (retryCount < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }

            for (ColumnFamilyDefinition cfdef : keyspaceDef.getCfDefs()) {
                if (cfdef.getName().equals(columnFamilyName)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Column Family " + columnFamilyName + " already exists.");
                    }
                    CFCache.putCF(cluster, keyspaceName, columnFamilyName, true);
                    return cfdef;
                }
            }
            retryCount++;
        }

        throw new RuntimeException("The column family " + columnFamilyName + " was  not created");
    }


    public boolean createKeySpaceIfNotExisting(Cluster cluster, String keySpaceName) {

        KeyspaceDefinition keySpaceDef = cluster.describeKeyspace(keySpaceName);

        if (keySpaceDef == null) {
            cluster.addKeyspace(HFactory.createKeyspaceDefinition(
                    keySpaceName, Utils.getStrategyClass(), Utils.getReplicationFactor(), null));

            keySpaceDef = cluster.describeKeyspace(keySpaceName);
            //Sometimes it takes some time to make keySpaceDef!=null
            int retryCount = 0;
            while (keySpaceDef == null && retryCount < 100) {
                try {
                    Thread.sleep(100);
                    keySpaceDef = cluster.describeKeyspace(keySpaceName);
                    if (keySpaceDef != null) {
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            return true;
        } else {
            return false;
        }


    }

    public List<String> insertEventList(Credentials credentials, Cluster cluster,
                                        List<Event> eventList)
            throws StreamDefinitionStoreException {
        StreamDefinition streamDef;

        Mutator<String> mutator = getMutator(cluster);

        List<String> rowKeyList = new ArrayList<String>();
        startTimeMeasurement(IS_PERFORMANCE_MEASURED);


        for (Event event : eventList) {

            String rowKey;
            streamDef = getStreamDefinitionFromStore(credentials, event.getStreamId());
            String streamColumnFamily = CassandraSDSUtils.convertStreamNameToCFName(
                    streamDef.getName());
            if ((streamDef == null) || (streamColumnFamily == null)) {
                String errorMsg = "Event stream definition or column family cannot be null";
                log.error(errorMsg);
                throw new StreamDefinitionStoreException(errorMsg);
            }


            if (log.isTraceEnabled()) {
                KeyspaceDefinition keyspaceDefinition = cluster.describeKeyspace(BAM_EVENT_DATA_KEYSPACE);
                log.trace("Keyspace desc. : " + keyspaceDefinition);

                String CFInfo = "CFs present \n";
                for (ColumnFamilyDefinition columnFamilyDefinition : keyspaceDefinition.getCfDefs()) {
                    CFInfo += "cf name : " + columnFamilyDefinition.getName() + "\n";
                }
                log.trace(CFInfo);
            }


            eventCounter.incrementAndGet();


            // / add  current server time as time stamp if time stamp is not set
            long timestamp;
            if (event.getTimeStamp() != 0L) {
                timestamp = event.getTimeStamp();
            } else {
                timestamp = System.currentTimeMillis();
            }


            rowKey = CassandraSDSUtils.createRowKey(timestamp, localAddress, port, rowkeyCounter.incrementAndGet());

            String streamDefDescription = streamDef.getDescription();
            String streamDefNickName = streamDef.getNickName();

            mutator.addInsertion(rowKey, streamColumnFamily,
                                 HFactory.createStringColumn(STREAM_ID_KEY, streamDef.getStreamId()));
            mutator.addInsertion(rowKey, streamColumnFamily,
                                 HFactory.createStringColumn(STREAM_NAME_KEY, streamDef.getName()));
            mutator.addInsertion(rowKey, streamColumnFamily,
                                 HFactory.createStringColumn(STREAM_VERSION_KEY, streamDef.getVersion()));

            if (streamDefDescription != null) {
                mutator.addInsertion(rowKey, streamColumnFamily,
                                     HFactory.createStringColumn(STREAM_DESCRIPTION_KEY, streamDefDescription));
            }
            if (streamDefNickName != null) {
                mutator.addInsertion(rowKey, streamColumnFamily,
                                     HFactory.createStringColumn(STREAM_NICK_NAME_KEY, streamDefNickName));
            }

            mutator.addInsertion(rowKey, streamColumnFamily,
                                 HFactory.createColumn(STREAM_TIMESTAMP_KEY, timestamp, stringSerializer,
                                                       longSerializer));

            if (event.getArbitraryDataMap() != null) {
                this.insertVariableFields(streamColumnFamily, rowKey, mutator, event.getArbitraryDataMap());
            }


            if (streamDef.getMetaData() != null) {
                prepareDataForInsertion(event.getMetaData(), streamDef.getMetaData(), DataType.meta, rowKey,
                                        streamColumnFamily, mutator);

            }
            //Iterate for correlation  data
            if (event.getCorrelationData() != null) {
                prepareDataForInsertion(event.getCorrelationData(), streamDef.getCorrelationData(),
                                        DataType.correlation,
                                        rowKey, streamColumnFamily, mutator);
            }

            //Iterate for payload data
            if (event.getPayloadData() != null) {
                prepareDataForInsertion(event.getPayloadData(), streamDef.getPayloadData(), DataType.payload,
                                        rowKey, streamColumnFamily, mutator);
            }

            rowKeyList.add(rowKey);

        }

        commit(mutator);

        endTimeMeasurement(IS_PERFORMANCE_MEASURED);

        return rowKeyList;

    }

    private void endTimeMeasurement(boolean isPerformanceMeasured) {
        if (isPerformanceMeasured) {
            if (eventCounter.get() > 100000) {
                synchronized (this) {
                    if (eventCounter.get() > 100000) {

                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();

                        long endTime = System.currentTimeMillis();
                        int currentBatchSize = eventCounter.getAndSet(0);
                        totalEventCounter.addAndGet(currentBatchSize);

                        String line = "[" + dateFormat.format(date) + "] # of events : " + currentBatchSize +
                                      " start timestamp : " + startTime +
                                      " end time stamp : " + endTime + " Throughput is (events / sec) : " +
                                      (currentBatchSize * 1000) / (endTime - startTime) + " Total Event Count : " +
                                      totalEventCounter + " \n";
                        File file = new File(CarbonUtils.getCarbonHome() + File.separator + "receiver-perf.txt");

                        try {
                            AppendUtils.appendToFile(IOUtils.toInputStream(line), file);
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }

                        startTime = 0;

                    }
                }
            }
        }

    }


    private void startTimeMeasurement(boolean isPerformanceMeasured) {
        if (isPerformanceMeasured) {
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
        }
    }

    private void addColumnDefinitionsToColumnFamily(List<Attribute> attributes, DataType dataType,
                                                    ColumnFamilyDefinition columnFamilyDefinition) {
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                BasicColumnDefinition columnDefinition = new BasicColumnDefinition();
                columnDefinition.setName(stringSerializer.toByteBuffer(
                        CassandraSDSUtils.getColumnName(dataType, attribute)));
                columnDefinition.setValidationClass(attributeComparatorMap.get(
                        attribute.getType()));

                try{
                columnFamilyDefinition.addColumnDefinition(columnDefinition);
                }catch (UnsupportedOperationException exception){
                   if(log.isDebugEnabled()){
                       log.debug("Cannot add the meta information to column family.",exception);
                   }
                }
            }
        }
    }

    private void addFilteredColumnDefinitionsToColumnFamily(
            List<Attribute> attributes, DataType dataType,
            List<ColumnDefinition> columnDefinitions,
            ColumnFamilyDefinition columnFamilyDefinition) {

        List<Attribute> filteredAttributes = new ArrayList<Attribute>();

        if (attributes != null) {
            filteredAttributes.addAll(attributes);

            Iterator<Attribute> attributeIterator = filteredAttributes.iterator();
            while (attributeIterator.hasNext()) {
                Attribute attribute = attributeIterator.next();

                boolean skipAddingMetaData = false; // If the column is already existing skip
                if (columnDefinitions != null) {
                    for (ColumnDefinition columnDefinition : columnDefinitions) {
                        String columnName = stringSerializer.
                                fromByteBuffer(columnDefinition.getName().asReadOnlyBuffer());
                        if (columnName.equals(CassandraSDSUtils.getColumnName(
                                dataType, attribute))) {
                            skipAddingMetaData = true;
                            break;
                        }
                    }

                    if (skipAddingMetaData) {
                        attributeIterator.remove();
                    }
                }
            }

            addColumnDefinitionsToColumnFamily(filteredAttributes, dataType,
                                               columnFamilyDefinition);
        }


    }

    private void addMetaColumnDefinitionsToColumnFamily(
            ColumnFamilyDefinition columnFamilyDefinition) {

        BasicColumnDefinition columnDefinition = new BasicColumnDefinition();

        columnDefinition.setName(stringSerializer.toByteBuffer(STREAM_ID_KEY));
        columnDefinition.setValidationClass(ComparatorType.UTF8TYPE.getClassName());
        columnFamilyDefinition.addColumnDefinition(columnDefinition);

        columnDefinition = new BasicColumnDefinition();
        columnDefinition.setName(stringSerializer.toByteBuffer(STREAM_NAME_KEY));
        columnDefinition.setValidationClass(ComparatorType.UTF8TYPE.getClassName());
        columnFamilyDefinition.addColumnDefinition(columnDefinition);

        columnDefinition = new BasicColumnDefinition();
        columnDefinition.setName(stringSerializer.toByteBuffer(STREAM_VERSION_KEY));
        columnDefinition.setValidationClass(ComparatorType.UTF8TYPE.getClassName());
        columnFamilyDefinition.addColumnDefinition(columnDefinition);

        columnDefinition = new BasicColumnDefinition();
        columnDefinition.setName(stringSerializer.toByteBuffer(STREAM_DESCRIPTION_KEY));
        columnDefinition.setValidationClass(ComparatorType.UTF8TYPE.getClassName());
        columnFamilyDefinition.addColumnDefinition(columnDefinition);

        columnDefinition = new BasicColumnDefinition();
        columnDefinition.setName(stringSerializer.toByteBuffer(STREAM_NICK_NAME_KEY));
        columnDefinition.setValidationClass(ComparatorType.UTF8TYPE.getClassName());
        columnFamilyDefinition.addColumnDefinition(columnDefinition);

        columnDefinition = new BasicColumnDefinition();
        columnDefinition.setName(stringSerializer.toByteBuffer(STREAM_TIMESTAMP_KEY));
        columnDefinition.setValidationClass(ComparatorType.LONGTYPE.getClassName());
        columnFamilyDefinition.addColumnDefinition(columnDefinition);

    }

    /**
     * Store event stream definition to Cassandra data store
     *
     * @param cluster Cluster of the tenant
     */
    public void saveStreamDefinitionToStore(Cluster cluster,
                                            StreamDefinition streamDefinition)
            throws StreamDefinitionStoreException {

        String CFName = CassandraSDSUtils.convertStreamNameToCFName(streamDefinition.getName());


        try {
            //todo move this to defineStream
            if (!CFCache.getCF(cluster, BAM_EVENT_DATA_KEYSPACE, CFName)) {
                createColumnFamily(cluster, BAM_EVENT_DATA_KEYSPACE, CFName, streamDefinition);
            }


            Keyspace keyspace = getKeyspace(BAM_META_KEYSPACE, cluster);
            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
            mutator.addInsertion(streamDefinition.getStreamId(), BAM_META_STREAM_DEF_CF,
                                 HFactory.createStringColumn(STREAM_DEF, EventDefinitionConverterUtils
                                         .convertToJson(streamDefinition)
                                 ));

            mutator.execute();

            log.info("Saving Stream Definition : " + streamDefinition);

            if (log.isDebugEnabled()) {
                String logMsg = "saveStreamDefinition executed. \n";

                Credentials credentials = getCredentials(cluster);
                StreamDefinition streamDefinitionFromStore =
                        getStreamDefinitionFromStore(credentials, streamDefinition.getStreamId());
                logMsg += " stream definition saved : " + streamDefinitionFromStore.toString() +
                          " \n";

                log.debug(logMsg);
            }

        } catch (ExecutionException e) {
            throw new StreamDefinitionStoreException("Error getting column family : " + CFName, e);
        }


    }

    public static Credentials getCredentials(Cluster cluster) {
        Map<String, String> credentials = cluster.getCredentials();

        Credentials creds = null;
        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            String userName = entry.getKey();
            String password = entry.getValue();
            String tenantDomain = MultitenantUtils.getTenantDomain(userName);

            creds = new Credentials(userName, password, tenantDomain);
        }

        return creds;
    }

    public boolean deleteStreamDefinitionFromStore(Cluster cluster, String streamId)
            throws StreamDefinitionStoreException {

        // delete entry from stream definitions
        Keyspace keyspace = getKeyspace(BAM_META_KEYSPACE, cluster);
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        mutator.delete(streamId, BAM_META_STREAM_DEF_CF, STREAM_DEF, stringSerializer);

        return true;
    }

    public boolean deleteStreamDefinitionFromCassandra(Cluster cluster, String streamId)
            throws StreamDefinitionStoreException {

        Credentials credentials = getCredentials(cluster);
        // clear data
        deleteDataFromStreamDefinition(credentials, cluster, streamId);

        // delete entry from stream definitions
        Keyspace keyspace = getKeyspace(BAM_META_KEYSPACE, cluster);
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        mutator.delete(streamId, BAM_META_STREAM_DEF_CF, STREAM_DEF, stringSerializer);

        return true;
    }

    private void deleteDataFromStreamDefinition(Credentials credentials, Cluster cluster,
                                                String streamId) {
        Keyspace keyspace = getKeyspace(BAM_EVENT_DATA_KEYSPACE, cluster);

        String CFName = CassandraSDSUtils.convertStreamNameToCFName(
                DataBridgeCommonsUtils.getStreamNameFromStreamId(streamId));

        String deleteVersion = DataBridgeCommonsUtils.getStreamVersionFromStreamId(streamId);

        int row_count = 1000;
        // get all stream ids
        RangeSlicesQuery<String, String, String> query =
                HFactory.createRangeSlicesQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);
        query.setColumnFamily(CFName).setColumnNames(STREAM_VERSION_KEY);

        String last_key = "";
        query.setRowCount(row_count);


        if (log.isDebugEnabled()) {
            log.debug("Deleting stream definition with id : " + streamId);
        }

        boolean isLastRow = false;

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        boolean anotherVersionFound = false;
        while (!isLastRow) {
            query.setKeys(last_key, "");
            QueryResult<OrderedRows<String, String, String>> result = query.execute();

            int iter = 0;
            for (Row<String, String, String> row : result.get()) {
                iter++;
                if (row == null) {
                    continue;
                }

                if (!last_key.equals("") && iter == 1) {
                    //since last iteration-last row, and this iteration first ro returns same row.
                    continue;
                }

                // this has already been deleted, and hence a tombstone, refer http://wiki.apache.org/cassandra/FAQ#range_ghosts
                HColumn<String, String> versionColumn = row.getColumnSlice().getColumnByName(STREAM_VERSION_KEY);
                if (versionColumn == null) {
                    continue;
                }

                String actualVersion = versionColumn.getValue();

                // delete row
                if (deleteVersion.equals(actualVersion)) {
                    mutator.addDeletion(row.getKey(), CFName);

                } else {
                    anotherVersionFound = true;
                }

                last_key = row.getKey();

            }

            // delete off for every 1000 rows
            mutator.execute();

            if (result.get().getCount() < row_count) {
                isLastRow = true;
            }
        }

        // This is the only existing version of this stream definition. So delete the column family
        // backing the stream definition as well with the deletion of this stream definition
        if (!anotherVersionFound) {
            cluster.dropColumnFamily(keyspace.getKeyspaceName(), CFName);
        }


    }

    /**
     * Retrun Stream Definition   stored in stream definition column family under key domainName-streamIdKey
     *
     * @param streamId Stream Id
     * @return Returns event stream definition stored in BAM meta data keyspace
     * @throws StreamDefinitionStoreException Thrown if the stream definitions are malformed
     */

    public StreamDefinition getStreamDefinitionFromStore(Credentials credentials, String streamId) {
        try {
            return StreamDefnCache.getStreamDefinition(credentials, streamId);
        } catch (ExecutionException e) {
            return null;
        }
    }

    public StreamDefinition getStreamDefinitionFromCassandra(
            Cluster cluster, String streamId) throws StreamDefinitionStoreException {
        Keyspace keyspace =
                getKeyspace(BAM_META_KEYSPACE, cluster);
        ColumnQuery<String, String, String> columnQuery =
                HFactory.createStringColumnQuery(keyspace);
        columnQuery.setColumnFamily(BAM_META_STREAM_DEF_CF)
                .setKey(streamId).setName(STREAM_DEF);
        QueryResult<HColumn<String, String>> result = columnQuery.execute();
        HColumn<String, String> hColumn = result.get();
        try {
            if (hColumn != null) {
                return EventDefinitionConverterUtils.convertFromJson(hColumn.getValue());
            }
        } catch (MalformedStreamDefinitionException e) {
            throw new StreamDefinitionStoreException(
                    "Retrieved definition from Cassandra store is malformed. Retrieved "
                    + "value : " + hColumn.getValue());
        }

        return null;
    }

    public void definedStream(Cluster cluster,
                              StreamDefinition streamDefinition) {
        String CFName = CassandraSDSUtils.convertStreamNameToCFName(streamDefinition.getName());

        ColumnFamilyDefinition cfDef = null;
        try {
            cfDef = getColumnFamily(cluster, BAM_EVENT_DATA_KEYSPACE, CFName);
            if (!CFCache.getCF(cluster, BAM_EVENT_DATA_KEYSPACE, CFName)) {
                if (cfDef == null) {
                    cfDef = createColumnFamily(cluster, BAM_EVENT_DATA_KEYSPACE, CFName,
                                               streamDefinition);
                    return;
                } else {
                    CFCache.putCF(cluster, BAM_EVENT_DATA_KEYSPACE, CFName, true);
                }
            }

            List<ColumnDefinition> columnDefinitions = cfDef.getColumnMetadata();

            int originalColumnDefinitionSize = columnDefinitions.size();

            addFilteredColumnDefinitionsToColumnFamily(streamDefinition.getPayloadData(),
                                                       DataType.payload, columnDefinitions, cfDef);
            addFilteredColumnDefinitionsToColumnFamily(streamDefinition.getMetaData(),
                                                       DataType.meta, columnDefinitions, cfDef);
            addFilteredColumnDefinitionsToColumnFamily(streamDefinition.getCorrelationData(),
                                                       DataType.correlation, columnDefinitions,
                                                       cfDef);

            int newColumnDefinitionSize = cfDef.getColumnMetadata().size();

            if (originalColumnDefinitionSize != newColumnDefinitionSize) {
                cluster.updateColumnFamily(cfDef, true);
            }

        } catch (ExecutionException e) {
            log.error("Error while getting column family definition from cache at defined stream."
                    , e);
        }


    }

    public void removeStream(Credentials credentials, Cluster cluster,
                             StreamDefinition streamDefinition) {

        // clear data
        deleteDataFromStreamDefinition(credentials, cluster, streamDefinition.getStreamId());

        // invalidate cache
        StreamDefnCache.invalidateStreamDefinition(credentials, streamDefinition.getStreamId());
    }

    private static class StreamDefnCache {

        private volatile static LoadingCache<StreamIdClusterBean, StreamDefinition> streamDefnCache = null;

        private static void init() {
            if (streamDefnCache != null) {
                return;
            }
            synchronized (StreamDefnCache.class) {
                if (streamDefnCache != null) {
                    return;
                }
                streamDefnCache = CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .expireAfterAccess(30, TimeUnit.MINUTES)
                        .build(new CacheLoader<StreamIdClusterBean, StreamDefinition>() {
                            @Override
                            public StreamDefinition load(StreamIdClusterBean streamIdClusterBean)
                                    throws Exception {

                                String sessionId = ServiceHolder.getDataBridgeReceiverService().
                                        login(streamIdClusterBean.getUserName(),
                                              streamIdClusterBean.getPassword());

                                StreamDefinition streamDefinition =
                                        ServiceHolder.getDataBridgeReceiverService().
                                                getStreamDefinition(
                                                        sessionId, DataBridgeCommonsUtils.getStreamNameFromStreamId(
                                                                streamIdClusterBean.getStreamId()),
                                                        DataBridgeCommonsUtils.getStreamVersionFromStreamId(
                                                                streamIdClusterBean.getStreamId()));

                                if (streamDefinition != null) {
                                    return streamDefinition;
                                }

/*                                Keyspace keyspace =
                                        getKeyspace(BAM_META_KEYSPACE, streamIdClusterBean.getCluster());
                                ColumnQuery<String, String, String> columnQuery =
                                        HFactory.createStringColumnQuery(keyspace);
                                columnQuery.setColumnFamily(BAM_META_STREAM_DEF_CF)
                                        .setKey(streamIdClusterBean.getStreamId()).setName(STREAM_DEF);
                                QueryResult<HColumn<String, String>> result = columnQuery.execute();
                                HColumn<String, String> hColumn = result.get();
                                try {
                                    if (hColumn != null) {
                                        return EventDefinitionConverterUtils.convertFromJson(hColumn.getValue());
                                    }
                                } catch (MalformedStreamDefinitionException e) {
                                    throw new StreamDefinitionStoreException(
                                            "Retrieved definition from Cassandra store is malformed. Retrieved "
                                            + "value : " + hColumn.getValue());
                                }*/

                                throw new NullValueException("No value found");
                            }
                        }
                        );
            }

        }

        public static StreamDefinition getStreamDefinition(Credentials credentials, String streamId)
                throws ExecutionException {
            init();
            return streamDefnCache.get(new StreamIdClusterBean(credentials, streamId));
        }

        public static void invalidateStreamDefinition(Credentials credentials, String streamId) {
            streamDefnCache.invalidate(new StreamIdClusterBean(credentials, streamId));
        }


        private static class StreamIdClusterBean {
            private String tenantDomain;
            private String streamId;
            private Credentials credentials;

            private StreamIdClusterBean(Credentials credentials, String streamId) {
                this.credentials = credentials;
                this.tenantDomain = credentials.getDomainName();
                this.streamId = streamId;
            }

            public String getUserName() {
                return credentials.getUsername();
            }

            public String getPassword() {
                return credentials.getPassword();
            }

            public String getStreamId() {
                return streamId;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                StreamIdClusterBean that = (StreamIdClusterBean) o;

                return tenantDomain.equals(that.tenantDomain) && streamId.equals(that.streamId);

            }

            @Override
            public int hashCode() {
                int result = tenantDomain.hashCode();
                result = 31 * result + streamId.hashCode();
                return result;
            }

        }
    }

    /**
     * Retrun all stream definitions stored under one domain
     *
     * @param cluster Tenant cluster
     * @return All stream definitions related to given tenant domain
     * @throws StreamDefinitionStoreException If the stream definitions are malformed
     */
    public Collection<StreamDefinition> getAllStreamDefinitionFromStore(Cluster cluster)
            throws StreamDefinitionStoreException {

        List<StreamDefinition> streamDefinitions = new ArrayList<StreamDefinition>();

        Keyspace keyspace = getKeyspace(BAM_META_KEYSPACE, cluster);
        int row_count = 100;
        // get all stream ids
        RangeSlicesQuery<String, String, String> query =
                HFactory.createRangeSlicesQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);
        query.setColumnFamily(BAM_META_STREAM_DEF_CF);
        String last_key = "";
        query.setColumnNames(STREAM_DEF);
        query.setRowCount(row_count);


        String logMsg = null;
        if (log.isDebugEnabled()) {
            logMsg = "getAllStreamDefinitions called : \n";
        }
        int count = 0;
        while (true) {
            query.setKeys(last_key, "");
            QueryResult<OrderedRows<String, String, String>> result = query.execute();

            int iter = 0;
            for (Row<String, String, String> row : result.get()) {
                iter++;
                if (row == null) {
                    continue;
                }

                if (!last_key.equals("") && iter == 1) {
                    //since last iteration-last row, and this iteration first ro returns same row.
                    continue;
                }
                count++;

                last_key = row.getKey();

                if (null != row.getColumnSlice().getColumnByName(STREAM_DEF)) {
                    String streamDefinitionString = row.getColumnSlice().getColumnByName(STREAM_DEF).getValue();

                    try {
                        StreamDefinition streamDefinition = EventDefinitionConverterUtils.convertFromJson(streamDefinitionString);
                        streamDefinitions.add(streamDefinition);

                    } catch (MalformedStreamDefinitionException e) {
                        log.error("Malformed StreamDefinition " + streamDefinitionString);
                    }
                }

            }

            if (result.get().getCount() < row_count) {
                break;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(logMsg);
            log.info("Stream Id returned from cassandra: " + count);
        }

        return streamDefinitions;
    }

    // Default access methods shared witloadh unit tests

    void insertVariableFields(String streamColumnFamily, String rowKey,
                              Mutator<String> mutator,
                              Map<String, String> customKeyValuePairs) {
        for (Map.Entry<String, String> stringStringEntry : customKeyValuePairs.entrySet()) {
            mutator.addInsertion(rowKey, streamColumnFamily,
                                 HFactory.createStringColumn(stringStringEntry.getKey(),
                                                             stringStringEntry.getValue()));
        }
    }

    Mutator prepareDataForInsertion(Object[] data, List<Attribute> streamDefnAttrList,
                                    DataType dataType,
                                    String rowKey, String streamColumnFamily,
                                    Mutator<String> mutator) {
        for (int i = 0; i < streamDefnAttrList.size(); i++) {
            Attribute attribute = streamDefnAttrList.get(i);
            TypeInserter typeInserter = inserterMap.get(attribute.getType());
            String columnName = CassandraSDSUtils.getColumnName(dataType, attribute);

            typeInserter.addDataToBatchInsertion(data[i], streamColumnFamily, columnName, rowKey, mutator);
        }
        return mutator;
    }

    Object getValueForDataTypeList(
            ColumnSlice<String, ByteBuffer> columnSlice, Attribute payloadDefinition,
            DataType dataType) throws IOException {
        HColumn<String, ByteBuffer> eventCol =
                columnSlice.getColumnByName(
                        CassandraSDSUtils.getColumnName(dataType, payloadDefinition));
        return CassandraSDSUtils
                .getOriginalValueFromColumnValue(eventCol.getValue(), payloadDefinition.getType());
    }

    Mutator<String> getMutator(Cluster cluster) throws StreamDefinitionStoreException {
        Keyspace keyspace = getKeyspace(BAM_EVENT_DATA_KEYSPACE, cluster);
        return HFactory.createMutator(keyspace, stringSerializer);
    }

    static Keyspace getKeyspace(String keyspace, Cluster cluster) {
        return HFactory.createKeyspace(keyspace, cluster, Utils.getGlobalConsistencyLevelPolicy());
    }

}



