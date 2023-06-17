package pencil
package data

object NameType {
  opaque type Name = String

  object Name:
    def apply(name: String): Name = name

}
