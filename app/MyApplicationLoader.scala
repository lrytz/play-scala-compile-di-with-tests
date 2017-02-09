import javax.inject.Provider

import play.api._
import play.api.http.{HttpFilters, HttpRequestHandler, JavaCompatibleHttpRequestHandler}
import play.api.i18n._
import play.api.inject._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.core.DefaultWebCommands
import play.core.j.DefaultJavaHandlerComponents
import play.http.DefaultActionCreator
import router.Routes

class MyApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new MyComponents(context).application
  }
}

/**
 * An application builder for running an application in tests
 */
class MyApplicationBuilder {

  def build(): Application = {
    val env = Environment.simple()
    val context = new ApplicationLoader.Context(
      environment = env,
      sourceMapper = None,
      webCommands = new DefaultWebCommands(),
      initialConfiguration = Configuration.load(env)
    )
    val loader = new MyApplicationLoader()
    loader.load(context)
  }
}

class MyComponents(context: ApplicationLoader.Context) 
  extends BuiltInComponentsFromContext(context)
  with I18nComponents
  with AhcWSComponents {

  override lazy val injector =  {
    new SimpleInjector(NewInstanceInjector) +
      router +
      cookieSigner +
      csrfTokenSigner +
      httpConfiguration +
      tempFileCreator +
      global +
      crypto +
      wsApi +
      messagesApi +
      bodyParser
  }

  lazy val router: Router =
    new Routes(httpErrorHandler, homeController, assets, javaController)

  // need a play.Environment, `environment` is play.api.Environment
  val jEnvironment: play.Environment = null
  // need a play.Configuration, `configuration` is play.api.Configuration
  val jConfiguration: play.Configuration = null
  // need a play.http.HttpErrorHandler, `httpErrorHandler` is `play.api.http.HttpErrorHandler`
  val jHttpErrorHandler: play.http.HttpErrorHandler =
    new play.http.DefaultHttpErrorHandler(
      jConfiguration,
      jEnvironment,
      new OptionalSourceMapper(sourceMapper),
      new Provider[Router] { def get = router })
  lazy val bodyParser =
    new play.mvc.BodyParser.Default(jHttpErrorHandler, httpConfiguration)

  lazy val jhc =
    new DefaultJavaHandlerComponents(injector, new DefaultActionCreator())
  override lazy val httpRequestHandler: HttpRequestHandler =
    new JavaCompatibleHttpRequestHandler(router,
                                         httpErrorHandler,
                                         httpConfiguration,
                                         HttpFilters(httpFilters: _*),
                                         jhc)

  lazy val homeController = new controllers.HomeController()
  lazy val assets = new controllers.Assets(httpErrorHandler)
  lazy val javaController = new controllers.JavaController()
}
