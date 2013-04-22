package org.gokb

import grails.util.GrailsNameUtils


import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.gokb.cred.Combo
import org.gokb.cred.KBComponent
import org.gokb.cred.RefdataCategory
import org.gokb.cred.RefdataValue
import groovy.util.logging.*

@Log4j
class DomainClassExtender {
  
  private static addGetComboMap = { DefaultGrailsDomainClass domainClass ->

    // Get the metaclass.
    domainClass.getMetaClass().getComboMap = { String mappingName ->
      log.trace("getComboMap called on ${delegate} with args ${[mappingName]}")
      getComboMapFor (domainClass.getClazz(), mappingName)
    }
  }
  
  private static extendMapConstructor = { DefaultGrailsDomainClass domainClass ->
    
    // Get the metaclass.
    ExpandoMetaClass mc = domainClass.getMetaClass()
    
    // Get the original contructor.
    def oldConstructor = mc.retrieveConstructor(Map)
    mc.constructor = { Map args ->
      log.trace("MapConstructor called for new ${delegate} with args ${args}")
      
      log.debug ("Calling original contructor for new ${delegate} with args ${args}.")
      
      // Instantiate the object and save...
      // We really need to save here so we can reference this object within the combos.
      def instance = oldConstructor.newInstance(args).save()
      
      // Now that we have created our instance using the original constructor we can,
      // now set the combo props that were missed.
      Set cProps = []
      cProps.addAll( getComboMapFor (instance.getClass(), Combo.HAS).keySet() )
      cProps.addAll( getComboMapFor (instance.getClass(), Combo.MANY).keySet() )
      for (prop in args.keySet()) {
        if (cProps.contains(prop)) {
          // Set the combo property directly.
          instance.setComboProperty(prop, args[prop])
        }
      }
      instance
    }    
  }

  private static addGetComboMapFor = { DefaultGrailsDomainClass domainClass ->

    // Get the metaclass.
    ExpandoMetaClass mc = domainClass.getMetaClass()
    mc.static.getComboMapFor = { Class forClass, String mapName ->
      log.trace("getComboMapFor called on ${delegate} with args ${[forClass,mapName]}")

      // Return from cache if present.
      String cacheKey = "${GrailsNameUtils.getShortName(forClass)}:${mapName}"
      log.debug("Checking cache for ${cacheKey}")
      def value = DomainClassExtender.comboMappingCache[cacheKey]
      if (value != null) return value

      log.debug("Not found in cache.")
      try {
      
        // Lookup the value using the metaclass allowing superclass traversal.
        value = mc.getProperty(delegate.getClass(), forClass, mapName, true, true)
      } catch (Exception e) { value = [:] }

      if (value == null) value = [:]
      
      // Cache it.
      DomainClassExtender.comboMappingCache[cacheKey] = value

      // Return the value.
      log.debug("${value} found.")
      value
    }
  }

  private static addGetComboProperty = { DefaultGrailsDomainClass domainClass ->
    MetaClass mc = domainClass.getMetaClass()
    mc.getComboProperty {String propertyName ->
      log.trace("getComboProperty called on ${delegate} with args ${[propertyName]}")

      // Test this way to allow us to cache null values.
      String cacheKey = "${delegate.toString()}.${propertyName}".toString()
      if (DomainClassExtender.comboPropertyCache.containsKey(cacheKey)) return DomainClassExtender.comboPropertyCache.get(cacheKey);

      // Check the type.
      Class typeClass = lookupComboMapping(Combo.MANY, propertyName)

      // Generate the type.
      RefdataValue type = RefdataCategory.lookupOrCreate("Combo.Type", getComboTypeValue(propertyName))

      if (typeClass) {

        // Default many association maps to empty set.
        def result = []

        if (isComboReverse(propertyName)) {
          // Reverse.
          def combos = incomingCombos.findAll {
            it.type == (type)
          }

          if (combos) {
            for (combo in combos) {
              result.add(combo.fromComponent)
            }
          }

        } else {
          def combos = outgoingCombos.findAll {
            it.type == (type)
          }

          if (combos) {
            for (combo in combos) {
              result.add(combo.toComponent)
            }
          }
        }

        // Add the result to the cache.
        DomainClassExtender.comboPropertyCache.put(cacheKey, result)

        log.debug("${result} found.")
        return result

      } else {

        // Try singular.
        typeClass = lookupComboMapping(Combo.HAS, propertyName)

        if (typeClass) {
          def result = null
          if (isComboReverse(propertyName)) {

            // Just return the component.
            Combo combo = incomingCombos.find {
              it.type == (type)
            }

            if (combo) result = combo.fromComponent
          } else {
            Combo combo = outgoingCombos.find {
              it.type == (type)
            }

            if (combo) result = combo.toComponent
          }

          // Add the result to the cache.
          DomainClassExtender.comboPropertyCache.put(cacheKey, result)

          log.debug("${result} found.")
          return result
        }

        log.debug("No Property found, throw Exception.")
        // If we get here then throw an exception.
        throw new MissingPropertyException(propertyName, this.class)
      }

    }
  }
  
