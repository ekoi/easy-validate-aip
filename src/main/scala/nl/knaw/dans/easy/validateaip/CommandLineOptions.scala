/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.validateaip

import java.io.{File}
import java.net.URL
import java.util.Properties


import com.typesafe.config.{Config, ConfigFactory}
import org.rogach.scallop._
import org.slf4j.LoggerFactory

import scala.util.Failure

object CommandLineOptions {
  val log = LoggerFactory.getLogger(getClass)
  val conf = ConfigFactory.load
  log.debug("Parsing command line ...")

  def parse(args: Array[String]): Settings = {
    val opts = new ScallopCommandLine(conf, args)

    if (args.length == 1) {
      log.debug("Validate Single AIP...")
      val aipDir = opts.aipDirectory.apply()
      Settings(aipDir)
    }
    else {
      log.debug("Validate Multi AIPs...")
      val fedoraUrl: URL = opts.fedoraServiceUrl()
      val username: String = opts.username.get.getOrElse(askUsername(fedoraUrl.toString))
      val password: String = opts.password.get.getOrElse(askPassword(username,fedoraUrl.toString))

      val aipBaseDir = opts.aipBaseDirectory()

      new Settings(username, password, fedoraUrl, aipBaseDir)
    }
  }

class ScallopCommandLine(conf: Config, args: Array[String]) extends ScallopConf(args) {
  printedName = "process-multi-deposit"
  version(s"$printedName ${Version()}")
  banner("""
           |Validate one or more AIPs in (dark) archival storage.
           |
           |Usage:
           |
           | $printedName <aip-directory>
           | $printedName --fcrepo-user <user> --fcrepo-password <password> <Fedora service URL> <aip-base-directory>
           |
           |Options:
           |""".stripMargin)

   val shouldExist = singleArgConverter[File](conv = {f =>
    if (!new File(f).isDirectory) {
      log.error(s"$f is not an existing directory")
      throw new IllegalArgumentException()
    }
    new File(f)
  })
  val aipDirectory =
    trailArg[File](
      name = "aip-directory",
      descr = "Directory that will be validated.",
      required = false)

  //TODO: Validate the given directory parameter.
//  validateOpt(aipDirectory)(_.map(file =>
//      if (!file.isDirectory)
//        Left(s"Not a directory '$file'")
//      else
//        Right(()))
//    .getOrElse(Left("Could not parse parameter aip-directory")))

  val username = opt[String]("fcrepo-user",
                    noshort = true,
                    descr = "Username to use for authentication/authorisation to the fedora service.",
                    default = Some(conf.getString("default.fcrepo-user")))
  val password = opt[String]("fcrepo-password",
                    noshort = true,
                    descr = "Password to use for authentication/authorisation to the fedora service",
                    default = Some(conf.getString("default.fcrepo-password")))

  val fedoraServiceUrl = trailArg[URL](name = "fedora-service-url",
    required = false,
    descr = "URL of Fedora Commons Repository Server to connect to ",
    default = Some(new URL(conf.getString("default.fedora-service-url"))))

  val aipBaseDirectory =
    trailArg[File](
      name = "aip-base-directory",
      required = false,
      default = Some(new File(conf.getString("default.aip-base-directory"))))
  //TODO: Validate the given directory parameter.
//  validateOpt(aipBaseDirectory)(_.map(file =>
//    if (!file.isDirectory)
//      Left(s"Not a directory '$file'")
//    else
//      Right(()))
//    .getOrElse(Left("Could not parse parameter aip-base-directory")))

  footer("")
}

  object Version {
    def apply(): String = {
      val properties = new Properties()
      properties.load(getClass.getResourceAsStream("/Version.properties"))
      properties.getProperty("easy-validate-aip.version")
    }
  }

  def askUsername(url: String): String = {
    print(s"Username for $url: ")
    System.console().readLine()
  }

  def askPassword(user: String, url: String): String = {
    print(s"Password for $user on $url: ")
    System.console.readPassword().mkString
  }
}
