package rittenhouse.collections

import collection.immutable.StringLike

import akka.actor._

import rittenhouse.exceptions

import com.redis._
import serialization._

class RedisString[A](key: String)(implicit parse: Parse[A], client: RedisClient) extends RedisKey[A](key)(parse, client) 
                                                               with StringLike[A] {

  def append(str: String): StringLike[A] = {
    client.append(key, str)
    this
  }

  def :+(str: String): StringLike[A] = this.append(str)

  def seq: IndexedSeq[Char] = throw new UnsupportedOperationException

  protected[this] def newBuilder = throw new UnsupportedOperationException

}
