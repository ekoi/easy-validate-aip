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

  printedName = "validateAip"
  version(s"$printedName ${Version()}")
  banner(
    s"""
       |Validate one or more AIPs in (dark) archival storage.
       |
       |When validating a single AIP the directory containing the AIP is passed as an argument.
       |The program reports back a message stating whether the bag is valid.
       |
       |To validate all the AIPs registered in an EASY Fedora 3.x repository the service URL
       |of the repository and the base directory containing all the AIPs are passed as arguments.
       |The program queries Fedora's Resource Index for all the datasets that have the relation
       |http://dans.knaw.nl/ontologies/relations#storedInDarkArchive set to true.
       |From these datasets the [URN:NBN] identifier is retrieved. This identifier is used to
       |find the AIP directory in the AIP base directory.
       |
       |Usage:
       |
       | single:     $printedName -a <aip-directory>
       | multiple:   $printedName -f <Fedora service URL> -b <aip-base-directory>
       |
       |Options:
       | """.stripMargin)

  val aipDirectory = opt[File](name = "aip-directory",
    short = 'a', descr = "Directory that will be validated.")(fileShouldExist)

  val fedoraServiceUrl = opt[URL](name = "fedora-service-url",
    short = 'f', descr = "URL of Fedora Commons Repository Server to connect to ")

  val aipBaseDirectory = opt[File](name = "aip-base-directory",
    short = 'b', descr = "Base directory containing all the AIPs")(fileShouldExist)

  // either aipDirectory or both fedoraServiceUrl and aipBaseDirectory are supplied
  codependent(fedoraServiceUrl, aipBaseDirectory)
  conflicts(aipDirectory, fedoraServiceUrl :: aipBaseDirectory :: Nil)

  verify()
}

object CommandLineOptions {
  val log = LoggerFactory.getLogger(getClass)

  def parse(args: Array[String]): Settings = {
    log.debug("Parsing command line ...")

    val opts = new ScallopCommandLine(args)

    if (opts.aipDirectory.isSupplied) {
      log.debug("Validate Single AIP...")
      SingleSettings(opts.aipDirectory())
    }
    else {
      assert(opts.fedoraServiceUrl.isSupplied)
      assert(opts.aipBaseDirectory.isSupplied)

      log.debug("Validate Multiple AIPs...")
      MultipleSettings(opts.fedoraServiceUrl(), opts.aipBaseDirectory())
    }
  }
}
