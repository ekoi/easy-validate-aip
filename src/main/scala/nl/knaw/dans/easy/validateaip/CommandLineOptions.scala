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

import org.rogach.scallop._
import org.slf4j.LoggerFactory

import scala.util.Failure

class CommandLineOptions (args: Array[String]) extends ScallopConf(args){
  val log = LoggerFactory.getLogger(getClass)
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
      required = false)(shouldExist)

  val username = opt[String]("fcrepo-user", noshort = true, descr = "Username to use for authentication/authorisation to the fedora service.")
  val password = opt[String]("fcrepo-password", noshort = true, descr = "Password to use for authentication/authorisation to the fedora service")
  val fedoraServiceUrl = trailArg[URL](name = "fedora-service-url",
    required = false,
    descr = "URL of Fedora Commons Repository Server to connect to ")

//  ,
//    default = Some(new URL("http://localhost:8080/fedora")))

  val aipBaseDirectory =
    trailArg[File](
      name = "aip-base-directory",
      required = false)(shouldExist)

  footer("")
}

object CommandLineOptions {
  def parse(args: Array[String]): Settings = {
    val opts = new CommandLineOptions(args)
    if (args.contains("--help"))
      Array[String]()
    if (args.size == 0) {
      print("No arguments provided. More info: easy-validate-aip --help")
      throw new RuntimeException("No arguments provided. More info: easy-validate-aip --help")
    }else if (args.length == 1) {
      val aipDir = opts.aipDirectory.apply()
      new Settings(aipDir)
    }
    else {
      val fedoraUrl: URL = opts.fedoraServiceUrl.apply()
      val username: String = opts.username.get.getOrElse(askUsername(fedoraUrl.toString))
      val password: String = opts.password.get.getOrElse(askPassword(username,fedoraUrl.toString))

      val aipBaseDir = opts.aipBaseDirectory.apply()

      new Settings(username, password, fedoraUrl, aipBaseDir)
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
