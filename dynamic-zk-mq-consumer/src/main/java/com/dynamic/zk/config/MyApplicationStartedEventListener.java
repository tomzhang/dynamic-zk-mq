package com.dynamic.zk.config;

import com.dynamic.zk.listener.ChildrenCacheListener;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * @author ssk www.8win.com Inc.All rights reserved
 * @version v1.0
 * @date 2018-09-29-上午 11:03
 */
@Log4j2
@Configuration
public class MyApplicationStartedEventListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private CuratorProperties curatorProperties;

    private String baseZkMqPath;

    private PathChildrenCache pathChildrenCache;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {

        SpringApplication app = event.getSpringApplication();

        this.baseZkMqPath = curatorProperties.getBasePath();

        log.info("==MyApplicationStartedEventListener==");

        if (checkNodeExist()) {
            createZkNode();
        }

        this.initZkNodeCache();

        this.addListener();
    }

    private void createZkNode() {

        try {
            curatorFramework.create().withMode(CreateMode.PERSISTENT).forPath(baseZkMqPath, baseZkMqPath.getBytes());
        } catch (Exception e) {
            log.error("创建zk 节点失败{}",e.getMessage());
        }

    }

    private boolean checkNodeExist() {

        Stat stat = null;
        try {
            stat = curatorFramework.checkExists().forPath(baseZkMqPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stat == null ? Boolean.TRUE : Boolean.FALSE;
    }

    private void initZkNodeCache() {

        log.info("初始化zk 节点缓存");
        //建立一个PathChildrenCache缓存,第三个参数为是否接受节点数据内容 如果为false则不接受
        pathChildrenCache = new PathChildrenCache(curatorFramework, curatorProperties.getBasePath(), true);
        // 在初始化的时候就进行缓存监听
        try {
            pathChildrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        } catch (Exception e) {
            log.error("初始化进行缓存监听失败");
            e.printStackTrace();
        }
    }

    private void addListener() {

        pathChildrenCache.getListenable().addListener(new ChildrenCacheListener(publisher));
    }
}
