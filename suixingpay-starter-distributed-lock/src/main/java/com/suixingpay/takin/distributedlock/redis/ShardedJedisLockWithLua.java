/**  
 * All rights Reserved, Designed By Suixingpay.
 * @author: jiayu.qiu[qiu_jy@suixingpay.com] 
 * @date: 2017年7月19日 下午10:58:32   
 * @Copyright ©2017 Suixingpay. All rights reserved. 
 * 注意：本内容仅限于随行付支付有限公司内部传阅，禁止外泄以及用于其他的商业用途。
 */
package com.suixingpay.takin.distributedlock.redis;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * 基于ShardedJedisPool + Lua 实现分布式锁
 * 
 * @author: jiayu.qiu[qiu_jy@suixingpay.com]
 * @date: 2017年7月19日 下午10:58:32
 * @version: V1.0
 * @review: jiayu.qiu[qiu_jy@suixingpay.com]/2017年7月19日 下午10:58:32
 */
public class ShardedJedisLockWithLua extends AbstractRedisLockWithLua {

    private static final Logger logger = LoggerFactory.getLogger(ShardedJedisLockWithLua.class);

    private ShardedJedisPool shardedJedisPool;

    public ShardedJedisLockWithLua(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    private void returnResource(ShardedJedis shardedJedis) {
        shardedJedis.close();
    }

    @Override
    protected Long eval(byte[] lockScript, String key, List<byte[]> args) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            List<byte[]> keys = new ArrayList<byte[]>();
            keys.add(key.getBytes(CHAR_SET));
            return (Long) jedis.eval(lockScript, keys, args);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            returnResource(shardedJedis);
        }
        return 0L;
    }

    @Override
    protected void del(String key) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            jedis.del(key);
        } finally {
            returnResource(shardedJedis);
        }
    }

}
