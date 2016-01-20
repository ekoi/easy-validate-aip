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

import java.io._

import gov.loc.repository.bagit.BagFactory
import gov.loc.repository.bagit.utilities.SimpleResult
import scala.util.{Failure, Success, Try}
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory


object EasyValidateAip {
  val log = LoggerFactory.getLogger(getClass)
  implicit val bagFactory = new BagFactory

  def main(args: Array[String]) {
    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
    val conf = new Conf(args, props)
    implicit val s = Settings(conf.aipDirectory(), conf.username(), conf.password(), conf.fedoraServiceUrl())

    run match {
      case Success(_) => log.info("AIP validation SUCCESS")
      case Failure(t) => log.error("AIP validation FAIL", t)
    }
  }

  def run(implicit s: Settings): Try[Unit] = {
    log.debug(s"Settings = $s")
    for {
      bagDir <- validateBag
     
    } yield ()
  }
  
  private def validateBag(implicit s: Settings): Try[Unit] = {
    log.debug(s"[$s] Validate bag of " + s.aipDir.getPath)
    val bag = bagFactory.createBag(s.aipDir.listFiles()(0), BagFactory.Version.V0_97, BagFactory.LoadOption.BY_MANIFESTS)
    val validationResult: SimpleResult = bag.verifyValid
    if (validationResult.isSuccess) Success(Unit)
    else {
      Failure(new RuntimeException(s"${s.aipDir} is not valid."))

    }
  }

  
}
