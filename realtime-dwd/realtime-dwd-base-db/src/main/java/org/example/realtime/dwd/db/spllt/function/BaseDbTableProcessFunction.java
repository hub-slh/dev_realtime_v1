package org.example.realtime.dwd.db.spllt.function;

import com.alibaba.fastjson.JSONObject;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.example.realtime.common.bean.TableProcessDwd;
import org.example.realtime.common.util.JdbcUtil;

import java.sql.Connection;
import java.util.*;

import static org.example.realtime.common.util.RedisUtil.getKey;

/**
 * @Package org.example.realtime.dwd.db.spllt.function.BaseDbTableProcessFunction
 * @Author song.lihao
 * @Date 2025/4/8 22:51
 * @description:
 */
public class BaseDbTableProcessFunction extends BroadcastProcessFunction<JSONObject, TableProcessDwd, Tuple2<JSONObject, TableProcessDwd>> {
    private MapStateDescriptor<String, TableProcessDwd> mapStateDescriptor;

    private Map<String, TableProcessDwd> configMap = new HashMap<>();
    public BaseDbTableProcessFunction(MapStateDescriptor<String, TableProcessDwd> mapStateDescriptor) {
        this.mapStateDescriptor = mapStateDescriptor;
    }
    @Override
    public void open(Configuration parameters) throws Exception {
        //将配置信息预加载到程序中
        Connection mySQLConnection = JdbcUtil.getMySQLConnection();
        List<TableProcessDwd> tableProcessDwdList
                = JdbcUtil.queryList(mySQLConnection, "select * from gmall2024_config.table_process_dwd", TableProcessDwd.class, true);
        for (TableProcessDwd tableProcessDwd : tableProcessDwdList) {
            String sourceTable = tableProcessDwd.getSourceTable();
            String sourceType = tableProcessDwd.getSourceType();
            String key = getKey(sourceTable, sourceType);
            configMap.put(key, tableProcessDwd);
        }
        JdbcUtil.closeMySQLConnection(mySQLConnection);
    }
    private String getKey(String sourceTable, String sourceType) {
        String key = sourceTable + ":" + sourceType;
        return key;
    }
    @Override
    public void processElement(JSONObject jsonObj, BroadcastProcessFunction<JSONObject, TableProcessDwd, Tuple2<JSONObject, TableProcessDwd>>.ReadOnlyContext ctx, Collector<Tuple2<JSONObject, TableProcessDwd>> out) throws Exception {
        String table = jsonObj.getString("table");
        //获取操作类型
        String type = jsonObj.getString("type");
        //拼接key
        String key = getKey(table, type);
        //获取广播状态
        ReadOnlyBroadcastState<String, TableProcessDwd> broadcastState = ctx.getBroadcastState(mapStateDescriptor);
        //根据key到广播状态以及configMap中获取对应的配置信息
        TableProcessDwd tp = null;

        if((tp = broadcastState.get(key)) != null
                ||(tp = configMap.get(key)) != null){
            //说明当前数据，是需要动态分流处理的事实表数据，将data部分传递到下游
            JSONObject dataJsonObj = jsonObj.getJSONObject("data");
            //在向下游传递数据前，过滤掉不需要传递的字段
            String sinkColumns = tp.getSinkColumns();
            deleteNotNeedColumns(dataJsonObj,sinkColumns);
            //在向下游传递数据前， 将ts事件时间补充到data对象上
            Long ts = jsonObj.getLong("ts");
            dataJsonObj.put("ts",ts);
            out.collect(Tuple2.of(dataJsonObj,tp));
        }
    }

    @Override
    public void processBroadcastElement(TableProcessDwd tp, BroadcastProcessFunction<JSONObject, TableProcessDwd, Tuple2<JSONObject, TableProcessDwd>>.Context ctx, Collector<Tuple2<JSONObject, TableProcessDwd>> out) throws Exception {
        String op = tp.getOp();
        //获取广播状态
        BroadcastState<String, TableProcessDwd> broadcastState = ctx.getBroadcastState(mapStateDescriptor);
        //获取业务数据库的表的表名
        String sourceTable = tp.getSourceTable();
        //获取业务数据库的表对应的操作类型
        String sourceType = tp.getSourceType();
        //拼接key
        String key = getKey(sourceTable, sourceType);

        if("d".equals(op)){
            //从配置表中删除了一条数据，那么需要将广播状态以及configMap中对应的配置也删除掉
            broadcastState.remove(key);
            configMap.remove(key);
        }else{
            //从配置表中读取数据或者添加、更新了数据  需要将最新的这条配置信息放到广播状态以及configMap中
            broadcastState.put(key,tp);
            configMap.put(key,tp);
        }
    }
    private static void deleteNotNeedColumns(JSONObject dataJsonObj, String sinkColumns) {
        List<String> columnList = Arrays.asList(sinkColumns.split(","));

        Set<Map.Entry<String, Object>> entrySet = dataJsonObj.entrySet();

        entrySet.removeIf(entry-> !columnList.contains(entry.getKey()));

    }
}
