package net.propoint.fun.config

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{Duration, FiniteDuration}

final case class AppConfig (
                       ecommDBWriteConfig: MysqlConfig,
                       catalogDBWriteConfig: MysqlConfig
                     )

object AppConfig {
  implicit class RichConfig(val c: Config) {
    def getStringOpt(path: String): Option[String] = if (c.hasPath(path)) {
      Some(c.getString(path))
    } else None

    def getBooleanOpt(path: String): Option[Boolean] = if (c.hasPath(path)) {
      Some(c.getBoolean(path))
    } else None

    def getConfigOpt(path: String): Option[Config] = if (c.hasPath(path)) {
      Some(c.getConfig(path))
    } else None

    def getFiniteDuration(path: String): FiniteDuration = Duration.fromNanos(c.getDuration(path).toNanos)
  }

  // call this at the start of the program, and nowhere else
  def loadFromEnvironment(): AppConfig = {
    load(ConfigFactory.load())
  }

  private def load(config: Config): AppConfig = {
    AppConfig(
      ecommDBWriteConfig = MysqlConfig.load(config, "dbs.ecomm.write"),
      catalogDBWriteConfig = MysqlConfig.load(config, "dbs.catalog.write")
    )
  }
  
}
