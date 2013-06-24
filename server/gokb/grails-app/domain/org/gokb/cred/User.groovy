package org.gokb.cred

class User {

  transient springSecurityService

  String username
  String password
  String displayName
  String email
  boolean enabled
  boolean accountExpired
  boolean accountLocked
  boolean passwordExpired
  
  static manyByCombo = [
    territories : Territory
  ]

  static constraints = {
    username blank: false, unique: true
    password blank: false
    displayName blank: true, nullable:true
    email blank: true, nullable:true
  }

  static mapping = {
    password column: '`password`'
  }

  Set<Role> getAuthorities() {
    UserRole.findAllByUser(this).collect { it.role } as Set
  }

  def beforeInsert() {
    encodePassword()
    if ( displayName == null )
      displayName = username
  }

  def beforeUpdate() {
    if (isDirty('password')) {
      encodePassword()
    }
    if ( displayName == null )
      displayName = username
  }

  protected void encodePassword() {
    password = springSecurityService.encodePassword(password)
  }
}
