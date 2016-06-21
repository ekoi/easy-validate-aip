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

import java.io.File
import java.net.URL

import org.rogach.scallop._
import org.slf4j.LoggerFactory
import nl.knaw.dans.easy.validateaip.CommandLineOptions._

class ScallopCommandLine(args: Array[String]) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  val fileShouldExist = singleArgConverter[File](conv = { f =>
    if (!new File(f).isDirectory) {
      log.error(s"$f is not an existing directory")
      throw new IllegalArgumentException()
    }
    new File(f)
  })

  printedName = "ValidateAip"
  version(s"$printedName ${Version()}")
  banner(
    s"""
       |Validate one or more AIPs in (dark) archival storage.
       |
       |Usage:
       |
       | $printedName <aip-directory>
       | $printedName <Fedora service URL> <aip-base-directory>
       |
       |Options:
       | """.stripMargin)

  val aipDirectory = trailArg[File](name = "aip-directory",
    descr = "Directory that will be validated.",
    required = false)(fileShouldExist)

  val fedoraServiceUrl = trailArg[URL](name = "fedora-service-url",
    required = false,
    descr = "URL of Fedora Commons Repository Server to connect to ")

  val aipBaseDirectory = trailArg[File](name = "aip-base-directory",
    required = false,
    descr = "")(fileShouldExist) // TODO fill in this value

  footer("")
  verify()
}

object CommandLineOptions {
  val log = LoggerFactory.getLogger(getClass)

  def parse(args: Array[String]): Settings = {
    log.debug("Parsing command line ...")
    val homeDir = new File(System.getProperty("app.home"))

    val opts = new ScallopCommandLine(args)

    if (args.length == 1) {
      log.debug("Validate Single AIP...")
      SingleSettings(opts.aipDirectory())
    }
    else {
      log.debug("Validate Multiple AIPs...")
      MultipleSettings(opts.fedoraServiceUrl(), opts.aipBaseDirectory())
    }
  }
}