  private static addGetComboTypeValue = {DefaultGrailsDomainClass domainClass ->

    // Get the metaclass.
    domainClass.getMetaClass().getComboTypeValue = {String propertyName  ->
      log.trace("getComboTypeValue called on ${delegate} with args ${[propertyName]}")
      getComboTypeValueFor(domainClass.getClazz(), propertyName)
    }
  }

  private static addGetComboTypeValueFor = {DefaultGrailsDomainClass domainClass ->

    // Get the metaclass.
    MetaClass mc = domainClass.getMetaClass()
    mc.static.getComboTypeValueFor = {Class forClass, String propertyName  ->
      log.trace("getComboTypeValueFor called on ${delegate} with args ${[forClass,propertyName]}")
      String capProp
      Class mappedByClass
      def mappedByProp = lookupComboMappingFor(forClass, Combo.MAPPED_BY, propertyName)
      if (mappedByProp) {
        // We need to look up the relationship the other way round.
        // First find the class type mapped to.
        mappedByClass = lookupComboMappingFor(forClass, Combo.MANY, propertyName)
        mappedByClass = mappedByClass ?: lookupComboMappingFor(forClass, Combo.HAS, propertyName)

        if (mappedByClass) {
          // Found the class, we can now use this information to build up our string.
          if (mappedByProp.length() > 1) {
            capProp = mappedByProp[0].toUpperCase() + mappedByProp[1..-1]
          } else {
            capProp = mappedByProp.toUpperCase()
          }
        }
      } else {
        if (propertyName.length() > 1) {
          capProp = propertyName[0].toUpperCase() + propertyName[1..-1]
        } else {
          capProp = propertyName.toUpperCase();
        }

        // Set the class also.
        mappedByClass = forClass
      }

      // Return the constructed key.
      def key = GrailsNameUtils.getShortName(mappedByClass) + ".${capProp}"
      
      log.debug("${key} generated.")
      key
    }
  }

  private static addIsComboReverse = { DefaultGrailsDomainClass domainClass ->
    domainClass.getMetaClass().isComboReverse = {String propertyName ->
      log.trace("isComboReverse called on ${delegate} with args ${[propertyName]}")
      (lookupComboMapping (Combo.MAPPED_BY, propertyName) != null)
    }
  }

  private static addLookupComboMapping = { DefaultGrailsDomainClass domainClass ->

    // Get the metaclass.
    domainClass.getMetaClass().lookupComboMapping = {String mappingName, String propertyName ->
      
      log.trace("lookupComboMapping called on ${delegate} with args ${[mappingName,propertyName]}")
      lookupComboMappingFor (domainClass.getClazz(), mappingName, propertyName)
    }
  }

  private static addLookupComboMappingFor = { DefaultGrailsDomainClass domainClass ->

    // Get the metaclass.
    MetaClass mc = domainClass.getMetaClass()
    mc.static.lookupComboMappingFor = {Class forClass, String mappingName, String propertyName ->
      log.trace("lookupComboMappingFor called on ${delegate} with args ${[forClass,mappingName,propertyName]}")
      // Get the map.
      def map = getComboMapFor (forClass, mappingName)

      // Return the property.
      def prop = map[propertyName]
      
      log.debug("${delegate}.${propertyName} maps to type ${prop}.")
      prop
    }
  }
  private static addRemoveComboPropertyVals = { DefaultGrailsDomainClass domainClass ->
    MetaClass mc = domainClass.getMetaClass()
    mc.removeComboPropertyVals {String propertyName ->
      log.trace("removeComboPropertyVals called on ${delegate} with args ${propertyName}")
      // Generate the type.
      RefdataValue type = RefdataCategory.lookupOrCreate("Combo.Type", getComboTypeValue(propertyName))
  
      // Get all..
      List<Combo> combos
      if (isComboReverse(propertyName)) {
        // Reverse.
        combos = incomingCombos.findAll {
          it.type == (type)
        }
      } else {
        combos = outgoingCombos.findAll {
          it.type == (type)
        }
      }
  
      // Delete each combo in turn.
      for (combo in combos) {
        log.debug("removing Combo of type ${combo.type} from ${delegate}.")
        
        // Need to make sure we remove from both sides of the,
        // association before attempting to remove the combo.
        combo.fromComponent?.removeFromOutgoingCombos(combo)
        combo.toComponent?.removeFromIncomingCombos(combo)
        
        // Remove the combo.
        combo.delete()
      }
  
      // Clear the cached value too if present.
      DomainClassExtender.comboPropertyCache.remove("${delegate}.${propertyName}".toString())
    }
  }

