package {{package}}

import org.apache.thrift.TEnum

{{docstring}}
object {{EnumName}} {
{{#values}}
  case object {{name}} extends {{EnumName}}({{value}}, "{{name}}")
{{/values}}

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * @throws NoSuchElementException if the value is not found.
   */
  def apply(value: Int): {{EnumName}} = {
    value match {
{{#values}}
      case {{value}} => {{name}}
{{/values}}
      case _ => throw new NoSuchElementException(value.toString)
    }
  }

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * Returns None if the value is not found
   */
  def get(value: Int): Option[{{EnumName}}] = {
    value match {
{{#values}}
      case {{value}} => scala.Some({{name}})
{{/values}}
      case _ => scala.None
    }
  }

  def valueOf(name: String): Option[{{EnumName}}] = {
    name.toLowerCase match {
{{#values}}
      case "{{nameLowerCase}}" => scala.Some({{EnumName}}.{{name}})
{{/values}}
      case _ => scala.None
    }
  }
}

{{docstring}}
abstract class {{EnumName}}(val value: Int, val name: String) extends TEnum {
  def getValue = value
}

