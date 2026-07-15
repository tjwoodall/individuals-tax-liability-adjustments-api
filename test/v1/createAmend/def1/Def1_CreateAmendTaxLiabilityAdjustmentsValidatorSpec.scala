/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.createAmend.def1

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.utils.JsonErrorValidators
import api.utils.UnitSpec
import play.api.libs.json.*
import v1.createAmend.def1.fixture.Def1_CreateAmendTaxLiabilityAdjustmentsFixture.*
import v1.createAmend.def1.model.request.Def1_CreateAmendTaxLiabilityAdjustmentsRequestData

class Def1_CreateAmendTaxLiabilityAdjustmentsValidatorSpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "someCorrelationId"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2026-27"

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private def validate(nino: String = validNino, body: JsValue = requestBodyJson) =
    new Def1_CreateAmendTaxLiabilityAdjustmentsValidator(nino, validTaxYear, body).validateAndWrapResult()

  private def error(mtdError: MtdError) = Left(ErrorWrapper(correlationId, mtdError))

  "Def1_CreateAmendTaxLiabilityAdjustmentsValidator" should {
    "return the parsed object" when {
      "a valid request is supplied" in {
        validate(validNino, requestBodyJson) shouldBe
          Right(
            Def1_CreateAmendTaxLiabilityAdjustmentsRequestData(
              parsedNino,
              parsedTaxYear,
              requestBodyModel
            ))
      }
    }

    "return a NinoFormatError" when {
      "an invalid nino is supplied" in {
        validate(nino = "A12344A") shouldBe error(NinoFormatError)
      }
    }

    "return a RuleIncorrectOrEmptyBodyError" when {
      "an empty Json body is submitted" in {
        validate(body = JsObject.empty) shouldBe error(RuleIncorrectOrEmptyBodyError)
      }

      "a non-empty JSON body is submitted without any expected fields" in {
        validate(body = Json.parse("""{"field": "value"}""")) shouldBe error(RuleIncorrectOrEmptyBodyError)
      }

      requestBodyJson.collectPaths().foreach { path =>
        s"the submitted request body has field $path with incorrect type" in {
          val invalidJson: JsValue = requestBodyJson.update(path, JsBoolean(true))

          validate(body = invalidJson) shouldBe error(RuleIncorrectOrEmptyBodyError.withPath(path))
        }
      }

      "the submitted request body has empty carryBackLossesDecrease objects" in {
        val invalidJson: JsValue = requestBodyJson
          .replaceWithEmptyObject("/carryBackLossesDecrease")

        validate(body = invalidJson) shouldBe error(
          RuleIncorrectOrEmptyBodyError.withPaths(
            Seq(
              "/carryBackLossesDecrease"
            )
          )
        )
      }
    }

    "return ValueFormatError" when {
      Seq(
        "/carryBackLossesDecrease/incomeTax",
        "/carryBackLossesDecrease/class4",
        "/carryBackLossesDecrease/capitalGainsTax",
        "/taxRefundedOrSetOff/amount"
      ).foreach { path =>
        s"the submitted request body has field $path with an invalid value" in {
          val invalidJson: JsValue = requestBodyJson.update(path, JsNumber(-500.99))

          validate(body = invalidJson) shouldBe error(ValueFormatError.withPath(path))
        }
      }
    }
  }

}
