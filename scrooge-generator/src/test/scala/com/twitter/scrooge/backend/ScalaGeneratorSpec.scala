package com.twitter.scrooge.backend

import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.specs.mock.{ClassMocker, JMocker}
import org.specs.SpecificationWithJUnit
import thrift.test._
import thrift.test1._
import thrift.test2._
import thrift.`def`.default._
import com.twitter.scrooge.{ThriftStruct, ThriftException, EvalHelper}
import sun.tools.java.Constants
import com.twitter.finagle.SourcedException

class ScalaGeneratorSpec extends SpecificationWithJUnit with EvalHelper with JMocker with ClassMocker {
  val protocol = mock[TProtocol]

  def stringToBytes(string: String) = ByteBuffer.wrap(string.getBytes)

  "ScalaGenerator" should {
    "generate an enum" in {
      "correct constants" in {
        NumberId.One.value mustEqual 1
        NumberId.Two.value mustEqual 2
        NumberId.Three.value mustEqual 3
        NumberId.Five.value mustEqual 5
        NumberId.Six.value mustEqual 6
        NumberId.Eight.value mustEqual 8
      }

      "apply" in {
        NumberId(1) mustEqual NumberId.One
        NumberId(2) mustEqual NumberId.Two
        NumberId(3) mustEqual NumberId.Three
        NumberId(5) mustEqual NumberId.Five
        NumberId(6) mustEqual NumberId.Six
        NumberId(8) mustEqual NumberId.Eight
      }

      "get" in {
        NumberId.get(1) must beSome(NumberId.One)
        NumberId.get(2) must beSome(NumberId.Two)
        NumberId.get(3) must beSome(NumberId.Three)
        NumberId.get(5) must beSome(NumberId.Five)
        NumberId.get(6) must beSome(NumberId.Six)
        NumberId.get(8) must beSome(NumberId.Eight)
        NumberId.get(10) must beNone
      }

      "valueOf" in {
        NumberId.valueOf("One") must beSome(NumberId.One)
        NumberId.valueOf("Two") must beSome(NumberId.Two)
        NumberId.valueOf("Three") must beSome(NumberId.Three)
        NumberId.valueOf("Five") must beSome(NumberId.Five)
        NumberId.valueOf("Six") must beSome(NumberId.Six)
        NumberId.valueOf("Eight") must beSome(NumberId.Eight)
        NumberId.valueOf("Ten") must beNone
      }
    }

    "generate constants" in {
      thrift.test.Constants.myNumberID mustEqual NumberId.One
      thrift.test.Constants.name mustEqual "Columbo"
      thrift.test.Constants.someInt mustEqual 1
      thrift.test.Constants.someDouble mustEqual 3.0
      thrift.test.Constants.someList mustEqual List("piggy")
      thrift.test.Constants.emptyList mustEqual List()
      thrift.test.Constants.someMap mustEqual Map("foo" -> "bar")
    }

    "basic structs" in {
      "ints" in {
        "read" in {
          expect {
            startRead(protocol, new TField("baby", TType.I16, 1))
            one(protocol).readI16() willReturn (16: Short)
            nextRead(protocol, new TField("mama", TType.I32, 2))
            one(protocol).readI32() willReturn 32
            nextRead(protocol, new TField("papa", TType.I64, 3))
            one(protocol).readI64() willReturn 64L
            endRead(protocol)
          }

          Ints(protocol) mustEqual Ints(16, 32, 64L)
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("baby", TType.I16, 1))
            one(protocol).writeI16(16)
            nextWrite(protocol, new TField("mama", TType.I32, 2))
            one(protocol).writeI32(32)
            nextWrite(protocol, new TField("papa", TType.I64, 3))
            one(protocol).writeI64(64)
            endWrite(protocol)
          }

          Ints(16, 32, 64L).write(protocol) mustEqual ()
        }
      }

      "bytes" in {
        "read" in {
          expect {
            startRead(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).readByte() willReturn 3.toByte
            nextRead(protocol, new TField("y", TType.STRING, 2))
            one(protocol).readBinary() willReturn stringToBytes("hello")
            endRead(protocol)
          }

          val bytes = Bytes(protocol)
          bytes.x mustEqual 3.toByte
          new String(bytes.y.array) mustEqual "hello"
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).writeByte(16.toByte)
            nextWrite(protocol, new TField("y", TType.STRING, 2))
            one(protocol).writeBinary(stringToBytes("goodbye"))
            endWrite(protocol)
          }

