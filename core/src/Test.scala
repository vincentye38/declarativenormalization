import scala.util.matching.Regex

/**
  * Created by vincenty on 5/19/16.
  */

@Normalize("normalizeAge" , (age: Int) => Math.abs(age) )
@Normalize("inferKid", (i: Int) => if (i < 18) true else false )
@Normalize("firstName", Utils.regex("^([a-z]+[,.]?[ ]?|[a-z]+['-]?)+$".r, 1))
case class Person(

                 @NormalizeInput("firstName")
                 name: String,

                 @NormalizeInput("normalizeAge")
                 @NormalizeOutput("normalizeAge")
                 @NormalizeInput("inferKid")
                 age: Int,

                 @NormalizeOutput("inferKid")
                 isKid: Boolean = false,

                 @NormalizeOutput("firstName")
                 firstName: String = ""

                 )

@Normalize("AgentNameNormalizer", (name: String) => if (name == null) "None" else name)
@Normalize("AgentEmailNormalizer", (email: String) => {val validEmail = "\\S+@\\S+".r; validEmail.findFirstIn(email).getOrElse(null)})
@Normalize("UrlAuthorityNormalizer", Utils.authorityFromURL)
@Normalize("PriceNormalizer", (price: Int) => Math.min(Math.abs(price), 10000000))
case class ListingAnnotated(
                    @NormalizeInput("AgentNameNormalizer")
                    @NormalizeOutput("AgentNameNormalizer")
                    agentName: String,

                    @NormalizeInput("AgentEmailNormalizer")
                    @NormalizeOutput("AgentEmailNormalizer")
                    agentEmail: String,

                    @NormalizeInput("UrlAuthorityNormalizer")
                    agentUrl: String,

                    @NormalizeInput("PriceNormalizer")
                    @NormalizeOutput("PriceNormalizer")
                    price: Int,

                    @NormalizeOutput("UrlAuthorityNormalizer")
                    agentUrlAuthority: String = null
                  )


object Utils {
  def prefix(prefix: String)(i: String) = prefix + i

  def authorityFromURL(url: String): String = {
    if(url == null) {
      return null;
    } else {
      val urlPattern = "^(?=[^&])(?:(?<scheme>[^:/?#]+):)?(?://(?<authority>[^/?#]*))?(?<path>[^?#]*)(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?".r
      url match {
        //strip out 'http://'
        case urlPattern(scheme, authority, path, query, fragment) => authority
        case _ => null
      }
    }
  }

  def regex(pattern: Regex, group : Int): (String) => String = {
    def ret(i: String): String = {
      pattern.findAllIn(i).group(group)
    }
    ret _
  }
}

object Test extends App{


    val com = ((i: Int) => i + 1) compose ((i: Int) => i + 2)
    def normalize[T: Normalizer](t: T): T = implicitly[Normalizer[T]].normalize(t)

  val listing = ListingAnnotated("agent A", "vincent@trulia.com", "http://abc.com", 100000000)
    println(s"original  : $listing")
    println(s"normalized: ${normalize(listing)}")
}

