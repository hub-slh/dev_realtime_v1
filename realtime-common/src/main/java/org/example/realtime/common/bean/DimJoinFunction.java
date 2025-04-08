package org.example.realtime.common.bean;

import com.alibaba.fastjson.JSONObject;

/**
 * @Package org.example.realtime.common.bean.DimJoinFunction
 * @Author song.lihao
 * @Date 2025/4/8 22:28
 * @description:
 */
public interface DimJoinFunction<T> {
    void addDims(T obj, JSONObject dimJsonObj) ;

    String getTableName() ;

    String getRowKey(T obj) ;
}
