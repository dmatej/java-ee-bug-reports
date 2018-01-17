/**
 *
 */
package org.bugreport.ejb;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.bugreport.ejb.test.ArquillianDaoUnitTest;
import org.junit.Test;

/**
 * @author David Matějček
 */
public class BugReportTest extends ArquillianDaoUnitTest {

  @Inject
  private CdiBean bean;

  @Test
  public void test() {
    final String response = bean.hello();
    assertEquals("hello!", response);
  }

}
