package com.dynamic.zk.mq;

import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.concurrent.CountDownLatch;

/**
 * @author ssk www.8win.com Inc.All rights reserved
 * @version v1.0
 * @date 2018-09-28-下午 2:55
 */
@Log4j2
public class MqConsumersThread implements Runnable {

    private MQCustomConsumer consumer;

    private CountDownLatch latch;

    public MqConsumersThread(MQCustomConsumer consumer, CountDownLatch latch) {

        this.consumer = consumer;
        this.latch = latch;
    }

    @Override
    public void run() {

        try {
            log.info("异步执行 MQCustomConsumer 启动开始 ★★★ group【{}】,topic【{}】,tags【{}】",consumer.getConsumerGroup(),consumer.getTopic(),consumer.getTags());

            Thread.sleep(5000);

            this.consumer.start();
            log.info("异步执行 MQCustomConsumer 启动完成 ★★★ group【{}】,topic【{}】,tags【{}】",consumer.getConsumerGroup(),consumer.getTopic(),consumer.getTags());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MQClientException e) {
            log.error("异步执行 MQCustomConsumer 启动失败 ×××【{}】，consumerId【{}】", consumer.getConsumerGroup(), consumer.getConsumerId());
        } finally {
            latch.countDown();
        }
    }
}
