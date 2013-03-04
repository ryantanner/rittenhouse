package rittenhouse.collections

import rittenhouse.exceptions._

import scala.collection.mutable.LinearSeq

import com.redis._
import serialization._

class RedisList[A](key: String)(implicit parse: Parse[A], client: RedisClient) extends RedisKey[A](key)(parse, client)
                                                                                  with LinearSeq[A] {

  def apply(idx: Int): A = {
    if(idx < this.length)
      client.lindex[A](key, idx) match {
        case Some(elem) => elem
        case None => throw new RedisKeyDoesNotExistException(key, client.toString)
      }
    else
      throw new java.lang.IndexOutOfBoundsException(s"List: $key, Index: $idx, Length: ${this.length}")
  }
  
  def update(idx: Int, elem: A) = 
    client.lset(key, idx, elem)

  override def iterator: Iterator[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => list.flatten.iterator
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  def length: Int = client.llen(key) match {
    case Some(len) => len.toInt
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  override def isEmpty: Boolean = this.length == 0

  def +=(elem: A): RedisList[A] = this.append(elem)

  def append(elem: A, elems: A*): RedisList[A] = {
    val list = for {
      len <- client.rpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }
 
  def +=:(elem: A): RedisList[A] = this.prepend(elem)

  def prepend(elem: A, elems: A*): RedisList[A] = {
    val list = for {
      len <- client.lpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }

  def insertAll(n: Int, elems: Traversable[A]) = {
    val newList = client.lrange[A](key, 0, -1).map(list =>
      list.take(n) ++ elems ++ list.takeRight(list.length - n)
    ).getOrElse(throw new RedisKeyDoesNotExistException(key, client.toString))

    client.del(key)

    client.rpush(key, newList.head, newList.tail:_*)
  }

  def remove(n: Int): A = throw new UnsupportedOperationException

  def clear = client.del(key)

  override def seq: LinearSeq[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => LinearSeq.empty ++ list.flatten.toSeq 
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  def :=(seq: LinearSeq[A]): RedisList[A] = {
    client.del(key)
    seq.foreach(client.rpush(key, _))

    this
  }

}


