package com.st.cloud.gametool.tools;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author dev03
 */
@Data
public class GameVo {
    /**
     * 单注
     */
    private long bet;
    /**
     * 底注
     */
    private long score;
    /**
     * 携带
     */
    private AtomicLong carry;
    /**
     * 总赢
     */
    private AtomicLong win;
    /**
     * 总押注
     */
    private AtomicLong allBet;

    private long startTime = System.currentTimeMillis();

    private int runNum;
    private AtomicInteger count = new AtomicInteger(0);


    public boolean addCount() {
        return count.incrementAndGet() > runNum;
    }


}
