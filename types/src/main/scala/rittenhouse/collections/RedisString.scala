package rittenhouse.collections

import akka.actor._

import rittenhouse.exceptions

import com.redis._
import serialization._

class RedisString[A](key: String)(implicit parse: Parse[A]) extends RedisKey[A](key)(parse) 
                                                                                   with StringLike[A] {

  def append(str: String)(implicit client: RedisClient): String[A] = {
    client.append(key, str)
    this
  }

  def :+(str: String)(implicit client: RedisClient): String[A] = this.append(str)

}
