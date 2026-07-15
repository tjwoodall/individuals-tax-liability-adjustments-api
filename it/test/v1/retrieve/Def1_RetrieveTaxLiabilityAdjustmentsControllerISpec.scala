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

package v1.retrieve

import api.models.errors.*
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import v1.retrieve.def1.fixture.Def1_RetrieveTaxLiabilityAdjustmentsFixture.responseJson

class Def1_RetrieveTaxLiabilityAdjustmentsControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String          = "AA123456A"
    def taxYear: String       = "2026-27"
    val responseBody: JsValue = responseJson

    def downstreamUri: String = s"/itsa/income-tax/v1/26-27/adjustments/tax/$nino"

    def request(): WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()
      buildRequest(s"/$nino/$taxYear")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

    def setupStubs(): Unit = ()

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

  "Calling the Retrieve Tax Liability Adjustments endpoint" should {
    "return a 200 status code" when {
      "successful request is made" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.GET,
          uri = downstreamUri,
          status = OK,
          body = responseJson
        )

        val response: WSResponse = await(request().get())
        response.json shouldBe responseBody
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {
      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {
            override val nino: String    = requestNino
            override val taxYear: String = requestTaxYear

            val response: WSResponse = await(request().get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = List(
          ("AA1123A", "2026-27", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "invalid", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2025-27", BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", "2025-26", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        input.foreach(args => validationErrorTest.tupled(args))
      }

      "downstream service error" when {
        "return mapped downstream service error" when {
          def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
            s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {
              override def setupStubs(): Unit = DownstreamStub.onError(
                method = DownstreamStub.GET,
                uri = downstreamUri,
                errorStatus = downstreamStatus,
                errorBody = errorBody(downstreamCode)
              )

              val response: WSResponse = await(request().get())
              response.status shouldBe expectedStatus
              response.json shouldBe Json.toJson(expectedBody)
              response.header("X-CorrelationId").nonEmpty shouldBe true
              response.header("Content-Type") shouldBe Some("application/json")
            }
          }

          val input = List(
            (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
            (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
            (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
            (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError),
            (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
            (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", INTERNAL_SERVER_ERROR, InternalError),
            (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
            (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
          )

          input.foreach(args => serviceErrorTest.tupled(args))
        }
      }
    }
  }

}
