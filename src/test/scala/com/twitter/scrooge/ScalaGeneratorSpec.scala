package com.twitter.scrooge

import org.specs.Specification
import scala.collection.JavaConversions._

class ScalaGeneratorSpec extends Specification {
  import AST._
  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"
  val header =
"""/**
 * Autogenerated by Scrooge
 * Edit this shit, I dare you
 */
package awwYeah"""

  "ScalaGenerator" should {
    "generate an enum" in {
      gen.enumTemplate(Enum("SomeEnum", List(EnumValue("FOO", 1), EnumValue("BAR", 2)).toArray), gen) mustEqual
header + """

import org.apache.thrift.TEnum

object SomeEnum {
  case object FOO extends SomeEnum(1)
  case object BAR extends SomeEnum(2)

  def apply(value: Int): Option[SomeEnum] = {
    value match {
      case 1 => Some(FOO)
      case 2 => Some(BAR)
      case _ => None
    }
  }
}

abstract class SomeEnum(val value: Int){
  def toThrift = new TEnum {
    override def getValue = {
      value
    }
  }
}"""
    }
  }
}