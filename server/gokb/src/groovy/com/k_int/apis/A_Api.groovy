package com.k_int.apis

import groovy.util.logging.Log4j

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.grails.datastore.gorm.AbstractGormApi
import org.springframework.context.ApplicationContext


/**
 * @author Steve Osguthorpe <steve.osguthorpe@k-int.com>
 * API class to add metamethods associated with Security.
 * 
 * This abstract class is an attempt to produce a mechanism to easily extend the classes within
 * Grails and Groovy.
 * 
 * Extenders of this class should declare any methods they wish to add as meta-methods using the public visibility
 * modifier.
 * 
 * Public static methods are added to the class and the first parameter supplied should be the class they are extending.
 * Public none-static methods are added to each instance of the class and the first parameter is the instance itself.
 * 
 * The first parameter is added dynamically and won't be part of the generated meta-signature.
 * 
 * i.e. public myMethod(T instance, String foo) would add the method myMethod(String foo) to the target.
 */
@Log4j
abstract class A_Api <T> {

  static String NO_ARGS_METHOD = 'NO_ARGS_METHOD'

  protected static final Set<String> EXCLUDES = AbstractGormApi.EXCLUDES + [
    // Extend the list with any that aren't caught here.
  ]

  /**
   * Map to allow quick access to the APIs attached to a particular class.
   */
  private static final Map<Class<T>, Map<Class<A_Api>, A_Api>> map = [:].withDefault {Class target ->
    [:].withDefault { Class type ->
      type.newInstance(["targetClass" : (target)])
    }
  }

  protected Class<T> targetClass

  protected A_Api () {}

  protected static ApplicationContext appContext

  /**
   * Statically retrieve the application context.
   * 
   * @return ApplicationContext the app context
   */
  protected static ApplicationContext getApplicationContext() {
    if (!appContext) appContext = SCH.servletContext.getAttribute(GA.APPLICATION_CONTEXT)
    appContext
  }

  /**
   * Implementation of the groovy property missing.
   * @param name
   * @return
   */
  protected def propertyMissing (String name) {
    A_Api.propertyMissing(name)
  }

  static {

    // Add the method missing in a static context.
    getMetaClass()."static".propertyMissing = { name ->
      // Try and retrieve a service from the application context.
      if (name =~ /.*Service/) {
        try {
          return getApplicationContext()."${name}"

        } catch (Exception e) {
          throw new MissingPropertyException(name, this, e)
        }
      }

      // We should always throw a property missing exception if we haven't returned above.
      throw new MissingPropertyException(name, this)
    }
  }

  /**
   * This is responsible for adding the methods to the targets.
   * 
   * @param targetClass The target class
   * @param apiClass The class containing the methods we are to add.
   */
  public static void addMethods(Class<T> targetClass, Class<A_Api> apiClass) {

    // The API.
    A_Api api = A_Api.map.get(targetClass).get(apiClass)

    // println("Attempt to bind api ${apiClass.name} to targetClass ${targetClass.name}");

    // Should we bind this api to this class?
    if (api.applicableFor(targetClass) ) {
      log.debug("Adding ${api.class.name} methods to ${targetClass.name}");

      apiClass.getDeclaredMethods().each { Method m ->
        // println("processing declared method ${m}");
        def mods = m.getModifiers()

        if (!m.isSynthetic() && Modifier.isPublic(mods) && !EXCLUDES.contains(m.name)) {
          // println("Inside2");
          Class<?>[] pTypes = m.getParameterTypes()
          if (pTypes.length > 0) {
            pTypes = Arrays.copyOfRange(pTypes, 1, pTypes.length)
          }

          if (!Modifier.isStatic(mods)) {
            
            // See if we can already find a match for this method.
            def existing = targetClass.metaClass.methods.find { MetaMethod meth ->
              meth.getName() == "${m.name}" &&
              !meth.isStatic() &&
              meth.isValidMethod(pTypes)
            }
            
            if (!existing) {
            
              // Add this method to the target.
              targetClass.metaClass."${m.name}" << { args = NO_ARGS_METHOD ->
  
                // println("dynamic ${m.name} ${args}");
  
                // Please see coment in static section below - I *think* the same problem will manifest itself for non-static methods
                // So adding this fix here also
                // List the_args = (args != null && !(args instanceof Collection)) ? [args] : (args ?: []) as List
                // Array list lets us add null entries for calls like f(null) - OBV this is usually going to be an error, but we need the actual function f
                // to encounter the null and throw the exception rather than bombing out here
                ArrayList the_args = new ArrayList()
  
                if ( args.is(NO_ARGS_METHOD) ) { // no args method - nothing to do!!
                  // println("Dynamic no args..");
                }
                else {
                  if ( args instanceof Collection )
                    the_args.addAll(args);
                  else
                    the_args.add(args);
                }
  
                // First check to see if there is an original method defined.
                // if there is then we should use that instead.
                if (delegate.respondsTo("${m.name}", the_args.toArray())) {
                  // The element
                }
  
  
                // Prepend the new value.
                the_args.add(0, delegate)
                api.invokeMethod("${m.name}", the_args.toArray())
              }
            } else {
              log.debug("Skipping ${m.name} on ${targetClass.name}, allready declared.") 
            }
          } else {
            // println("Adding static method.. ${m.name} to ${targetClass.name}");
            // Add to the static scope.

            // II : If the meta method is invoked as f() then args will take the default value NO_ARGS_METHOD and we know thats how it's been called
            // If the meta method is invoked as f(null) then we get an explicit null in args, and all is well with the world..
            // targetClass.metaClass.static."${m.name}" = { args = NO_ARGS_METHOD ->
            // See if we can already find a match for this method.
            def existing = targetClass.metaClass.methods.find { MetaMethod meth ->
              meth.getName() == "${m.name}" &&
              meth.isStatic() &&
              meth.isValidMethod(pTypes)
            }
            
            if (!existing) {
              targetClass.metaClass.static."${m.name}" << { args = NO_ARGS_METHOD ->
  
                // List the_args = (args != null && !(args instanceof Collection)) ? [args] : (args ?: []) as List
  
                // Think this breaks when a function to call has a single argument, and that argument is passed as null - ie f(null)
                // essentially creates a the_args with no args
                // Array list lets us add null entries for calls like f(null) - OBV this is usually going to be an error, but we need the actual function f
                // to encounter the null and throw the exception rather than bombing out here
                ArrayList the_args = new ArrayList()
  
                if ( args?.is(NO_ARGS_METHOD) ) { // no args method - nothing to do!!
                }
                else {
                  if ( args instanceof Collection ) {
                    the_args.addAll(args);
                  }
                  else {
                    the_args.add(args);
                  }
                }
  
                // println("Calling static method ${m.name}.. Target Class was ${targetClass.name} Delegate is ${delegate.class.name}");
  
                // Prepend the new value.
                // II : I *think* this is what was intended
                the_args.add(0, targetClass)
                // And not this:: the_args.add(0, delegate.class) (which is java.lang.Class in the case where a static is called)
                def result = apiClass.invokeMethod("${m.name}", the_args.toArray())
  
                // println("Call completed returning");
  
                result
              }
            } else {
              log.debug("Skipping static ${m.name} on ${targetClass.name}, allready declared.") 
            }
          }
        }
      }
    }
    else {
      log.debug("Skipping ${targetClass.name} for ${api.class}");
    }
  }

  /**
   * Allows us to programmatically exclude a class. Defaults to true here.
   * 
   * @param targetClass The target class to check.
   * @return
   */
  protected boolean applicableFor (Class targetClass) {
    return true
  }
}
