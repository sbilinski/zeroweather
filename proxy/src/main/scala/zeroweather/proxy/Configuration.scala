package zeroweather.proxy

import com.typesafe.config.{ Config, ConfigFactory }

trait Configuration {
  lazy val config: Config = ConfigFactory.load()
}
