package com.kn0de

import com.redis._
import serialization._

import com.kn0de.redis.RedisMacros

abstract class RedisData[A](val key: String)(implicit val parse: Parse[A]) {

}

class RedisList[A](key: String)(implicit parse: Parse[A]) extends RedisData[A](key)(parse) {

  def apply(idx: Int)(implicit client: RedisClient): A = client.lindex[A](key, idx) match {
    case Some(elem) => elem
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }
  
  def update(idx: Int, elem: A)(implicit client: RedisClient): Boolean = 
    client.lset(key, idx, elem)

  def iterator(implicit client: RedisClient): Iterator[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => list.flatten.iterator
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  def length(implicit client: RedisClient): Int = client.llen(key) match {
    case Some(len) => len.toInt
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  def +=(elem: A)(implicit client: RedisClient): RedisList[A] = this.append(elem)(client)

  def append(elem: A, elems: A*)(implicit client: RedisClient): RedisList[A] = {
    val list = for {
      len <- client.rpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }

  def +=:(elem: A)(implicit client: RedisClient): RedisList[A] = this.prepend(elem)(client)

  def prepend(elem: A, elems: A*)(implicit client: RedisClient): RedisList[A] = {
    val list = for {
      len <- client.lpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }

  def insertAll(n: Int, elems: Traversable[A])(implicit client: RedisClient) = {
    val newList = client.lrange[A](key, 0, -1).map(list =>
      list.take(n) ++ elems ++ list.takeRight(list.length - n)
    ).getOrElse(throw new RedisKeyDoesNotExistException(key, client.toString))

    client.del(key)

    client.rpush(key, newList.head, newList.tail:_*)
  }

  def remove(n: Int)(implicit client: RedisClient): A = throw new UnsupportedOperationException

  def clear(implicit client: RedisClient) = client.del(key)

  def seq(implicit client: RedisClient): Seq[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => Seq.empty ++ list.flatten.toSeq 
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

}

case class RedisKeyDoesNotExistException(keyName: String, dbInfo: String) extends Exception   {

  override def getMessage = s"Redis key '$keyName' does not exist in database at $dbInfo"

}
