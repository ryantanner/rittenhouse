package com.kn0de

import com.redis._

import scala.collection.SetLike
import scala.collection.immutable.{ Set => ImmutableSet }
import scala.collection.generic._

import java.util.AbstractSet

import com.kn0de.redis.RedisMacros

class Set[A] extends AbstractSet[A]
             with ImmutableSet[A]
             with GenericSetTemplate[A, Set] 
             with SetLike[A, Set[A]]  {

  def _setName = RedisMacros.keyName(this)

  override def contains(key: A): Boolean = false

  override def iterator: Iterator[A] = Iterator.empty

  override def +(elem: A): com.kn0de.Set[A] = Set.empty[A]

  override def -(elem: A): com.kn0de.Set[A] = Set.empty[A]

}

object Set extends ImmutableSetFactory[Set]  {

  override def empty[A] = new Set[A]()

}
