package org.bugreport.ejb.test;

import java.util.Objects;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFishException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

/**
 * @author David Matějček
 */
@RunWith(Arquillian.class)
public abstract class ArquillianDaoUnitTest {

  private static final Logger LOG = LogManager.getLogger(ArquillianDaoUnitTest.class);

  private static boolean containerInitialized;

  private long start;
  @Rule
  public final TestName name = new TestName();


  /**
   * Only to mark the class initialization in logs
   */
  @BeforeClass
  public static void initContainer() {
    LOG.info("initContainer()");
  }


  @Before
  public void before() {
    LOG.info("before(). Test name: {}", name.getMethodName());
    this.start = System.currentTimeMillis();
  }


  @After
  public void after() {
    LOG.info("after(). Test name: {}, test time: {} ms", name.getMethodName(), System.currentTimeMillis() - this.start);
  }


  /**
   * Initializes the deployment unit.
   *
   * @return {@link EnterpriseArchive} to deploy to the container.
   * @throws Exception exception
   */
  @Deployment
  public static JavaArchive getArchiveToDeploy() throws Exception {
    if (!containerInitialized) {
      initEnvironment();
      containerInitialized = true;
    }

    final JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class).addPackages(true, "org.bugreport.ejb")
        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");//

    LOG.info(ejbModule.toString(true));
    return ejbModule;
  }


  private static void initEnvironment() {
    LOG.debug("initEnvironment()");
    LOG.debug("System properties:\n  {}", System.getProperties());

    try {
      runCommand("list-jdbc-connection-pools", "--echo=true", "--terse=true");

      runCommand("set", "configs.config.server-config.jms-service.type=DISABLED");
      runCommand("set", "configs.config.server-config.admin-service.das-config.deploy-xml-validation=none");
      runCommand("set", "configs.config.server-config.iiop-service.iiop-listener.orb-listener-1.port=17300");
      runCommand("set", "configs.config.server-config.iiop-service.iiop-listener.SSL.port=17301");
      runCommand("set", "configs.config.server-config.iiop-service.iiop-listener.SSL_MUTUALAUTH.port=17302");
    } catch (final Exception e) {
      throw new IllegalStateException("Cannot initialize the container!", e);
    }
  }


  /**
   * Execute the command with parameters and return a result.
   *
   * @param command
   * @param parameters
   * @return result of the command
   * @throws GlassFishException - cannot communicate with the instance
   * @throws IllegalStateException - invalid parameters or command
   */
  private static CommandResult runCommand(final String command, final String... parameters) throws GlassFishException {
    LOG.debug("runCommand(command={}, parameters={})", command, parameters);

    final CommandRunner runner;
    try {
      final InitialContext ctx = new InitialContext();
      runner = (CommandRunner) ctx.lookup(CommandRunner.class.getCanonicalName());
      Objects.requireNonNull(runner, "No command runner instance found in initial context!");
    } catch (final NamingException e) {
      throw new IllegalStateException("Cannot run command " + command, e);
    }

    final CommandResult result = runner.run(command, parameters);
    checkCommandResult(command, result);
    return result;
  }


  private static void checkCommandResult(final String cmd, final CommandResult result) {
    LOG.info("Command: {}\n  Result.status:\n  {}\n  Result.out:\n  {}\n  Result.failCause:\n  {}\n", cmd,
        result.getExitStatus(), result.getOutput(), result.getFailureCause());

    if (result.getExitStatus().ordinal() != 0) {
      throw new IllegalStateException("Command '" + cmd + "' was unsuccessful: " + result.getOutput(),
          result.getFailureCause());
    }
  }
}
