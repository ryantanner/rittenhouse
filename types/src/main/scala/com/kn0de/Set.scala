/*
package com.kn0de

import com.redis._

import scala.collection.SetLike
import scala.collection.immutable.{ Set => ImmutableSet }
import scala.collection.generic._

import com.kn0de.redis.RedisMacros

class RedisSet[A] extends ImmutableSet[A]
             with GenericSetTemplate[A, RedisSet] 
             with SetLike[A, RedisSet[A]]  {

  def _setName = RedisMacros.keyName(this)

  override def contains(key: A): Boolean = false

  override def iterator: Iterator[A] = Iterator.empty
    var that: Set[A] = this
    def hasNext = that.nonEmpty
    def next: A = 
      if (hasNext) {
        var res = that.head
        that = that.tail
        res
      }
      else Iterator.empty.next()
  }

  override def +(elem: A): RedisSet[A] = RedisSet.empty[A]

  override def -(elem: A): RedisSet[A] = RedisSet.empty[A]

}

object RedisSet extends ImmutableSetFactory[Set]  {

  override def empty[A] = new RedisSet[A]()

}
*/
