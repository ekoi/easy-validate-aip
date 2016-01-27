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

import com.yourmediashelf.fedora.client.FedoraCredentials
import gov.loc.repository.bagit.BagFactory
import gov.loc.repository.bagit.utilities.SimpleResult
import scala.util.{Failure, Success, Try}
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory
import scalaj.http.Http


object EasyValidateAip {
  val log = LoggerFactory.getLogger(getClass)
  implicit val bagFactory = new BagFactory

  def main(args: Array[String]) {
//    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
//    val conf = new Conf(args, props)
    implicit val settings: Settings = CommandLineOptions.parse(args)

    run match {
      case Success(_) => log.info("AIP validation SUCCESS")
      case Failure(t) => log.error("AIP validation FAIL", t)
    }
  }

  def run(implicit s: Settings): Try[Unit] = {
    log.debug(s"Settings = $s")
    validateAip
  }
  
  def validateAip(implicit s: Settings): Try[Unit] = {
    log.debug(s"[$s] Validate bag of " + s.aipDir.getPath)
    if (s.singleAip) {
      validateSingleAip(s)
    } else {
      log.debug("Validata all the AIPs registered in an EASY Fedora 3.x repository")
      val r = queryUrn
      Success(Unit)
    }
  }

  def validateSingleAip(s: Settings): Try[Unit] = {
    log.debug("Validate a single AIP.")
    if (s.aipDir.list().size != 1) Failure(new RuntimeException(s"${s.aipDir} directory contains multiple files/directories."))
    else {
      val bag = bagFactory.createBag(s.aipDir.listFiles()(0), BagFactory.Version.V0_97, BagFactory.LoadOption.BY_MANIFESTS)
      val validationResult: SimpleResult = bag.verifyValid

      if (validationResult.isSuccess) Success(Unit)

      else Failure(new RuntimeException(s"${s.aipDir} is not valid."))
    }
  }

  def queryUrn()(implicit s: Settings): Try[List[String]] = Try {
    val fedoraCredentials = new FedoraCredentials(s.fedoraUrl, s.username, s.password)

    val url = s"${s.fedoraUrl}/risearch"
    val response = Http(url)
      .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
      .param("type", "tuples")
      .param("lang", "sparql")
      .param("format", "CSV")
      .param("query",
        s"""
           |select ?s
           |from <#ri>
           |where { ?s <http://dans.knaw.nl/ontologies/relations#storedInDarkArchive> true  }
        """.stripMargin)
      .asString
    log.debug(s"Response code: $response.code")
    if (response.code != 200)
      throw new RuntimeException(s"Failed to query fedora resource index ($url), response code: ${response.code}")
    response.body.lines.toList.drop(1)
      .map(_.replace("info:fedora/", ""))
  }
}
