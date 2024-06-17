package com.xzll.common.util.msgId;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/**
 * @Author: hzz
 * @Date: 2024/6/16 08:46:17
 * @Description: 基于雪花算法保证消息绝对唯一【数据中心暂时是集群名，workId 根据配置的策略来生成】，
 * 融入会话类型和uid使其信息【更丰富】
 * 增加本地序列号 保证 消息id 【严格自增】
 */
public class MsgIdUtilsService {

    // 定义 Snowflake 算法的参数
    private final long twepoch = 1288834974657L; // 自定义的时间戳起点
    private final long workerIdBits = 5L;       // 机器 ID 所占的位数
    private final long datacenterIdBits = 5L;   // 数据中心 ID 所占的位数
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long sequenceBits = 12L;      // 序列所占的位数
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private final AtomicLong sequence = new AtomicLong(0);
    private final long workerId;
    private final long datacenterId;
    private long lastTimestamp = -1L;

    // 本地递增序列，用于确保严格递增
    private final AtomicLong localSequence = new AtomicLong(0);

    public MsgIdUtilsService(Long workerId, String datacenterIdStr) {
        //workId
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        this.workerId = workerId;
        //根据集群名 生成数据中心id
        long datacenterId = generateDatacenterId(datacenterIdStr);
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.datacenterId = datacenterId;
    }

    //集群名称转为数字
    private long generateDatacenterId(String groupName) {
        CRC32 crc = new CRC32();
        crc.update(groupName.getBytes(StandardCharsets.UTF_8));
        return crc.getValue() % (maxDatacenterId + 1);
    }


    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (lastTimestamp == timestamp) {
            long seq = (sequence.incrementAndGet()) & sequenceMask;
            if (seq == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence.set(0);
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence.get();
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public String generateMessageId(long userId, boolean isGroupChat) {
        long id = nextId();
        long localSeq = localSequence.incrementAndGet(); // 获取本地递增序列
        Integer type = isGroupChat ? 2 : 1;
        return String.format("%d-%d-%d-%d", type, userId, id, localSeq);
    }


    public static void main(String[] args) {
        MsgIdUtilsService generator = new MsgIdUtilsService(null, "xzll-im");
        for (int i = 0; i < 10; i++) {
            System.out.println(generator.generateMessageId(12345, false));
        }
    }
}
