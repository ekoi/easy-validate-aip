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

import java.io._
import java.net.URL

import gov.loc.repository.bagit.BagFactory
import gov.loc.repository.bagit.utilities.SimpleResult
import scala.util.{Failure, Success, Try}
import javax.ws.rs.core.Response.Status
import org.slf4j.LoggerFactory
import scalaj.http.Http
import nl.knaw.dans.easy.validateaip.{CommandLineOptions => cmd}


object EasyValidateAip {
  implicit val log = LoggerFactory.getLogger(getClass)
  implicit val bagFactory = new BagFactory

  def main(args: Array[String]) {
    log.debug("Starting application.")
    implicit val settings: Settings = cmd.parse(args)

    run match {
      case Success(_) => log.info("AIP validation SUCCESS")
      case Failure(t) => log.error("AIP validation FAIL", t)
    }
  }

  def run(implicit s: Settings): Try[Result] = {
    log.debug(s"Settings = $s")
    validateAip
  }
  
  def validateAip(implicit s: Settings): Try[Result] = {
    s match {
      case SingleSettings(aipDir) => validateSingleAip(aipDir)
      case MultipleSettings(fedoraUrl, aipBaseDir) => queryUrn(fedoraUrl).map(urns => {
        log.info(s"Number of urns to be validated: ${urns.size}")
        val invalidUrns = validateMultiAips(aipBaseDir.getPath, urns)
        log.info(s"Number of invalid urns: ${invalidUrns.size}")
        if (invalidUrns.isEmpty)
          Success(Result(valid = true))
        else
          Success(Result(valid = false, invalidUrns))
      }).getOrElse(Failure(new RuntimeException("Failed to query fedora resource index.")))
    }
  }

  def validateSingleAip(f:File): Try[Result] = {
    log.info(s"Validate bag of ${f.getPath}")
    if (!f.exists()) {
      log.info(s"${f.getPath} doesn't exist.")
      return Failure(new RuntimeException(s"${f.getPath} doesn't exist."))
    }
    val directoryToValidate = f.listFiles().filter(_.isDirectory) //in urn:nbn:xxx directory contains  deposit.properties file, ignore it.
    if (directoryToValidate.isEmpty)
      Failure(new RuntimeException(s"${f.getPath} directory is empty."))
    else if (directoryToValidate.length == 1) {
      val bag = bagFactory.createBag(directoryToValidate(0), BagFactory.Version.V0_97, BagFactory.LoadOption.BY_MANIFESTS)
      val validationResult: SimpleResult = bag.verifyValid()
      if (validationResult.isSuccess) Success(Result(valid = true))
      else Success(Result(valid = false, List(f.getPath)))
    }
    else
      Failure(new RuntimeException(s"${f.getPath} directory contains multiple directories."))
  }


  def validateMultiAips(aipBaseDir: String, urns: List[String]) : List[String] = {
    log.debug("Validate multiple AIPs")
    log.debug("Validate all the AIPs registered in an EASY Fedora 3.x repository")

    for {
      urn <- urns
      validate = validateSingleAip(new File(s"$aipBaseDir/$urn"))
      if validate.isFailure
    } yield urn
  }

  def queryUrn(fedoraUrl: URL): Try[List[String]] = Try {
    val url = s"$fedoraUrl/risearch"
    log.debug(s"fedora server url: $url")
    val response = Http(url)
      .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
      .param("type", "tuples")
      .param("lang", "sparql")
      .param("format", "CSV")
      .param("query",
        s"""
           |select ?pid
           |from <#ri>
           |where { ?s <http://dans.knaw.nl/ontologies/relations#storedInDarkArchive> 'true' .
           |        ?s <http://dans.knaw.nl/ontologies/relations#hasPid> ?pid . }
        """.stripMargin)
      .asString

    if (!response.code.equals(Status.OK.getStatusCode))
      throw new RuntimeException(s"Failed to query fedora resource index ($url), response code: ${response.code}")

    response.body.lines.toList.drop(1)
      .map(_.replace("info:fedora/", ""))
  }


  case class Result(valid: Boolean, invalidAips: List[String] = List())
}
