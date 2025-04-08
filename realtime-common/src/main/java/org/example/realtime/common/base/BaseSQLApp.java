package org.example.realtime.common.base;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.example.realtime.common.constant.Constant;
import org.example.realtime.common.util.SQLUtil;

/**
 * @Package org.example.realtime.common.base.BaseSQLApp
 * @Author song.lihao
 * @Date 2025/4/8 21:49
 * @description:
 */
public abstract class BaseSQLApp {
    public void start(int port, int parallelism,String ck){
    Configuration conf = new Configuration();
    conf.set(RestOptions.PORT,port);
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
    //1.2 设置并行度
        env.setParallelism(parallelism);
    //1.3 指定表执行环境
    StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
    //TODO 2.检查点相关的设置
        env.enableCheckpointing(5000L, CheckpointingMode.EXACTLY_ONCE);
        env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.days(30),Time.seconds(3)));
    handle(tableEnv);
}

    public abstract void handle(StreamTableEnvironment tableEnv) ;

    //从topic_db主题中读取数据，创建动态表
    public void readOdsDb(StreamTableEnvironment tableEnv,String groupId) {
        tableEnv.executeSql("CREATE TABLE topic_db (\n" +
                "  `database` string,\n" +
                "  `table` string,\n" +
                "  `type` string,\n" +
                "  `ts` bigint,\n" +
                "  `data` MAP<string, string>,\n" +
                "  `old` MAP<string, string>,\n" +
                "  pt as proctime(),\n" +
                "  et as to_timestamp_ltz(ts, 0), " +
                "  watermark for et as et - interval '3' second " +
                ") " + SQLUtil.getKafkaDDL(Constant.TOPIC_DB,groupId));
        //tableEnv.executeSql("select * from topic_db").print();
    }

    //从Hbase的字典表中读取数据，创建动态表
    public void readBaseDic(StreamTableEnvironment tableEnv) {
        tableEnv.executeSql("CREATE TABLE base_dic (\n" +
                " dic_code string,\n" +
                " info ROW<dic_name string>,\n" +
                " PRIMARY KEY (dic_code) NOT ENFORCED\n" +
                ") " + SQLUtil.getHBaseDDL("dim_base_dic"));
        //tableEnv.executeSql("select * from base_dic").print();
    }
}
