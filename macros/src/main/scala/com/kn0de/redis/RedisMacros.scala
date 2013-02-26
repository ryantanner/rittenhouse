package com.kn0de.redis

import language.experimental.macros

import reflect.macros.Context

object RedisMacros  {

  def keyName(name: Any): String = macro keyName_impl

  def keyName_impl(c: Context)(name: c.Expr[Any]): c.Expr[String] = {
    import c.universe._
    val nameRep = show(name.tree)
    val nameRepTree = Literal(Constant(nameRep))
    val nameRepExpr = c.Expr[String](nameRepTree)
    reify { nameRepExpr.splice.toString }
  }

}
