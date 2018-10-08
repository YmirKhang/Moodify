package moodify.service

import com.redis.{RedisClientPool, Seconds}
import moodify.Config

/**
  * Redis service to provide an interface/abstraction for Redis accesses.
  */
object RedisService extends Config {

  /**
    * Pool connection for Redis instance.
    */
  private val rcp = new RedisClientPool(REDIS_HOST, REDIS_PORT)

  /**
    * Gets the value for the specified key.
    *
    * @param key Redis key.
    * @return Value of given key.
    */
  def get(key: String): Option[String] = {
    rcp.withClient {
      client =>
        client.get(key)
    }
  }

  /**
    * Sets the key with the specified value.
    *
    * @param key   Redis key.
    * @param value Value to be set.
    * @param ttl   Time to live for created key in seconds.
    * @return Success.
    */
  def set(key: String, value: String, ttl: Int): Boolean = {
    rcp.withClient {
      client =>
        client.set(key, value, onlyIfExists = false, time = Seconds(ttl))
    }
  }

  /**
    * Sets the expire time (in sec.) for the specified key.
    *
    * @param key Redis key.
    * @param ttl Time to live for given key in seconds.
    * @return Success.
    */
  def expire(key: String, ttl: Int): Boolean = {
    rcp.withClient {
      client =>
        client.expire(key, ttl)
    }
  }

  /**
    * Returns all fields and values of the hash stored at key.
    *
    * @param key Redis key.
    * @return Key value pairs of given key.
    */
  def hgetall(key: String): Option[Map[String, String]] = {
    rcp.withClient {
      client =>
        client.hgetall1(key)
    }
  }

  /**
    * Sets the specified fields to their respective values in the hash stored at key.
    *
    * @param key   Redis key.
    * @param value Key value pairs to be set.
    * @param ttl   Time to live for created key in seconds.
    * @return Success.
    */
  def hmset(key: String, value: Map[String, String], ttl: Int): Boolean = {
    rcp.withClient {
      client =>
        client.hmset(key, value)
        client.expire(key, ttl)
    }
  }

}
