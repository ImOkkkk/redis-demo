package org.imokkkk;

import cn.hutool.core.collection.CollUtil;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Tuple;

/**
 * @author ImOkkkk
 * @date 2022/5/30 20:05
 * @since 1.0
 */
public class DelayQueueTest {
  private static final Logger log = LoggerFactory.getLogger(DelayQueueTest.class);
  private final int threadNum = 10;
  private JedisPool jedisPool;
  private CountDownLatch countDownLatch = new CountDownLatch(10);

  public DelayQueueTest() {}

  @BeforeEach
  public void init() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(50);
    this.jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 5000, "547717253");
  }

  @AfterEach
  public void closeJedis() {
    if (this.jedisPool != null) {
      this.jedisPool.close();
    }
  }

  @Test
  public void test() {
    for (int i = 0; i < threadNum; ++i) {
      (new Thread(new DelayQueueTest.DelayMessage())).start();
      this.countDownLatch.countDown();
    }
    this.produce();
  }

  private void produce() {
    Jedis jedis = this.jedisPool.getResource();

    for (int i = 0; i < 5; ++i) {
      String orderId = "OID0000001" + i;
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.SECOND, 3);
      int second3later = (int) (calendar.getTimeInMillis() / 1000L);
      jedis.zadd("orderId", (double) second3later, orderId);
      log.info("redis生成了一个订单任务：订单ID为{}", orderId);
      Thread currentThread = Thread.currentThread();
      if (currentThread.isInterrupted()) {
        break;
      }
      try {
        Thread.sleep(2000L);
      } catch (InterruptedException e) {
        e.printStackTrace();
        currentThread.interrupt();
      }
    }
  }

  private void consumer() {
    Jedis jedis = this.jedisPool.getResource();
    while (true) {
      while (true) {
        Set<Tuple> items = jedis.zrangeWithScores("orderId", 0L, 1L);
        if (CollUtil.isEmpty(items)) {
          log.info("当前没有等待的任务");
          Thread currentThread = Thread.currentThread();
          if (currentThread.isInterrupted()) {
            return;
          }
          try {
            Thread.sleep(500L);
          } catch (InterruptedException e) {
            e.printStackTrace();
            currentThread.interrupt();
          }
        } else {
          int score = (int) ((Tuple) items.toArray()[0]).getScore();
          Calendar calendar = Calendar.getInstance();
          int now = (int) (calendar.getTimeInMillis() / 1000L);
          if (now >= score) {
            String orderId = ((Tuple) items.toArray()[0]).getElement();
            Long num = jedis.zrem("orderId", new String[] {orderId});
            if (num != null && num > 0L) {
              log.info("redis消费了一个任务：消费的订单OrderId为{}", orderId);
            }
          }
        }
      }
    }
  }

  class DelayMessage implements Runnable {
    public void run() {
      Thread currentThread = Thread.currentThread();
      try {
        if (currentThread.isInterrupted()) {
          return;
        }
        DelayQueueTest.this.countDownLatch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
        currentThread.interrupt();
      }
      consumer();
    }
  }
}
