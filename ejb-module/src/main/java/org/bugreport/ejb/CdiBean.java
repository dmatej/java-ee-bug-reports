/**
 *
 */
package org.bugreport.ejb;

import javax.inject.Named;

@Named
public class CdiBean {

  public String hello() {
    return "hello!";
  }
}
