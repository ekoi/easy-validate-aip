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
    if (s.singleAip) {
      validateSingleAip(s.aipDir)
    } else {
      val queryResult = queryUrn
      if(queryResult.isSuccess) {
        val urns = queryResult.get
        log.debug(s"Number of urn's ${urns.size}")
        validateMultiAips(s.aipBaseDir.getPath, urns)
        Success(Unit)
      } else Failure(new RuntimeException("Failed to query fedora resource index."))
    }
  }

  def validateSingleAip(f:File): Try[Unit] = {
    log.debug("Validate a single AIP.")
    log.debug(s"Validate bag of ${f.getPath}")
    if (f.list().size != 1)
      Failure(new RuntimeException(s"${f.getPath} directory contains multiple files/directories."))
    else {
      val bag = bagFactory.createBag(f.listFiles()(0), BagFactory.Version.V0_97, BagFactory.LoadOption.BY_MANIFESTS)
     val validationResult: SimpleResult = bag.verifyValid()
      if (validationResult.isSuccess) Success(Unit)
      else Failure(new RuntimeException(s"${f.getPath} is not valid."))
  }



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

  def queryUrn(implicit s: Settings): Try[List[String]] = Try {
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
           |where { ?s <http://dans.knaw.nl/ontologies/relations#storedInDarkArchive> true  } limit 10
        """.stripMargin)
      .asString
    if (response.code != 200)
      throw new RuntimeException(s"Failed to query fedora resource index ($url), response code: ${response.code}")
    response.body.lines.toList.drop(1)
      .map(_.replace("info:fedora/", ""))
  }
}
