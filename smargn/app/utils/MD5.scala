package utils

/*
 * Contributors:
 *  - Valentin Rutz
 */
/**
 * From Valentin with love on 13/05/15.
 */
object MD5 {
  def hash(s: String) = {
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = s.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }
}
