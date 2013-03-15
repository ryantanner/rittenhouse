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

class RedisList[A](key: String)(implicit parse: Parse[A], client: RedisClient) extends RedisKey[A](key)(parse, client)
                                                                                  with LinearSeq[A] {

  /** Selects an element by its index in the immutable sequence.
    *
    * Uses [[http://redis.io/commands/lindex lindex]]
    */
  def apply(idx: Int): A = {
    if(idx < this.length)
      client.lindex[A](key, idx) match {
        case Some(elem) => elem
        case None => throw new RedisKeyDoesNotExistException(key, client.toString)
      }
    else
      throw new RedisListIndexOutOfBoundsException(key, idx, this.length)
  }
  
  /** Updates the n-th element of this list to a new value.
    *
    * Uses {{{{lset}}}}
    */
  def update(idx: Int, elem: A) = 
    try {
      client.lset(key, idx, elem)
    } catch {
      case e:Exception if (e.getMessage.equals("ERR index out of range")) =>
        throw new RedisListIndexOutOfBoundsException(key, idx, this.length)
      case e:Exception if (e.getMessage.equals("ERR no such key")) => 
        throw new RedisKeyDoesNotExistException(key, client.toString)
      case t:Throwable => throw t
    }

  /** Returns an iterator over all elements of this list
    *
    * Uses {{{{lrange}}}} to get all elements
    */
  override def iterator: Iterator[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => list.flatten.iterator
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  /** Returns the length of this list
    *
    * Uses {{{{llen}}}}
    */
  def length: Int = client.llen(key) match {
    case Some(len) => len.toInt
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  /** Is the list empty?
    *
    * Uses {{{{llen}}}}
    */
  override def isEmpty: Boolean = this.length == 0

  /** Appends a single element to this buffer
    *
    * Uses {{{{rpush}}}} and {{{{lrange}}}}
    */
  def +=(elem: A): RedisList[A] = this.append(elem)

  /** Appends one or more elements to this buffer
    *
    * Uses {{{{rpush}}}} and {{{{lrange}}}}
    */
  def append(elem: A, elems: A*): RedisList[A] = {
    val list = for {
      len <- client.rpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }
 
  /** Prepends a single element to this buffer
    *
    * Uses {{{{lpush}}}} and {{{{lrange}}}}
    */
  def +=:(elem: A): RedisList[A] = this.prepend(elem)

  /** Prepends one or more elements to this buffer
    *
    * Uses {{{{lpush}}}} and {{{{lrange}}}}
    */
  def prepend(elem: A, elems: A*): RedisList[A] = {
    val list = for {
      len <- client.lpush(key, elem, elems:_*)
      list <- client.lrange[A](key, 0, -1)
    } yield list
    this
  }

  /** Inserts all elements of a Traversable into this collection at the given index
    *
    * Uses {{{{lrange}}}}, {{{{del}}}} and {{{{rpush}}}}
    */
  def insertAll(n: Int, elems: Traversable[A]) = {
    /* Gets entire list from Redis, splits it at n and concatenates the splits 
     * with the given Traversable in the middle
     *
     * The original list is then deleted and the new elements pushed into the
     * same key using rpush
     */
    val newList = client.lrange[A](key, 0, -1).map(list =>
      list.take(n) ++ elems ++ list.takeRight(list.length - n)
    ).getOrElse(throw new RedisKeyDoesNotExistException(key, client.toString))

    client.del(key)

    client.rpush(key, newList.head, newList.tail:_*)
  }

  /** Returns the first element of this list
    * 
    * Uses {{{{lindex}}}}
    */ 
  def head: A = this.apply(0)

  def tail: RedisList[A] = client.lrange[A](key, 1, -1) 

  /** Pops the list
    * 
    * Uses {{{{lpop}}}}
    */
  def pop: A = client.lpop[A](key)

  /** Pushes an element onto this list; simply calls this.prepend
    * 
    * Uses {{{{lpush}}}}
    */
  def prepend(elem: A): RedisList[A] = this.prepend(elem)

  /** This operation is unsupported as removing a key for a list
    * still in operation in the runtime would lead to unsafe
    * behavior
    */
  def remove(n: Int): A = throw new UnsupportedOperationException

  /** Removes all elements from this list
    *
    * Uses {{{{del}}}}
    */
  def clear = client.del(key)

  /** Returns a Seq of this list which does not operate on a Redis list
    *
    * Uses {{{{lrange}}}}
    */
  override def seq: LinearSeq[A] = client.lrange[A](key, 0, -1) match {
    case Some(list) => LinearSeq.empty ++ list.flatten.toSeq 
    case None => throw new RedisKeyDoesNotExistException(key, client.toString)
  }

  /** Replaces the current list with the given Seq, returns self
    *
    * Uses {{{{del}}}} and {{{{rpush}}}}
    */
  def :=(seq: LinearSeq[A]): RedisList[A] = {
    client.del(key)
    seq.foreach(client.rpush(key, _))

    this
  }

  /** This method implements BLPOP for the exposed methods
    * in order to keep code cleaner
    */
  private def blpopImpl(duration: Duration): Future[A] = future {
    client.blpop[String, A](duration.toSeconds.toInt, key).map(_._2).getOrElse(throw new RedisKeyDoesNotExistException(key, client.toString))
  }

  /** Returns the head element of this list immediately or indefinitely blocks 
    * the RedisClient connection until another client pushes an element into
    * this list.  By returning a Future this implementation will not block client
    * code but no other operations on collections using this client will be
    * possible until an element is returned.
    *
    * Uses {{{{blpop}}}}
    *
    * Returns a Future with no timeout
    */
  def blpop: Future[A] = blpopImpl(0 millis)
 
  /** Returns the head element of this list immediately or blocks 
    * the RedisClient connection until another client pushes an element into
    * this list or the given duration elapses.  By returning a Future this 
    * implementation will not block client code but no other operations on collections 
    * using this client will be possible until an element is returned or the given
    * duration elapses.
    *
    * Uses {{{{blpop}}}}
    *
    * Returns a Future with Success if an element is available within the given
    * duration or a Failure if the timeout elapses
    */
  def blpop(duration: FiniteDuration): Future[A] = Future firstCompletedOf Seq(this.blpopImpl(duration), after(duration, using = Context.system.scheduler)(
    Future.failed(new IllegalStateException("BLPOP failed to receive value before timeout"))))

  /** An alternative to providing a FiniteDuration
    *
    * Uses {{{{blpop}}}}
    *
    * Returns a Future with the given deadline
    */
  def blpop(deadline: Deadline): Future[A] = blpop(deadline.time)

  /** Combines this list with other RedisLists of the same type into a Set
    *
    * Facilitates blocking operations on multiple lists
    *
    * Returns a Set of this and the given RedisList
    */
  def ~(o: RedisList[A]): Set[RedisList[A]] = Set(this, o)

}

object RedisList {

  private def blpopImpl[A](timeout: Duration, lists: Set[RedisList[A]]): Future[(RedisList[A], A)] = {
    require(checkClients(lists.map(_.client)))

    val client = lists.head.client
    implicit val parse: Parse[A] = lists.head.parse

    future {
      (for {
        (key, value)  <- client.blpop[String, A](timeout.toSeconds.toInt, lists.head.key, lists.tail.map(_.key).toSeq:_*)
        listOfKey     <- lists.find(_.key == key)
      } yield (listOfKey, value)) getOrElse (throw new IllegalStateException("Could not BLPOP"))
    } 
  }

  /** Perform a blocking pop operation on multiple RedisLists 
    * 
    * Uses {{{{blpop}}}}
    *
    * Returns the head element of the first list with an available element
    */
  def blpop[A](lists: Set[RedisList[A]]): Future[(RedisList[A], A)] = {
    blpopImpl[A](0 seconds, lists)
  }

  /** Perform a blocking pop operation on multiple RedisLists 
    * 
    * Uses {{{{blpop}}}}
    *
    * Returns the head element of the first list with an available element or
    * a Failure if the timeout elapses before an element is available
    */
  def blpop[A](timeout: FiniteDuration)(lists: Set[RedisList[A]]): Future[(RedisList[A], A)] = {
    blpopImpl[A](timeout, lists)
  }

  implicit class RedisListSetWrapper[A](set: Set[RedisList[A]]) {
    def ~(o: RedisList[A]): Set[RedisList[A]] = set + o
  }

  private def checkClients(clients: Set[RedisClient]): Boolean = clients.forall(_ == clients.head)

}