  private static addSetComboProperty = {DefaultGrailsDomainClass domainClass ->
    MetaClass mc = domainClass.getMetaClass()
    mc.setComboProperty = {String propertyName, def value ->
      log.trace("setComboProperty called on ${delegate} with args ${[propertyName, value]}")
      Class typeClass
      switch (value) {
        case Collection :
          // Check the many relationships
          typeClass = lookupComboMapping(Combo.MANY, propertyName)
          
          if (typeClass == null) throw new IllegalArgumentException(
            "Supplied value for setComboProperty was a collection, but could not find mapping for ${propertyName} in ${Combo.MANY} for class ${domainClass.getClazz()}"
          )
          break
        default:
          // Check single properties
          typeClass = lookupComboMapping(Combo.HAS, propertyName)
          
          if (typeClass == null) throw new IllegalArgumentException(
            "Supplied value for setComboProperty was a singular none collection, but could not find mapping for ${propertyName} in ${Combo.HAS} for class ${domainClass.getClazz()}"
          )
      }

      if (typeClass) {

        // Capitalise the propertyName.
        removeComboPropertyVals(propertyName)

        if (value) {

          // Generate the type.
          RefdataValue type = RefdataCategory.lookupOrCreate("Combo.Type", getComboTypeValue(propertyName))

          // Go through each item and generate a value.
          switch (value) {
            case Collection :

              if (isComboReverse(propertyName)) {
                // Reverse
                for (val in value) {
                  if (typeClass.isInstance(val)) {
                    
                    log.debug("adding incoming Combo of type ${type} from ${val} to ${delegate}.")
                    Combo combo = new Combo(
                      type : (type),
                      status : RefdataCategory.lookupOrCreate("Combo.Status", "Active")
                    ).save()

                    // Add to the collections.
                    delegate.addToIncomingCombos(combo)
                    val.addToOutgoingCombos(combo)
                  } else {
                    throw new IllegalArgumentException(
                      "All values in collection for property ${delegate}.${propertyName} should be of defined type: ${typeClass.getName()}"
                    )
                  }
                }
              } else {
                for (val in value) {
                  if (typeClass.isInstance(val)) {
                    log.debug("adding incoming Combo of type ${type} from ${delegate} to ${val}.")
                    Combo combo = new Combo(
                      type : (type),
                      status : RefdataCategory.lookupOrCreate("Combo.Status", "Active")
                    ).save()

                    // Add to the collections.
                    delegate.addToOutgoingCombos(combo)
                    val.addToIncomingCombos(combo)
                  } else {
                    throw new IllegalArgumentException(
                      "All values in collection for property ${delegate}.${propertyName} should be of defined type: ${typeClass.getName()}"
                    )
                  }
                }
              }
              break
            default:
              // Check single properties.
              typeClass = lookupComboMapping(Combo.HAS, propertyName)
              if (typeClass.isInstance(value)) {

                if (isComboReverse(propertyName)) {
                  log.debug("adding incoming Combo of type ${type} from ${value} to ${delegate}.")
                  Combo combo = new Combo(
                      type : (type),
                      status : RefdataCategory.lookupOrCreate("Combo.Status", "Active")
                   ).save()

                  // Add to the incoming collection
                  delegate.addToIncomingCombos(combo)
                  value.addToOutgoingCombos(combo)
                } else {
                  log.debug("adding outgoing Combo of type ${type} from ${delegate} to ${value}.")
                  Combo combo = new Combo(
                    type : (type),
                    status : RefdataCategory.lookupOrCreate("Combo.Status", "Active")
                  ).save()

                  // Add to the collections.
                  delegate.addToOutgoingCombos(combo)
                  value.addToIncomingCombos(combo)
                }
              } else {
                throw new IllegalArgumentException(
                  "Value for property ${delegate}.${propertyName} should be of defined type: ${typeClass.getName()}"
                )
              }
          }

          // Add to the cache.
          DomainClassExtender.comboPropertyCache.put("${delegate}.${propertyName}".toString(), value)
        }
      } else {
        log.debug("Thrown missing property exception for ${propertyName} on ${delegate}.")
        throw new MissingPropertyException(propertyName, domainClass.getClazz())
      }
    }
  }

  private static Map comboMappingCache = [:]
  private static Map comboPropertyCache = [:]