          Bytes(16.toByte, stringToBytes("goodbye")).write(protocol) mustEqual ()
        }
      }

      "bool, double, string" in {
        "read" in {
          expect {
            startRead(protocol, new TField("alive", TType.BOOL, 1))
            one(protocol).readBool() willReturn true
            nextRead(protocol, new TField("pi", TType.DOUBLE, 2))
            one(protocol).readDouble() willReturn 3.14
            nextRead(protocol, new TField("name", TType.STRING, 3))
            one(protocol).readString() willReturn "bender"
            endRead(protocol)
          }

          Misc(protocol) mustEqual Misc(true, 3.14, "bender")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("alive", TType.BOOL, 1))
            one(protocol).writeBool(false)
            nextWrite(protocol, new TField("pi", TType.DOUBLE, 2))
            one(protocol).writeDouble(6.28)
            nextWrite(protocol, new TField("name", TType.STRING, 3))
            one(protocol).writeString("fry")
            endWrite(protocol)
          }

          Misc(false, 6.28, "fry").write(protocol) mustEqual ()
        }
      }

      "lists, sets, and maps" in {
        val exemplar = Compound(
          intlist = List(10, 20),
          intset = Set(44, 55),
          namemap = Map("wendy" -> 500),
          nested = List(Set(9)))

        "read" in {
          expect {
            startRead(protocol, new TField("intlist", TType.LIST, 1))
            one(protocol).readListBegin() willReturn new TList(TType.I32, 2)
            one(protocol).readI32() willReturn 10
            one(protocol).readI32() willReturn 20
            one(protocol).readListEnd()
            nextRead(protocol, new TField("intset", TType.SET, 2))
            one(protocol).readSetBegin() willReturn new TSet(TType.I32, 2)
            one(protocol).readI32() willReturn 44
            one(protocol).readI32() willReturn 55
            one(protocol).readSetEnd()
            nextRead(protocol, new TField("namemap", TType.MAP, 3))
            one(protocol).readMapBegin() willReturn new TMap(TType.STRING, TType.I32, 1)
            one(protocol).readString() willReturn "wendy"
            one(protocol).readI32() willReturn 500
            one(protocol).readMapEnd()
            nextRead(protocol, new TField("nested", TType.LIST, 4))
            one(protocol).readListBegin() willReturn new TList(TType.SET, 1)
            one(protocol).readSetBegin() willReturn new TSet(TType.I32, 1)
            one(protocol).readI32() willReturn 9
            one(protocol).readSetEnd()
            one(protocol).readListEnd()
            endRead(protocol)
          }

          Compound(protocol) mustEqual exemplar
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("intlist", TType.LIST, 1))
            one(protocol).writeListBegin(equal(new TList(TType.I32, 2)))
            one(protocol).writeI32(10)
            one(protocol).writeI32(20)
            one(protocol).writeListEnd()
            nextWrite(protocol, new TField("intset", TType.SET, 2))
            one(protocol).writeSetBegin(equal(new TSet(TType.I32, 2)))
            one(protocol).writeI32(44)
            one(protocol).writeI32(55)
            one(protocol).writeSetEnd()
            nextWrite(protocol, new TField("namemap", TType.MAP, 3))
            one(protocol).writeMapBegin(equal(new TMap(TType.STRING, TType.I32, 1)))
            one(protocol).writeString("wendy")
            one(protocol).writeI32(500)
            one(protocol).writeMapEnd()
            nextWrite(protocol, new TField("nested", TType.LIST, 4))
            one(protocol).writeListBegin(equal(new TList(TType.SET, 1)))
            one(protocol).writeSetBegin(equal(new TSet(TType.I32, 1)))
            one(protocol).writeI32(9)
            one(protocol).writeSetEnd()
            one(protocol).writeListEnd()
            endWrite(protocol)
          }

          exemplar.write(protocol) mustEqual ()
        }
      }
    }

    "complicated structs" in {
      "with required fields" in {
        "read" in {
          expect {
            startRead(protocol, new TField("string", TType.STRING, 1))
            one(protocol).readString() willReturn "yo"
            endRead(protocol)
          }

          RequiredString(protocol) mustEqual RequiredString("yo")
        }

        "missing required value throws exception during deserialization" in {
          doBefore {
            expect {
              emptyRead(protocol)
            }
          }

          "with no default value" in {
            RequiredString(protocol) must throwA[TProtocolException]
          }

          "with default value" in {
            RequiredStringWithDefault(protocol) must throwA[TProtocolException]
          }
        }

        "null required value throws exception during serialization" in {
          "with no default value" in {
            RequiredString(value = null).write(protocol) must throwA[TProtocolException]
          }

          "with default value" in {
            RequiredStringWithDefault(value = null).write(protocol) must throwA[TProtocolException]
          }
        }
      }

      "with optional fields" in {
        "read" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            nextRead(protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32() willReturn 14
            endRead(protocol)
          }

          OptionalInt(protocol) mustEqual OptionalInt("Commie", Some(14))
        }

        "read with missing field" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            endRead(protocol)
          }

          OptionalInt(protocol) mustEqual OptionalInt("Commie", None)
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            nextWrite(protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(14)
            endWrite(protocol)
          }

          OptionalInt("Commie", Some(14)).write(protocol) mustEqual ()
        }

        "write with missing field" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            endWrite(protocol)
          }

          OptionalInt("Commie", None).write(protocol) mustEqual ()
        }
      }

      "with default values" in {
        "read with value missing, using default" in {
          expect {
            one(protocol).readStructBegin()
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues(protocol) mustEqual DefaultValues("leela")
        }

        "read with value present" in {
          expect {
            one(protocol).readStructBegin()
            nextRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "delilah"
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues(protocol) mustEqual DefaultValues("delilah")
        }
      }

      "nested" in {
        "read" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "United States of America"
            nextRead(protocol, new TField("provinces", TType.LIST, 2))
            one(protocol).readListBegin() willReturn new TList(TType.STRING, 2)
            one(protocol).readString() willReturn "connecticut"
            one(protocol).readString() willReturn "california"
            one(protocol).readListEnd()
            nextRead(protocol, new TField("emperor", TType.STRUCT, 5))

            /** Start of Emperor struct **/
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Bush"
            nextRead(protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32() willReturn 42
            endRead(protocol)
            /** End of Emperor struct **/

            endRead(protocol)
          }

          Empire(protocol) mustEqual Empire(
            "United States of America",
            List("connecticut", "california"),
            Emperor("Bush", 42))
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Canada")
            nextWrite(protocol, new TField("provinces", TType.LIST, 2))
            one(protocol).writeListBegin(equal(new TList(TType.STRING, 2)))
            one(protocol).writeString("Manitoba")
            one(protocol).writeString("Alberta")
            one(protocol).writeListEnd()
            nextWrite(protocol, new TField("emperor", TType.STRUCT, 5))

            // emperor
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Larry")
            nextWrite(protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(13)
            endWrite(protocol)

            endWrite(protocol)
          }

          Empire(
            "Canada",
            List("Manitoba", "Alberta"),
            Emperor("Larry", 13)
          ).write(protocol) mustEqual ()
        }
      }

      "exception" in {
        Xception(1, "boom") must haveSuperClass[Exception]
        Xception(1, "boom") must haveSuperClass[ThriftException]
        Xception(1, "boom") must haveSuperClass[SourcedException]
        Xception(1, "boom") must haveSuperClass[ThriftStruct]
        Xception(2, "kathunk").getMessage mustEqual "kathunk"
      }

      "exception getMessage" in {
        StringMsgException(1, "jeah").getMessage mustEqual "jeah"
        NonStringMessageException(5).getMessage mustEqual "5"
      }

      "with more than 22 fields" in {
        "apply" in {
          Biggie().num25 mustEqual 25
        }

        "two default object must be equal" in {
          Biggie() mustEqual Biggie()
        }

        "copy and equals" in {
          Biggie().copy(num10 = -5) mustEqual Biggie(num10 = -5)
        }

        "hashCode is the same for two similar objects" in {
          Biggie().hashCode mustEqual Biggie().hashCode
          Biggie(num10 = -5).hashCode mustEqual Biggie(num10 = -5).hashCode
        }

        "hashCode is different for two different objects" in {
          Biggie(num10 = -5).hashCode mustNot beEqual(Biggie().hashCode)
        }

        "toString" in {
          Biggie().toString mustEqual ("Biggie(" + 1.to(25).map(_.toString).mkString(",") + ")")
        }
      }

      "unapply single field" in {
        val struct: Any = RequiredString("hello")
        struct match {
          case RequiredString(value) =>
            value mustEqual "hello"
        }
      }

      "unapply multiple fields" in {
        val struct: Any = OptionalInt("foo", Some(32))
        struct match {
          case OptionalInt(name, age) =>
            name mustEqual "foo"
            age must beSome(32)
        }
      }
    }

    "unions" in {
      "zero fields" in {
        "read" in {
          expect {
            emptyRead(protocol)
          }

          Bird(protocol) must throwA[TProtocolException]
        }

        "write" in {
          Bird.Raptor(null).write(protocol) must throwA[TProtocolException]
        }
      }

      "one field" in {
        "read" in {
          expect {
            startRead(protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString() willReturn "Ruby-Throated"
            endRead(protocol)
          }

          Bird(protocol) mustEqual Bird.Hummingbird("Ruby-Throated")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).writeString("foo")
            endWrite(protocol)
          }

          Bird.OwletNightjar("foo").write(protocol)
        }
      }

      "more than one field" in {
        "read" in {
          expect {
            startRead(protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString() willReturn "Anna's Hummingbird"
            nextRead(protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).readBinary() willReturn ByteBuffer.allocate(1)
            endRead(protocol)
          }

          Bird(protocol) must throwA[TProtocolException]
        }

        // no write test because it's not possible
      }

      "nested struct" in {
        "read" in {
          expect {
            startRead(protocol, new TField("raptor", TType.STRUCT, 1))
            startRead(protocol, new TField("isOwl", TType.BOOL, 1))
            one(protocol).readBool() willReturn false
            nextRead(protocol, new TField("species", TType.STRING, 2))
            one(protocol).readString() willReturn "peregrine"
            endRead(protocol)
            endRead(protocol)
          }

          Bird(protocol) mustEqual Bird.Raptor(Raptor(false, "peregrine"))
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("raptor", TType.STRUCT, 1))
            startWrite(protocol, new TField("isOwl", TType.BOOL, 1))
            one(protocol).writeBool(true)
            nextWrite(protocol, new TField("species", TType.STRING, 2))
            one(protocol).writeString("Tyto alba")
            endWrite(protocol)
            endWrite(protocol)
          }

          Bird.Raptor(Raptor(true, "Tyto alba")).write(protocol)
        }
      }

      "collection" in {
        "read" in {
          expect {
            startRead(protocol, new TField("flock", TType.LIST, 4))
            one(protocol).readListBegin() willReturn new TList(TType.STRING, 3)
            one(protocol).readString() willReturn "starling"
            one(protocol).readString() willReturn "kestrel"
            one(protocol).readString() willReturn "warbler"
            one(protocol).readListEnd()
            endRead(protocol)
          }

          Bird(protocol) mustEqual Bird.Flock(List("starling", "kestrel", "warbler"))
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("flock", TType.LIST, 4))
            one(protocol).writeListBegin(equal(new TList(TType.STRING, 3)))
            one(protocol).writeString("starling")
            one(protocol).writeString("kestrel")
            one(protocol).writeString("warbler")
            one(protocol).writeListEnd()
            endWrite(protocol)
          }

          Bird.Flock(List("starling", "kestrel", "warbler")).write(protocol)
        }
      }

      "default value" in {
        Bird.Hummingbird() mustEqual Bird.Hummingbird("Calypte anna")
      }
    }

    "typedef relative fields" in {
      val candy = Candy(100, CandyType.Delicious)
      candy.sweetnessIso mustEqual 100
      candy.candyType.value mustEqual 1
      candy.brand mustEqual "Hershey"
      candy.count mustEqual 10
      candy.headline mustEqual "Life is short, eat dessert first"
    }
  }
}
