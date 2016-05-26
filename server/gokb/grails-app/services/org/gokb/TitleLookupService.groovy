package org.gokb

import org.gokb.cred.*

import com.k_int.ClassUtils

class TitleLookupService {

  def grailsApplication
  def componentLookupService

  @javax.annotation.PostConstruct
  def init() {
    log.debug("Init");
  }

  //  def find(title, issn, eissn) {
  //    find(title, issn, eissn, null, null)
  //  }

  private Map class_one_match (def ids) {

    // Get the class 1 identifier namespaces.
    Set<String> class_one_ids = grailsApplication.config.identifiers.class_ones
    def xcheck = grailsApplication.config.identifiers.cross_checks

    // Return the list of class 1 identifiers we have found or created, as well as the
    // list of matches
    def result = [
      "class_one"         : false,
      "ids"               : [],
      "matches"           : [] as Set,
      "x_check_matches"   : [] as Set
    ]

    // Go through each of the class_one_ids and look for a match.
    ids.each { id_def ->

      if (id_def.type && id_def.value) {

        // id_def is map with keys 'type' and 'value'
        Identifier the_id = Identifier.lookupOrCreateCanonicalIdentifier(id_def.type, id_def.value)

        // Add the id.
        result['ids'] << the_id

        // We only treat a component as a match if the matching Identifer
        // is a class 1 identifier.
        if (class_one_ids.contains(id_def.type)) {

          // Flag class one is present.
          result['class_one'] = true

          // Flag for title match
          boolean title_match = false
          
          // If we find an ID then lookup the components.
          Set<KBComponent> comp = the_id.identifiedComponents
          comp.each { KBComponent c ->

            // Ensure we're not looking at a Hibernate Proxy class representation of the class
            KBComponent dproxied = ClassUtils.deproxy(c);

            // Only add if it's a title.
            if ( dproxied instanceof TitleInstance ) {
              title_match = true
              result['matches'] << (dproxied as TitleInstance)
            }
          }
          
          // Did the ID yield a Title match?
          if (!title_match) {
            
            log.debug ("No class one ti match.")
            
            // We should see if the current ID namespace should be cross checked with another.
            def other_ns = null
            for (int i=0; i<xcheck.size() && (!(other_ns)); i++) {
              Set<String> test = xcheck[i]
              
              if (test.contains(id_def.type)) {
                
                // Create the set then remove the matched instance to test teh remaining ones.
                other_ns = new HashSet<String>(test)
                
                // Remove the current namespace.
                other_ns.remove(id_def.type)
                log.debug ("Cross checking for ${id_def.type} in ${other_ns.join(", ")}")
                
                Identifier xc_id = null
                for (int j=0; j<other_ns.size() && !(xc_id); j++) {
                  
                  String ns = other_ns[j]
                  
                  IdentifierNamespace namespace = IdentifierNamespace.findByValue(ns)
                  
                  if (namespace) {
                  
                    // Lookup the identifier namespace.
                    xc_id = Identifier.findByNamespaceAndValue(namespace, id_def.value)                  
                    log.debug ("Looking up ${ns}:${id_def.value} returned ${xc_id}.")
                    
                    comp = xc_id?.identifiedComponents
                    
                    comp?.each { KBComponent c ->
          
                      // Ensure we're not looking at a Hibernate Proxy class representation of the class
                      KBComponent dproxied = ClassUtils.deproxy(c);
          
                      // Only add if it's a title.
                      if ( dproxied instanceof TitleInstance ) {
                        
                        log.debug ("Found ${id_def.value} in ${ns} namespace.")
                        
                        // Save details here so we can raise a review request, only if a single title was matched.
                        result['x_check_matches'] << [
                          "suppliedNS"  : id_def.type,
                          "foundNS"     : ns
                        ]
                        result['matches'] << (dproxied as TitleInstance)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    result
  }

  def find (String title, String publisher_name, def identifiers, def user = null, def project = null) {

    // The TitleInstance
    TitleInstance the_title = null

    if (title == null) return null

    // Create the normalised title.
    String norm_title = GOKbTextUtils.generateComparableKey(title)

    // Lookup any class 1 identifier matches
    def results = class_one_match (identifiers)

    // The matches.
    List< KBComponent> matches = results['matches'] as List

    switch (matches.size()) {
      case 0 :
        // No match behaviour.
        log.debug ("Title class one identifier lookup yielded no matches.")

        // Check for presence of class one ID
        if (results['class_one']) {
          log.debug ("One or more class 1 IDs supplied so must be a new TI.")

          // Create the new TI.
          the_title = new TitleInstance(name:title, ids:[])

        } else {

          // No class 1s supplied we should try and find a match on the title string.
          log.debug ("No class 1 ids supplied.")

          // Lookup using title string match only.
          the_title = attemptStringMatch (norm_title)

          if (the_title) {
            log.debug("TI ${the_title} matched by name. Partial match")

            // Add the variant.
            the_title.addVariantTitle(title)

            // Raise a review request
            ReviewRequest.raise(
                the_title,
                "'${title}' added as a variant of '${the_title.name}'.",
                "No 1st class ID supplied but reasonable match was made on the title name.",
                user, project
                )

          } else {

            log.debug("No TI could be matched by name. New TI, flag for review.")

            // Could not match on title either.
            // Create a new TI but attach a Review request to it.
            the_title = new TitleInstance(name:title, ids:[])
            ReviewRequest.raise(
                the_title,
                "New TI created.",
                "No 1st class ID supplied and no match could be made on title name.",
                user, project
                )
          }
        }
        break;
      case 1 :
        // Single component match.
        log.debug ("Title class one identifier lookup yielded a single match.")
        
        // We should raise a review request here if the match was made by cross checking
        // different identifier namespaces.
        if (results['x_check_matches'].size() == 1) {
          
          def data = results['x_check_matches'][0]
          
          // Fire the review request.
          ReviewRequest.raise(
            matches[0],
            "Identifier type mismatch.",
            "Ingest file ${data['suppliedNS']} matched an existing ${data['foundNS']}.",
            user,
            project
          )
        }
        
        // Now we can examine the text of the title.
        the_title = singleTIMatch(title, norm_title, matches[0], user, project)

        break;
      default :
        // Multiple matches.
        log.debug ("Title class one identifier lookup yielded ${matches.size()} matches. This is a bad match. Ingest should skip this row.")
        break;
    }

    // If we have a title then lets set the publisher and ids...
    if (the_title) {
      
      // Add the publisher.
      addPublisher(publisher_name, the_title, user, project)

      def id_combo_type = RefdataCategory.lookupOrCreate('Combo.Type', 'KBComponent.Ids')
      results['ids'].each {
        if ( ! the_title.ids.contains(it) ) {
          // Double check the identifier we are about to add does not already exist in the system
          // Combo.Type : KBComponent.Ids
          existing_identifier = Combo.executeQuery("Select c from Combo as c where c.toComponent = ? and c.type = ?",it,id_combo_type);
          if ( existing_identifier.size() > 0 ) {
            ReviewRequest.raise(
              the_title,
              "Adding an identifier(${it.id}) to this title would create a duplicate record",
              "The ingest file suggested an identifier (${it.id}) for a title which conflicts with a record already in the system (${existing_identifier[0].fromComponent.id})",
              user,
              project
            )
          }
          else {
            the_title.ids.add(it);
          }
        }
      }

      // Try and save the result now.
      if ( the_title.save(failOnError:true, flush:true) ) {
        log.debug("Succesfully saved TI: ${the_title.name} (This may not change the db)")
      }
      else {
        the_title.errors.each { e ->
          log.error("Problem saving title: ${e}");
        }
      }
    }

    the_title
  }

  private TitleInstance addPublisher (publisher_name, ti, user = null, project = null) {

    if ( publisher_name != null ) {
      // Lookup our publisher.
      Org publisher = componentLookupService.lookupComponent(publisher_name)

      // Found a publisher.
      if (publisher) {
        def orgs = ti.getPublisher()

        // Has the publisher ever existed in the list against this title.
        if (!orgs.contains(publisher)) {

          // First publisher added?
          boolean not_first = orgs.size() > 0

          // Added a publisher?
          boolean added = ti.changePublisher (
              componentLookupService.lookupComponent(publisher_name),
              true
              )

          // Raise a review request, if needed.
          if (not_first && added) {
            ReviewRequest.raise(
                ti,
                "Added '${publisher.name}' as a publisher on '${ti.name}'.",
                "Publisher supplied in ingested file is different to any already present on TI.",
                user, project
                )
          }
        }
      }
    }

    ti
  }

  private TitleInstance attemptStringMatch (String norm_title) {

    // Default to return null.
    TitleInstance ti = null

    // Try and find a title by matching the norm string.
    // Default to the min threshold
    double best_distance = grailsApplication.config.cosine.good_threshold

    TitleInstance.list().each { TitleInstance t ->

      // Get the distance and then determine whether to add to the list or
      double distance = GOKbTextUtils.cosineSimilarity(norm_title, GOKbTextUtils.generateComparableKey(t.getName()))
      if (distance >= best_distance) {
        ti = t
        best_distance = distance
      }

      t.variantNames?.each { vn ->
        distance = GOKbTextUtils.cosineSimilarity(norm_title, vn.normVariantName)
        if (distance >= best_distance) {
          ti = t
          best_distance = distance
        }
      }
    }

    // Return what we have found... If anything.
    ti
  }

  private TitleInstance singleTIMatch(String title, String norm_title, TitleInstance ti, User user, project = null) {

    // The threshold for a good match.
    double threshold = grailsApplication.config.cosine.good_threshold

    // Work out the distance between the 2 title strings.
    double distance = GOKbTextUtils.cosineSimilarity(GOKbTextUtils.generateComparableKey(ti.getName()), norm_title)

    // Check the distance.
    switch (distance) {

      case 1 :

        // Do nothing just continue using the TI.
        log.debug("Exact distance match for TI.")
        break

      case {
        ti.variantNames.find {alt ->
          GOKbTextUtils.cosineSimilarity(GOKbTextUtils.generateComparableKey(alt.variantName), norm_title) >= threshold
        }}:
        // Good match on existing variant titles
        log.debug("Good match for TI on variant.")
        break

      case {it >= threshold} :

        // Good match. Need to add as alternate name.
        log.debug("Good distance match for TI. Add as variant.")
        ti.addVariantTitle(title)
        break

      default :
        // Bad match...
        ti.addVariantTitle(title)

        // Raise a review request
        ReviewRequest.raise(
            ti,
            "'${title}' added as a variant of '${ti.name}'.",
            "Match was made on 1st class identifier but title name seems to be very different.",
            user, project
            )
        break
    }

    ti
  }

  
  /**
   * @param ids should be a list of maps containing at least an ns and value key. 
   * @return
   */
  public def matchClassOnes (def ids) {
    def result = [] as Set
    
    // Get the class 1 identifier namespaces.
    Set<String> class_one_ids = grailsApplication.config.identifiers.class_ones

    def start_time = System.currentTimeMillis();
    
    ids.each { def id_def ->
      // Class ones only.
      if ( id_def.value && 
           id_def.ns && 
           class_one_ids.contains(id_def.ns) ) {
      
        def identifiers = Identifier.createCriteria().list(max: 5) {
          and { 
            namespace {
              inList "value", id_def.ns
            }
            
            eq "value", id_def.value
          }
        }

        if ( identifiers.size() > 4 ) {
          log.warn("matchClassOne for ${id_def} returned a high number of candidate records. This shouldn't be the case");
        }
        
        // Examine the identified components.
        identifiers?.each {
          it?.identifiedComponents.each {
            KBComponent comp = KBComponent.deproxy(it)
            if (comp instanceof TitleInstance) {
              
              // Add to the set.
              result << (TitleInstance)comp
            }
          }
        }
      }
    }

    def elapsed = System.currentTimeMillis() - start_time;
    if ( elapsed > 2000 ) {
      log.warn("matchClassOnes took much longer than expected to complete when processing ${ids}. Needs investigation");
    }
    
    result
  }

  public def matchClassOneComponentIds(def ids) {
    def result = null

    // Get the class 1 identifier namespaces.
    Set<String> class_one_ids = grailsApplication.config.identifiers.class_ones

    def start_time = System.currentTimeMillis();

    def bindvars = []
    StringWriter sw = new StringWriter()
    sw.write("select DISTINCT c.fromComponent.id from Combo as c where ( ")


    def ctr = 0;
    ids.each { def id_def ->
      // Class ones only.
      if ( id_def.value && id_def.ns && class_one_ids.contains(id_def.ns) ) { 
        def ns = IdentifierNamespace.findByValue(id_def.ns)
        if ( ns ) {

          if ( ctr++ ) {
            sw.write(" or ");
          }

          sw.write( "( c.toComponent.value = ? and c.toComponent.namespace.id = ? )" )
          bindvars.add(id_def.value)
          bindvars.add(ns.id)
        }
      }
    }


    if ( ctr > 0 ) {
      sw.write(" ) and c.type.value=? and c.toComponent.class = Identifier");
      bindvars.add('KBComponent.Ids');
      def qry = sw.toString();
      log.debug("Run: ${qry} ${bindvars}");
      result = TitleInstance.executeQuery(qry,bindvars);
    }
    else {
      log.warn("No class 1 identifiers(${class_one_ids}) in ${ids}");
    }

    log.debug("Returning Result of matchClassOneComponentIds(${ids}) : ${result}");
    result
  }

  def Object getTitleField(title_id, field_name) {
    def result = TitleInstance.executeQuery("select ti."+field_name+" from TitleInstance as ti where ti.id=?",title_id);
    return result.size() == 1 ? result[0] : null;
  }

  def Object getTitleFieldForIdentifier(ids, field_name) {
    def result = null
    def l = matchClassOneComponentIds(ids)
    if ( l && l.size() == 1 ) {
      result = TitleInstance.executeQuery("select ti."+field_name+" from TitleInstance as ti where ti.id=?",l[0])[0];
    }
    log.debug("getTitleFieldForIdentifier(${ids},${field_name} : ${result}");
    return result
  } 

}
