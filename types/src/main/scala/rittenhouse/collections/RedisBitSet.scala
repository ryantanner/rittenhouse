package rittenhouse.collections

import akka.actor._

import rittenhouse.exceptions

import com.redis._
import serialization._

/*
class RedisBitSet[A](key: String)(implicit parse: Parse[A]) extends RedisString[A](key)(parse)
                                                               with BitSet  {

  def bitcount(start: Int, end: Int)(implicit client RedisClient): Int {
    client.bitcount(key, Some((start, end))) getOrElse 0
  }

  def bitcount(implicit client: RedisClient): Int {
    client.bitcount(key, None) getOrElse 0
  }
}
*/
