package org.gokb.cred

class KBComponentVariantName {

  transient textNormalisationService

  KBComponent owner
  RefdataValue variantType
  RefdataValue locale
  RefdataValue status

  String variantName
  String normVariantName

  static mapping = {
        id column:'cvn_id'
        version column:'cvn_version'
        owner column:'cvn_kbc_fk'
        variantName column:'cvn_variant_name'
        normVariantName column:'cvn_norm_variant_name'
        variantType column:'cvn_type_rv_fk'
        locale column:'cvn_locale_rv_fk'
        status column:'cvn_status_rv_fk'
  }

  static constraints = {
        variantName (nullable:false, blank:false, maxSize:2048)
        normVariantName  (nullable:false, blank:false, maxSize:2048)
        variantType (nullable:true, blank:false)
        locale (nullable:true, blank:false)
        owner (nullable:false, blank:false)
        status (nullable:true, blank:false)
  }

  protected def generateNormname () {
    normVariantName = textNormalisationService.normalise(variantName);
  }

  def beforeInsert() {
    // Generate the any necessary values.
    generateNormname()
  }

  def beforeUpdate() {
    generateNormname()
  }


}