  public static extend = { DefaultGrailsDomainClass domainClass ->
    // Get the actual class that is represented by this domain class object.
    Class actualClass = domainClass.getClazz()

    if (!KBComponent.class.is(actualClass)) {
      if (KBComponent.class.isAssignableFrom(actualClass)) {

        // Extends KBCombonent.

        // Extend to handle ComboMapped Properties.
        DomainClassExtender.extendMapConstructor(domainClass)
        DomainClassExtender.extendMethodMissing (domainClass)
        DomainClassExtender.addGetComboMap (domainClass)
        DomainClassExtender.addLookupComboMapping (domainClass)
        DomainClassExtender.addGetComboTypeValue (domainClass)
        DomainClassExtender.addIsComboReverse (domainClass)
        DomainClassExtender.addSetComboProperty(domainClass)
        DomainClassExtender.addGetComboProperty(domainClass)
        DomainClassExtender.addRemoveComboPropertyVals(domainClass)
        DomainClassExtender.addPropertyMissing(domainClass)

        DomainClassExtender.addGetComboMapFor (domainClass)
        DomainClassExtender.addLookupComboMappingFor (domainClass)
        DomainClassExtender.addGetComboTypeValueFor (domainClass)
      }
    } else {

      // Is KBCombonent class. Just add the static helper methods.
      DomainClassExtender.extendMapConstructor(domainClass)
      DomainClassExtender.addGetComboMapFor (domainClass)
      DomainClassExtender.addLookupComboMappingFor (domainClass)
      DomainClassExtender.addGetComboTypeValueFor (domainClass)
    }
  }

  private static extendMethodMissing = { DefaultGrailsDomainClass domainClass ->
    log.debug("Extending methodMissing for ${domainClass.getClazz().getName()}")

    // Get the metaclass.
    MetaClass mc = domainClass.getMetaClass()

    // Save the old version of methodMissing so it can be used if needed
    MetaMethod oldMethodMissing = mc.methods.find { it.name == 'methodMissing' }

    mc.methodMissing = { String methodName, args ->
      log.trace("methodMissing called on ${delegate} with args ${methodName}, ${args.toString()}")
      String prefix;
      def propertyName = methodName[3].toLowerCase() + methodName[4..-1]

      // Add the propertyName as the first argument.
      def argVals = [propertyName]
      argVals.addAll(args)

      String methodToCall
      switch (methodName[0..2]) {
        case "get" :// Property name.
          methodToCall = "getComboProperty"
          break
        case "set" :
          methodToCall = "setComboProperty"
          break
      }

      // Invoke it.
      if (methodToCall) {
        try {
          Object result
          if (argVals[0] != "comboProperty") {
            
            log.debug("Invoking method ${methodToCall} on ${delegate} with args ${argVals}.")
            result = delegate.invokeMethod(methodToCall, argVals.toArray())

            // Add the metaclass method to speed up future calls.
            mc."${methodToCall}" = { Object[] varArgs ->
              log.debug("Invoking method ${methodToCall} on ${delegate} using ${varArgs}.")
              delegate.invokeMethod(methodToCall, varArgs)
            }

            return result
          }

        } catch (MissingPropertyException ex) {
          /* Do nothing as the code should drop through and try and run original method */
          log.debug("MissingPropertyException thrown (${ex.getMessage()})")
        }
      }

      // Invoke the old methodMissing...
      if (oldMethodMissing) {
        log.debug("calling oldMethodMissing on ${delegate} with args ${[args]}")
        return oldMethodMissing.invoke(delegate, args)
      }

      // Finally throw an exception if no luck.
      log.debug("Thrown MissingMethodException looking for ${methodName} on ${delegate}")
      throw new MissingMethodException(methodName, domainClass.getClazz(), args)
    }
  }
  
  private static addPropertyMissing = {DefaultGrailsDomainClass domainClass ->
    // Get the metaclass.
    MetaClass mc = domainClass.getMetaClass()

    // Save the old version of methodMissing so it can be used if needed
    MetaMethod oldPropertyMissing = mc.methods.find { it.name == 'propertyMissing' }
    
    mc.propertyMissing = {String name, value = null ->
      
      log.trace("propertyMissing called on ${delegate} with args ${[name, value]}")
      def result
      switch (name) {
        case Combo.HAS :
        case Combo.MAPPED_BY :
        case Combo.MANY :
          return null
          break
        default :
      
          // Execute the existing propertyMissing.
          if (value == null) {
            if (oldPropertyMissing != null) {
              log.trace("calling oldPropertyMissing on ${delegate} with args ${name}")
              result = oldPropertyMissing.doMethodInvoke(delegate, [name].toArray())
            }
            
            result = result ?: getComboProperty(name)
          } else {
            if (oldPropertyMissing != null) {
              
              log.trace("calling oldPropertyMissing on ${delegate} with args ${[name, value]}")
              result = oldPropertyMissing.doMethodInvoke(delegate, [name, value].toArray())
            }
            result = result ?: setComboProperty(name, value)
          }
      }
      result
    }
  }
}
