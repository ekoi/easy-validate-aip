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

import nl.knaw.dans.easy.validateaip.EasyValidateAip.{validateMultiAips, validateSingleAip}
import org.scalatest.{FlatSpec, Matchers}


class EasyValidateAipSpec extends FlatSpec with Matchers {
  System.setProperty("app.home", "src/main/assembly/dist")

  "validateSingleAip" should "succeed" in {
    validateSingleAip(new File("src/test/resources/single/valid")).isSuccess shouldBe true
  }

  it should "fail" in {
    validateSingleAip(new File("src/test/resources/single/invalid/aip-simple-invalid")).isFailure shouldBe true
  }

  it should "fail. The directory is empty" in {
   validateSingleAip(new File("src/test/resources/single/invalid/empty-dir")).isFailure shouldBe true
  }

  it should "fail. The directory contains multiple directories" in {
    validateSingleAip(new File("src/test/resources/single/invalid/multiple-dirs")).isFailure shouldBe true
  }

  "validateMultiAips" should "succeed" in {
    validateMultiAips("src/test/resources/multiple/valid-bagit", mockUrnQueryResponseValidBagit()).size shouldBe 0
  }

  def mockUrnQueryResponseValidBagit():List[String]={
    List("urn:nbn:nl:ui:13-5xhe-sn", "urn:nbn:nl:ui:13-6eub-aq", "urn:nbn:nl:ui:13-6xee-rq")
  }

  def mockUrnQueryResponseInvalidBagit():List[String]={
    List("urn:nbn:nl:ui:13-axz88-sa", "urn:nbn:nl:ui:13-10imka-12")
  }
}
