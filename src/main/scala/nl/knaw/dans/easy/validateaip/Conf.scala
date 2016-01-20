/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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

import java.io.File
import java.net.URL

import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime
import org.rogach.scallop.{ScallopConf, ValueConverter, singleArgConverter}
import org.slf4j.LoggerFactory

class Conf(args: Seq[String], props: PropertiesConfiguration) extends ScallopConf(args) {
  val log = LoggerFactory.getLogger(getClass)

  printedName = "easy-validate-aip"
  version(s"$printedName v${Version()}")
  private val _________ = printedName.map(_ => " ").mkString("")
  banner(s"""
           |Validate one or more AIPs in (dark) archival storage.
           |
           |Usage:
           |
           | $printedName <aip-directory>
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

  val aipDirectory = trailArg[File](
    name = "aip-directory",
    descr = "Directory that will be validated.",
    required = true)(shouldExist)

  val username = opt[String]("fcrepo-user",
    descr = "Username to use for authentication/authorisation to the fedora service",
    noshort = true,
    default = props.getString("default.fcrepo-user") match {
      case s: String => Some(s)
      case _ => Some("")
//      case _ => throw new RuntimeException("No fcrepo-user provided")
    })
  val password = opt[String]("fcrepo-password",
    descr = "Password to use for authentication/authorisation to the fedora service",
    noshort = true,
    default = props.getString("default.fcrepo-password") match {
      case s: String => Some(s)
      case _ => Some("")
      //      case _ => throw new RuntimeException("No fcrepo-password provided")
    })

  val fedoraServiceUrl = opt[URL]("fedora-service-url",
    descr="Fedora service URL",
    noshort = true,
    default = props.getString("default.fedora-service-url") match {
      case s: String => Some(new URL(s))
      case _ => Some(new URL("http://localhost:8080/fedora"))
//      case _ => throw new RuntimeException("No fedora service URL provided")
    })
 

  
}
