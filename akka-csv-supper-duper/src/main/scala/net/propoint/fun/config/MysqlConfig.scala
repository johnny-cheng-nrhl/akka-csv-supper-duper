package net.propoint.fun.config

import cats.effect.Async
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor

case class MysqlConfig (
                         driver: String,
                         url: String,
                         user: String,
                         password: String
                       )
object MysqlConfig {
  import DoobieConnections._

  def load(config: Config, configLocation: String): MysqlConfig = {
    val dbsDefaultConfig = config.getConfig(configLocation)
    val dbConfig = dbsDefaultConfig.getConfig("db")
    MysqlConfig(
      driver = dbsDefaultConfig.getString("driver"),
      url = dbConfig.getString("url"),
      user = dbConfig.getString("user"),
      password = dbConfig.getString("password")
    )
  }

  def writeConnection[F[_]: Async](databaseConfig: MysqlConfig): Write[F] = Write(
    xa = dbTransactor(databaseConfig)
  )

  private def dbTransactor[F[_]: Async](databaseConfig: MysqlConfig): HikariTransactor[F] =
    HikariTransactor[F](getCustomHikariDataSource(databaseConfig))

  private def getCustomHikariDataSource(databaseConfig: MysqlConfig): HikariDataSource = {
    val ds = new HikariDataSource()
    ds.setDriverClassName(databaseConfig.driver)
    ds.setJdbcUrl(databaseConfig.url)
    ds.setUsername(databaseConfig.user)
    ds.setPassword(databaseConfig.password)

    ds
  }
}
