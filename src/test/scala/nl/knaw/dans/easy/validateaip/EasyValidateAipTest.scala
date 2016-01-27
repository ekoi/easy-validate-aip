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

import nl.knaw.dans.easy.validateaip.EasyValidateAip.validateSingleAip
import org.scalatest.{Matchers, FlatSpec, FunSuite}

import java.io.File



class EasyValidateAipTest extends FlatSpec with Matchers {
  System.setProperty("app.home", "src/main/assembly/dist")

  "validateSingleAip" should "Success" in {
    validateSingleAip(new Settings(new File("src/test/resources/simple"))).isSuccess shouldBe true
  }

  it should "Failure" in {
    validateSingleAip(new Settings(new File("src/test/resources/simple-invalid"))).isFailure shouldBe true
  }

  it should "produce java.lang.NullPointerException" in {
    val thrown = intercept[java.lang.NullPointerException] {
      validateSingleAip(new Settings(new File("src/test/resources/simple-")))
    }
    thrown.isInstanceOf[java.lang.NullPointerException]
  }

  it should "failure sinds the directory contains multiple files/directories." in {
   validateSingleAip(new Settings(new File("src/test/resources/simple/aip-simple"))).isFailure shouldBe true
  }


}