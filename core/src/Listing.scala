import java.util.regex.Pattern

/**
  * Created by vincenty on 5/31/16.
  */
case class Listing(
                  agentName: String,
                  agentEmail: String,
                  agentUrl: String,
                  price: Int,
                  agentUrlAuthority: String = null
                  )


/**
  * Tranditional way to implement normalizer is to extend Normalizer inferface for each special normalizer logic
  */

class AgentNameNormalizer extends Normalizer[Listing]{
  override val normalize =
    (listing: Listing) => {
      val agentName = listing.agentName

      val newAgnentName = if (agentName == null) "None" else agentName

      listing.copy(agentName = newAgnentName)
  }
}

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

class PriceNormalizer extends Normalizer[Listing]{
  override val normalize =
    (listing: Listing) =>{
      val price = listing.price

      //cap the price by 10,000,000
      val newPrice = Math.min(Math.abs(price), 10000000)

      listing.copy(price = newPrice)
    }
}

class UrlAuthorityNormalizer extends Normalizer[Listing]{
  override val normalize =
    (listing: Listing) => {
      val agentUrl = listing.agentUrl

      val newAgentUrlAuthority = Utils.authorityFromURL(agentUrl)

      listing.copy(agentUrlAuthority = newAgentUrlAuthority)
    }
}

final class CombineNormalizer extends Normalizer[Listing]{
  //Create a chain of normalizers.
  val normalizers = List(
    new AgentNameNormalizer,
    new AgentEmailNormalizer,
    new PriceNormalizer,
    new UrlAuthorityNormalizer
  )

  override val normalize =
    (listing: Listing) =>{
      //call each special normalizer in the chain to normalize the listing.
      normalizers.foldLeft(listing)((listing, normalizer) => normalizer.normalize(listing));
    }
}


object CombineNormalizerTest extends App{
  val listing = Listing("agent A", "vincent@trulia.com", "http://abc.com", 100000000)
  val normalizer = new CombineNormalizer

  println(s"original listing: $listing")
  println(s"normalized listing: ${normalizer.normalize(listing)}")
}