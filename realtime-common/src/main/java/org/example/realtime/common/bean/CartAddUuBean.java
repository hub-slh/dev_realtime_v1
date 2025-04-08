package org.example.realtime.common.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Package org.example.realtime.common.bean.CartAddUuBean
 * @Author song.lihao
 * @Date 2025/4/8 22:29
 * @description: \
 */
@Data
@AllArgsConstructor
public class CartAddUuBean {
    // 窗口起始时间
    String stt;
    // 窗口闭合时间
    String edt;
    // 当天日期
    String curDate;
    // 加购独立用户数
    Long cartAddUuCt;
}
