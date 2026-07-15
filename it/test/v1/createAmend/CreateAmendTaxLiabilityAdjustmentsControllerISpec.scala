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

package v1.createAmend

import api.models.domain.TaxYear
import api.models.errors.*
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import v1.createAmend.def1.fixture.Def1_CreateAmendTaxLiabilityAdjustmentsFixture.requestBodyJson

import scala.math.Ordering.Implicits.infixOrderingOps

class CreateAmendTaxLiabilityAdjustmentsControllerISpec extends IntegrationBaseSpec {

  private val parsedTaxYear: TaxYear   = TaxYear.fromMtd("2026-27")
  private val notEndedTaxYear: TaxYear = parsedTaxYear.max(TaxYear.currentTaxYear)

  private val invalidRequestJsonNegativeFields: JsValue = Json.parse(
    """
      |{
      |  "carryBackLossesDecrease": {
      |    "incomeTax": -5000.99,
      |    "class4": -5000.99,
      |    "capitalGainsTax": -5000.99
      |  }
      |}
    """.stripMargin
  )

  "Calling the Create or Amend Tax Liability Adjustments endpoint" should {
    "return a 204 status code" when {
      "any valid request is made with a supported tax year that has not ended and suspendTemporalValidations is true" in new Test {
        override val taxYear: String       = notEndedTaxYear.asMtd
        override def downstreamUri: String = s"/itsa/income-tax/v1/${notEndedTaxYear.asTysDownstream}/adjustments/tax/$nino"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.authorised()
          DownstreamStub.onSuccess(
            method = DownstreamStub.PUT,
            uri = downstreamUri,
            status = NO_CONTENT,
            body = JsObject.empty
          )
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
        response.header("Content-Type") shouldBe None
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {
            override val nino: String                       = requestNino
            override val taxYear: String                    = requestTaxYear
            override val suspendTemporalValidations: String = (expectedBody != RuleTaxYearNotEndedError).toString

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              MtdIdLookupStub.ninoFound(nino)
              AuthStub.authorised()
            }

            val response: WSResponse = await(request().put(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = List(
          ("AA1123A", "2026-27", requestBodyJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "invalid", requestBodyJson, BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2025-27", requestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", "2025-26", requestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", notEndedTaxYear.asMtd, requestBodyJson, BAD_REQUEST, RuleTaxYearNotEndedError),
          ("AA123456A", "2026-27", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          (
            "AA123456A",
            "2026-27",
            invalidRequestJsonNegativeFields,
            BAD_REQUEST,
            ValueFormatError.withPaths(
              Seq(
                "/carryBackLossesDecrease/incomeTax",
                "/carryBackLossesDecrease/class4",
                "/carryBackLossesDecrease/capitalGainsTax"
              )
            )
          )
        )

        input.foreach(validationErrorTest.tupled)
      }
    }
    "downstream service error" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {
          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            MtdIdLookupStub.ninoFound(nino)
            AuthStub.authorised()
            DownstreamStub.onError(
              method = DownstreamStub.PUT,
              uri = downstreamUri,
              errorStatus = downstreamStatus,
              errorBody = errorBody(downstreamCode)
            )
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError),
        (UNPROCESSABLE_ENTITY, "INVALID_SUBMISSION", BAD_REQUEST, RuleTaxYearNotEndedError),
        (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", INTERNAL_SERVER_ERROR, InternalError),
        (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindowError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
      )

      errors.foreach(serviceErrorTest.tupled)
    }
  }

  private trait Test {
    val nino: String                       = "AA123456A"
    val taxYear: String                    = parsedTaxYear.asMtd
    val suspendTemporalValidations: String = "true"

    def setupStubs(): StubMapping

    private def mtdUri: String = s"/$nino/$taxYear"

    def request(): WSRequest = {
      AuthStub.resetAll()
      setupStubs()

      buildRequest(mtdUri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123"),
          ("suspend-temporal-validations", suspendTemporalValidations)
        )
    }

    def downstreamUri: String = s"/itsa/income-tax/v1/${parsedTaxYear.asTysDownstream}/adjustments/tax/$nino"

    def errorBody(code: String): String =
      s"""
         |{
         |  "origin": "HoD",
         |  "response": {
         |    "failures": [
         |      {
         |        "type": "$code",
         |        "reason": "downstream message"
         |      }
         |    ]
         |  }
         |}
      """.stripMargin

  }

}
