package com.dynamic.zk.listener;

import com.dynamic.zk.event.ZkNodeEvent;
import com.dynamic.zk.mq.MQCustomConsumer;
import com.dynamic.zk.mq.MQFactory;
import com.dynamic.zk.mq.MqConsumersThread;
import com.dynamic.zk.vo.Subscribe;
import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * @author ssk www.8win.com Inc.All rights reserved
 * @version v1.0
 * @date 2018-09-28-下午 4:42
 */
@Component
@Log4j2
public class ZkPublisherListener {

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private MQCustomConcurrentlyListener mqCustomConcurrentlyListener;

    @Autowired
    private MQCustomeOrderlyListener mqCustomeOrderlyListener;

    @Value("${dynamic.rocketmq.namesrvAddr}")
    private String namesrvAddr;


    private static Map<String/*consumerId*/, String/*updateTime*/> updateMQSubscribeMap = Maps.newHashMap();

    @Async
    @EventListener(condition = "#event.typeName=='CHILD_ADDED' && #event.subscribe.status=='0'")
    public void start(ZkNodeEvent event) {

        Subscribe subscribe = event.getSubscribe();

        String consumerId = subscribe.getProname() + "_" + subscribe.getId();
        String updateTime = DateFormatUtils.format(subscribe.getUpdateTime(), "yyyy-MM-dd HH:mm:ss") + "_" + subscribe.getId();
        String groupName = subscribe.getGroupname();
        String topic = subscribe.getTopic();
        String tag = subscribe.getTag();
        // ★★★★ 配置的信息为非顺序消费的时候，使用顺序消费生产者进行发送消息会有问题
        MessageListener listener = subscribe.isOrderly() ? mqCustomeOrderlyListener : mqCustomConcurrentlyListener;

        MQCustomConsumer consumer = MQFactory.getInstance()
                .createConsumer(
                        consumerId, groupName, namesrvAddr, topic, tag, listener, this.setOptions(subscribe));


        CountDownLatch latch = new CountDownLatch(1);

        try {
            /** 异步执行启动consumer.start(); */
            executorService.execute(new MqConsumersThread(consumer, latch));

            latch.await();
            updateMQSubscribeMap.put(consumerId, updateTime);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.error("监听启动事件");
    }

    @Async
    @EventListener(condition = "#event.typeName=='CHILD_UPDATED' && #event.subscribe.status=='1'")
    public void restart(ZkNodeEvent event) {

        Subscribe subscribe = event.getSubscribe();

        String consumerId = subscribe.getProname() + "_" + subscribe.getId();
        String updateTime = DateFormatUtils.format(subscribe.getUpdateTime(), "yyyy-MM-dd HH:mm:ss") + "_" + subscribe.getId();
        MQFactory.getInstance().stopConsumer(consumerId);
        String groupName = subscribe.getGroupname();
        String topic = subscribe.getTopic();
        String tag = subscribe.getTag();
        MessageListener listener = subscribe.isOrderly() ? mqCustomeOrderlyListener : mqCustomConcurrentlyListener;

        MQCustomConsumer consumer = MQFactory.getInstance().createConsumer(
                consumerId, groupName, namesrvAddr, topic, tag, listener, this.setOptions(subscribe));


        CountDownLatch latch = new CountDownLatch(1);

        try {

            updateMQSubscribeMap.remove(consumerId);

            executorService.execute(new MqConsumersThread(consumer, latch));
            latch.await();

            updateMQSubscribeMap.put(consumerId, updateTime);
            log.error("RocketMq 重启完成");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.error("监听重启事件");
    }

    @Async
    @EventListener(condition = "#event.typeName=='CHILD_UPDATED' && #event.subscribe.status=='2'")
    public void stop(ZkNodeEvent event) {

        log.error("监听停止事件");

        Subscribe subscribe = event.getSubscribe();

        String key = subscribe.getProname() + "_" + subscribe.getId();

        String consumerId = DateFormatUtils.format(subscribe.getUpdateTime(), "yyyy-MM-dd HH:mm:ss") + "_" + subscribe.getId();

        MQFactory.getInstance().stopConsumer(consumerId);

        updateMQSubscribeMap.remove(key);

        log.error("RocketMq 已经停止完成");
    }

    @Async
    @EventListener(condition = "#event.typeName=='CHILD_REMOVED'")
    public void destroy(ZkNodeEvent event) {

        Subscribe subscribe = event.getSubscribe();

        String consumerId = subscribe.getProname() + "_" + subscribe.getId();

        String updatetime = DateFormatUtils.format(subscribe.getUpdateTime(), "yyyy-MM-dd HH:mm:ss") + "_" + subscribe.getId();

        updateMQSubscribeMap.remove(consumerId);

        MQFactory.getInstance().stopConsumer(consumerId);

        log.error("监听销毁事件");
    }


    private Map<String, String> setOptions(Subscribe subscribe) {
        //设置消费者其它参数
        /**Consumer 启动后，默认从什么位置开始消费:默认CONSUME_FROM_LAST_OFFSET*/
        Map<String, String> options = Maps.newHashMap();
        //设置消费者其它参数
        /**Consumer 启动后，默认从什么位置开始消费:默认CONSUME_FROM_LAST_OFFSET*/
        options.put("consumeFromWhere", subscribe.getConsumeformwhere());
        /**消费线程池最小数量:默认10*/
        options.put("consumeThreadMin", subscribe.getConsumethreadmin());
        /**消费线程池最大数量:默认20*/
        options.put("consumeThreadMax", subscribe.getConsumethreadmax());
        /**拉消息本地队列缓存消息最大数：默认1000*/
        options.put("pullThresholdForQueue", subscribe.getPullthresholdforqueue());
        /**批量消费，一次消费多少条消息:默认1条*/
        options.put("consumeMessageBatchMaxSize", subscribe.getConsumemessagebatchmaxsize());
        /**批量拉消息，一次最多拉多少条,默认32条*/
        options.put("pullBatchSize", subscribe.getPullbatchsize());
        /**消息拉取线程每隔多久拉取一次,默认为0*/
        options.put("pullInterval", subscribe.getPullinterval());
        return options;
    }
}
