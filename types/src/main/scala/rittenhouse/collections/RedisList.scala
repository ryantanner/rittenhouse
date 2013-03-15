package rittenhouse.collections

import rittenhouse.exceptions._

import rittenhouse.Context

import scala.collection.mutable.LinearSeq
import scala.concurrent._
import duration._

import ExecutionContext.Implicits.global

import akka.pattern.after

import com.redis._
import serialization._

class RedisList[A](key: String)(implicit parse: Parse[A]) extends RedisKey[A](key)(parse)
                                                                                  with LinearSeq[A] {

  /** Selects an element by its index in the immutable sequence.
    *
    * Uses [[http://redis.io/commands/lindex lindex]]
    */
  def apply(idx: Int)(implicit client: RedisClient): A = {
    if(idx < this.length)
      client.lindex[A](key, idx) match {
        case Some(elem) => elem
        case None => throw new RedisKeyDoesNotExistException(key, client.toString)
      }
    else
      throw new java.lang.IndexOutOfBoundsException(s"List: $key, Index: $idx, Length: ${this.length}")
  }
  
  def update(idx: Int, elem: A)(implicit client: RedisClient) = 
    client.lset(key, idx, elem)

  override def iterator(implicit client: RedisClient): Iterator[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => list.flatten.iterator
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  def length(implicit client: RedisClient): Int = client.llen(key) match {
    case Some(len) => len.toInt
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  override def isEmpty(implicit client: RedisClient): Boolean = this.length == 0

  def +=(elem: A)(implicit client: RedisClient): RedisList[A] = this.append(elem)

  def append(elem: A, elems: A*)(implicit client: RedisClient): RedisList[A] = {
    val list = for {
      len <- client.rpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }
 
  def +=:(elem: A)(implicit client: RedisClient): RedisList[A] = this.prepend(elem)

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

  override def seq(implicit client: RedisClient): LinearSeq[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => LinearSeq.empty ++ list.flatten.toSeq 
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  def :=(seq: LinearSeq[A])(implicit client: RedisClient): RedisList[A] = {
    client.del(key)
    seq.foreach(client.rpush(key, _))

    this
  }

  private def blpopImpl(duration: Duration)(implicit client: RedisClient): Future[A] = future {
    client.blpop[String, A](duration.toSeconds.toInt, key).map(_._2).getOrElse(throw new RedisKeyDoesNotExistException(key, client.toString))
  }

  // Blocks indefinitely
  def blpop(implicit client: RedisClient): Future[A] = blpopImpl(0 millis)
 
  // Takes a duration blocking from now() for given duration
  def blpop(duration: FiniteDuration)(implicit client: RedisClient): Future[A] = Future firstCompletedOf Seq(this.blpopImpl(duration), after(duration, using = Context.system.scheduler)(
    Future.failed(new IllegalStateException("BLPOP failed to receive value before timeout"))))

  // Takes a deadline blocking until it is reached
  def blpop(deadline: Deadline)(implicit client: RedisClient): Future[A] = blpop(deadline.time)

  def ~(o: RedisList[A])(implicit client: RedisClient): Set[RedisList[A]] = Set(this, o)

}

object RedisList {

  private def blpopImpl[A](timeout: Duration, lists: Set[RedisList[A]])(implicit client: RedisClient): Future[(RedisList[A], A)] = {
    implicit val parse: Parse[A] = lists.head.parse

    future {
      (for {
        (key, value)  <- client.blpop[String, A](timeout.toSeconds.toInt, lists.head.key, lists.tail.map(_.key).toSeq:_*)
        listOfKey     <- lists.find(_.key == key)
      } yield (listOfKey, value)) getOrElse (throw new IllegalStateException("Could not BLPOP"))
    } 
  }

  def blpop[A](lists: Set[RedisList[A]])(implicit client: RedisClient): Future[(RedisList[A], A)] = {
    blpopImpl[A](0 seconds, lists)
  }

  def blpop[A](timeout: FiniteDuration)(lists: Set[RedisList[A]])(implicit client: RedisClient): Future[(RedisList[A], A)] = {
    blpopImpl[A](timeout, lists)
  }

  implicit class RedisListSetWrapper[A](set: Set[RedisList[A]]) {
    def ~(o: RedisList[A]): Set[RedisList[A]] = set + o
  }

}
