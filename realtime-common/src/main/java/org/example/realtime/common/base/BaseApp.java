package org.example.realtime.common.base;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.example.realtime.common.util.FlinkSourceUtil;


/**
 * @Package org.example.realtime.common.base.BaseApp
 * @Author song.lihao
 * @Date 2025/4/8 21:30
 * @description:
 */
public abstract class BaseApp {
    public void start(int port, int parallelism, String ckAndGroupId, String topic) throws Exception {
        Configuration conf = new Configuration();
        conf.setInteger("rest.port",port);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        env.setParallelism(parallelism);
        env.enableCheckpointing(5000L,CheckpointingMode.EXACTLY_ONCE);
        KafkaSource<String> kafkaSource = FlinkSourceUtil.getKafkaSource(topic, ckAndGroupId);
        DataStreamSource<String> kafkaStrDS
                = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka_Source");
        handle(env,kafkaStrDS);
        env.execute();
    }
    public abstract void handle(StreamExecutionEnvironment env, DataStreamSource<String> kafkaStrDS);
}
