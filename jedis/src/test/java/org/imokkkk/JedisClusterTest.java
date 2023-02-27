package org.imokkkk;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author liuwy
 * @date 2023/2/27 22:02
 * @since 1.0
 */
public class JedisClusterTest {

    @Test
    public void testCluster() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(5);

        Set<HostAndPort> clusterNode = new HashSet<>();
        clusterNode.add(new HostAndPort("192.168.1.246", 8001));
        clusterNode.add(new HostAndPort("192.168.1.246", 8002));
        clusterNode.add(new HostAndPort("192.168.1.246", 8003));
        clusterNode.add(new HostAndPort("192.168.1.246", 8004));
        clusterNode.add(new HostAndPort("192.168.1.246", 8005));
        clusterNode.add(new HostAndPort("192.168.1.246", 8006));
        JedisCluster jedisCluster = null;
        try {
            jedisCluster = new JedisCluster(clusterNode, 6000, 5000, 10, "547717253", config);
            jedisCluster.set("k1", "v1");
            System.out.println(jedisCluster.get("k1"));
        } finally {
            if (jedisCluster != null) {
                jedisCluster.close();
            }
        }
    }
}
