Declarative Normalizer

The project is to enable you to annotate the fields in your POJO with the transfermation functions you want to apply on. The compiler will generate the code and do type checking for you.

There are a few projects have been done in java to provide annotative/declairtive validation frameworks. They are implemented using run-time reflection and dymanic typing. The casting excetion will blow your application up if you make mistake in annotations. In addition, Java Reflection calls are ten times slower than compiled function calls in performance. To overcome these shortcomings, I decide to implement this type of framework using complier-time code generation in Scala. Scala provides a feature "Macros". It enable the developer to write functions for the complier to execute. The Maros function will return Expression (AST). The complier then injects the expression in the call site to expand the call site code.


The example is in Test.scala.



2016Q2 Declarative Normalization

We hate to write boilerplate code. Why not to let the compiler generate those for us. In this project I am going to show how to use annotation and Scala Macros to dictate compiler in Scala.

 
Objectives:
Get rid of boilerplate code.
Problems:
In the core data team, we build a big array of normalizers over time. It takes in a raw listing object and apply these normalizers to clean up some fields in the object. Let's say we have the following Listing domain object.
case class Listing(
                  agentName: String,
                  agentEmail: String,
                  agentUrl: String,
                  price: Int,
                  agentUrlAuthority: String = null
					.
					.
					.
 )
 
Every normalizer implements Normalizer interface
trait Normalizer[T]{
  val normalize: T => T
}
In this case T is of type Listing.
It takes two steps to add a normalizer.
Step 1: Extends Normalizer interface. For example, implementing AgentNameNormalizer. 
class AgentNameNormalizer extends Normalizer[Listing]{
  override val normalize =
    (listing: Listing) => {
      val agentName = listing.agentName

      val newAgnentName = if (agentName == null) "None" else agentName

      listing.copy(agentName = newAgnentName)
  }
}
 
Step 2: Add the normalizer into CombineNormalizer. CombineNormalizer is the root/container of all the other normalizers.
final class CombineNormalizer extends Normalizer[Listing]{
  //Create a chain of normalizers by adding a instance of different type of normalizers into the list you want to apply.
  val normalizers = List(
    new AgentNameNormalizer
  )

  override val normalize =
    (listing: Listing) =>{
      //Call each normalizer in the chain to normalize the listing. 
	  //A returned listing from a normalizer becomes a input listing to the next normalizer.
	  //Like functions composition: f(x).g(x).k(x) = f(g(k(x)))
      normalizers.foldLeft(listing)((listing, normalizer) => normalizer.normalize(listing));
    }
}
 
//testing
def main(args: Array[String]){
	val normalization = new CombineNormalizer()
	
	val listing = _ //instantiate a Listing object with raw data
	
	val normalizedListing = normalization.normalize(listing)
}
 
Add another normalizer
Step 1: Extends Normalizer interface. For example: implementing AgentEmailNormalizer.
class AgentEmailNormalizer extends Normalizer[Listing]{
  override val normalize =
    (listing : Listing) => {
      val email = listing.agentEmail

      //use regular expression to validate email. Null out email if it's not valid.
      val validEmail = "\\S+@\\S+".r
      val newEmail = validEmail.findFirstIn(email) match {
        case Some(email) => email
        case None => null
      }

      listing.copy(agentEmail = newEmail)
  }
}
 
Step 2: Add the normalizer into CombineNormalizer.
class CombineNormalizer extends Normalizer[Listing]{
  //Create a chain of normalizers.
  val normalizers = List(
    new AgentNameNormalizer,
    new AgentEmailNormalizer
  )

  override val normalize =
    (listing: Listing) =>{
      //call each special normalizer in the chain to normalize the listing.
      normalizers.foldLeft(listing)((listing, normalizer) => normalizer.normalize(listing));
    }
}
 
In the RTP project, we need to reimplement dozens of these normalizers. It turns out a tone of boilerplate code I need to write.
What will be the solution to get rid of those chores?
Using the combination of Annotation, type class, Scala Macros, Quasiquotes, and compile-time reflection in Scala, we can dictate the Scala compiler to generate the code for us.
 
