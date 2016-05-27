
import scala.annotation.StaticAnnotation
import scala.annotation.meta.field
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

/**
  * Created by vincentye on 5/18/16.
  */
@field
case class NormalizeInput(normalizeName: String) extends StaticAnnotation
@field
case class NormalizeOutput(normalizeName: String) extends StaticAnnotation
@field
case class Normalize[I, R](name: String, transformation: Function1[I, R])


trait Normalizer[T]{
  val normalize: T => T
}


object Normalizer{
  implicit def materializeNormalizer[T]: Normalizer[T] = macro materializeNormalizerImpl[T]

  def materializeNormalizerImpl[T: c.WeakTypeTag](c: Context): c.Expr[Normalizer[T]] = {
    import c.universe._
    val tpe: c.universe.Type = weakTypeOf[T]

    val normalizeAns = tpe.typeSymbol.annotations.map( _.tree match {
      case q"new $annotation[..$typeParams]( ..$args) " if annotation.tpe <:< typeOf[Normalize[_,_]] => {
        val (normalizeName :: function :: rest) = args
        normalizeName.toString -> function
      }
    }).toMap


    val getInputFields =
//      tpe.decls.filter(!_.annotations.isEmpty).flatMap(decl => decl.annotations.map((_, decl)))
//        .filter(_._1.tree.tpe =:= typeOf[NormalizeInput]).map(kv => kv._1)
      for {
        decl <- tpe.decls if !decl.annotations.isEmpty
        annotation <- decl.annotations
        input <- annotation.tree match {
          case q"new $annotation[..$typeParams]( ..$normalizeName ) " if annotation.tpe =:= typeOf[NormalizeInput] =>
            Some(normalizeName.head.toString(), decl)
          case _ => None
        }
      } yield input



    val getOutputFields =
      (for {
        decl <- tpe.decls if !decl.annotations.isEmpty
        annotation <- decl.annotations
        output <- annotation.tree match {
          case q"new $annotation[..$typeParams]( ..$normalizeName ) " if annotation.tpe =:= typeOf[NormalizeOutput] =>
            Some(normalizeName.head.toString(), decl)
          case _ => None
        }
      } yield output).toMap

    val functionCallsSym =
      for {
        input <- getInputFields
        inputName = input._2.name.toString.trim : TermName
        function <- normalizeAns.get(input._1)
        output <- getOutputFields.get(input._1)
        outputName = output.name.decodedName.toString.trim : TermName
      } yield
        q"""

            (obj: $tpe) => {
              val f:(${input._2.typeSignature}) => ${output.typeSignature} = $function
              obj.copy($outputName = f(obj.$inputName))
            }
         """


    c.Expr[Normalizer[T]]{
      val tree = functionCallsSym.foldLeft(q"(obj: $tpe) => obj" : Tree)((a : Tree , b : Tree) => q"$b.compose($a)" )
      val exp = q"""
        new Normalizer[$tpe]{
          override val normalize = $tree
        }
      """
      c.untypecheck(exp)

    }
  }

}

