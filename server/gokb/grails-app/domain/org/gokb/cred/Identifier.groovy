package org.gokb.cred

class Identifier extends KBComponent {

  IdentifierNamespace namespace
  String value
  KBComponent component
  
//  static belongsTo = [
//	component	: KBComponent
//  ]
  
//  static hasByCombo = [
//	component	: KBComponent
//  ]

  static constraints = {
  }

  static mapping = {
    namespace column:'id_namespace_fk', index:'id_namespace_fk_idx'
    value column:'id_value', index:'id_value_idx'
	component column:'id_component_fk', index:'id_component_fk_idx'
  }

  static def lookupOrCreateCanonicalIdentifier(ns, value) {
    // log.debug("lookupOrCreateCanonicalIdentifier(${ns},${value})");
    def namespace = IdentifierNamespace.findByNs(ns) ?: new IdentifierNamespace(ns:ns).save(failOnError:true);
    Identifier.findByNsAndValue(namespace,value) ?: new Identifier(ns:namespace, value:value).save(failOnError:true);
  }

}