First, How if we have these three annotations: 
@Normalize(name, function) : Declare a normalizer. Parameter 'name' is the name of the normalizer. Parameter 'function' is the logic of the normalizer.
@NormalizeInput(name) : Annotate a field to declare this field as a input to the normalizer specified by the name parameter.
@NormalizeOutput(name) : Annotate a field to declare this field as a output to the normalizer specified by the name parameter.
 
Then, we can use these annotations to annotate on Listing class to add the two normalizers above, and one more normalizer which uses different fields as input and output:
@Normalize("AgentNameNormalizer", (name: String) => if (name == null) "None" else name) //Inline function definition
@Normalize("AgentEmailNormalizer", (email: String) => {val validEmail = "\\S+@\\S+".r; validEmail.findFirstIn(email).getOrElse(null)})
@Normalize("UrlAuthorityNormalizer", Utils.authorityFromURL) //pass function reference as parameter.
case class ListingAnnotated(
                    @NormalizeInput("AgentNameNormalizer")
                    @NormalizeOutput("AgentNameNormalizer")
                    agentName: String,

                    @NormalizeInput("AgentEmailNormalizer")
                    @NormalizeOutput("AgentEmailNormalizer")
                    agentEmail: String,

					@NormalizeInput("UrlAuthorityNormalizer")
                    agentUrl: String,
                    price: Int,
					
					@NormalizeOutput("UrlAuthorityNormalizer")
                    agentUrlAuthority: String = null
                  )
}
 
//Utils class contains static functions
object Utils{
	def authorityFromURL(url: String): String = {
  		if(url == null) {
    		return null;
	  	} else {
    			val urlPattern = "^(?=[^&])(?:(?<scheme>[^:/?#]+):)?(?://(?<authority>[^/?#]*))?(?<path>[^?#]*)(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?".r
    			url match {
      		//decompose a url
      		case urlPattern(scheme, authority, path, query, fragment) => authority
      		case _ => null
    	}
  	}
}
The Scala compiler will generate code similar to the code written for step 1 and 2. 
macro expansion is delayed: Normalizer.materializeNormalizer[T]
performing macro expansion Normalizer.materializeNormalizer[ListingAnnotated] at source-/Users/vincenty/gitHub/DeclarativeNormalization/core/src/Test.scala,line-85,offset=2845
{
  final class $anon extends Normalizer[ListingAnnotated] {
    def <init>() = {
      super.<init>();
      ()
    };
    override val normalize = ((obj: ListingAnnotated) => {
  val f: _root_.scala.Function1[String, String] = {
    ((url: String) => Utils.authorityFromURL(url))
  };
  obj.copy(agentUrlAuthority = f(obj.agentUrl))
}).compose(((obj: ListingAnnotated) => {
  val f: _root_.scala.Function1[String, String] = ((email: scala.this.Predef.String) => {
    val validEmail = scala.this.Predef.augmentString("\\S+@\\S+").r;
    validEmail.findFirstIn(email).getOrElse[String](null)
  });
  obj.copy(agentEmail = f(obj.agentEmail))
}).compose(((obj: ListingAnnotated) => {
  val f: _root_.scala.Function1[String, String] = ((name: scala.this.Predef.String) => if (name.==(null))
    "None"
  else
    name);
  obj.copy(agentName = f(obj.agentName))
}).compose(((obj: ListingAnnotated) => obj))))
  };
  new $anon()
}
 
Under the hook there are sixty lines of code :
 Expand source 
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
 
The complete project is here http://stash.sv2.trulia.com/projects/IN/repos/declarativenormalization/browse
These compile-time metaprograming features make them as a tool to not limit on doing boilerplate code generation, but also doing
•	Aspect-oriented programing for Instrumentation, software tracing, etc,. (https://github.com/adamw/scala-macro-aop)
•	lightweight dependency injection (https://github.com/adamw/macwire)
•	Objects mapping/binding. Macros is heavily used by Scalding, Spark for objects binding. 
 
 
 
