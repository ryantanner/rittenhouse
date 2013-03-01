package com.kn0de

import com.redis._
import serialization._

import scala.collection.mutable.{ Seq, GrowingBuilder, ListBuffer }
import scala.collection.generic.{ SeqFactory, CanBuildFrom, Growable }
import scala.collection.mutable.{ ArrayBuffer, Builder, BufferLike }

import com.kn0de.redis.RedisMacros

class List[A <: AnyVal](implicit parse: Parse[A]) extends BufferLike[A, List[A]]  {

  private val _name = RedisMacros.keyName(this)

  def apply(idx: Int)(implicit client: RedisClient): A = client.lindex[A](_name, idx) match {
    case Some(elem) => elem
    case None => throw new RedisKeyDoesNotExistException(_name, client.toString)
  }
  
  def update(idx: Int, elem: A)(implicit client: RedisClient): Boolean = 
    client.lset(_name, idx, elem)

  def iterator(implicit client: RedisClient): Iterator[A] = client.lrange[A](_name, 0, -1) match {
    case Some(list) => list.flatten.iterator
    case None => throw new RedisKeyDoesNotExistException(_name, client.toString)
  }

  def length(implicit client: RedisClient): Int = client.llen(_name) match {
    case Some(len) => len.toInt
    case None => throw new RedisKeyDoesNotExistException(_name, client.toString)
  }

  def +=(elem: A)(implicit client: RedisClient): List[A] = this.append(elem)(client)

  def append(elem: A, elems: A*)(implicit client: RedisClient): List[A] = {
    val list = for {
      len <- client.rpush(_name, elem, elems:_*)
      list <- client.lrange[A](_name, 0, -1)
    } yield list
    this
  }

  def +=:(elem: A)(implicit client: RedisClient): List[A] = this.prepend(elem)(client)

  def prepend(elem: A, elems: A*)(implicit client: RedisClient): List[A] = {
    val list = for {
      len <- client.lpush(_name, elem, elems:_*)
      list <- client.lrange[A](_name, 0, -1)
    } yield list
    this
  }

  def insertAll(n: Int, elems: Traversable[A])(implicit client: RedisClient) = {
    val newList = client.lrange[A](_name, 0, -1).map(list =>
      list.take(n) ++ elems ++ list.takeRight(list.length - n)
    ).getOrElse(throw new RedisKeyDoesNotExistException(_name, client.toString))

    client.del(_name)

    client.rpush(_name, newList.head, newList.tail:_*)
  }

  def remove(n: Int)(implicit client: RedisClient): A = throw new UnsupportedOperationException

  def clear(implicit client: RedisClient) = client.del(_name)

  def seq(implicit client: RedisClient): Seq[A] = client.lrange[A](_name, 0, -1) match {
    case Some(list) => Seq.empty ++ list.flatten.toSeq 
    case None => throw new RedisKeyDoesNotExistException(_name, client.toString)
  }


}

/*
object List extends SeqFactory[List]  {

  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, List[A]] = ReusableCBF.asInstanceOf[GenericCanBuildFrom[A]]

  def newBuilder[A]: Builder[A, List[A]] = new ArrayBuffer

}
*/

case class RedisKeyDoesNotExistException(keyName: String, dbInfo: String) extends Exception   {

  override def getMessage = s"Redis key '$keyName' does not exist in database at $dbInfo"

}
