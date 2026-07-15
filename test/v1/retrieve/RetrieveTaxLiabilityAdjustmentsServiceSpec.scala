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

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v1.retrieve.def1.fixture.Def1_RetrieveTaxLiabilityAdjustmentsFixture.response
import v1.retrieve.def1.model.request.Def1_RetrieveTaxLiabilityAdjustmentsRequestData
import v1.retrieve.model.request.RetrieveTaxLiabilityAdjustmentsRequestData

import scala.concurrent.Future

class RetrieveTaxLiabilityAdjustmentsServiceSpec extends ServiceSpec {

  "RetrieveTaxLiabilityAdjustmentsService" should {
    "return correct result for a success" when {
      "using schema Def1" in new Test {
        MockRetrieveTaxLiabilityAdjustmentsConnector
          .retrieveTaxLiabilityAdjustments(requestData)
          .returns(
            Future.successful(Right(ResponseWrapper(correlationId, response)))
          )

        await(service.retrieveTaxLiabilityAdjustments(requestData)).shouldBe(
          Right(ResponseWrapper(correlationId, response))
        )
      }
    }

    "map errors according to spec" when {

      def serviceError(downStreamErrorCode: String, error: MtdError): Unit =
        s"a $downStreamErrorCode error is returned from the service" in new Test {
          MockRetrieveTaxLiabilityAdjustmentsConnector
            .retrieveTaxLiabilityAdjustments(requestData)
            .returns(
              Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downStreamErrorCode)))))
            )

          await(service.retrieveTaxLiabilityAdjustments(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errorMap = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "INVALID_CORRELATION_ID"    -> InternalError,
        "NO_DATA_FOUND"             -> NotFoundError,
        "TAX_YEAR_NOT_SUPPORTED"    -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      errorMap.foreach(args => serviceError.tupled(args))
    }

  }

  trait Test extends MockRetrieveTaxLiabilityAdjustmentsConnector {
    val service = new RetrieveTaxLiabilityAdjustmentsService(mockConnector)

    val requestData: RetrieveTaxLiabilityAdjustmentsRequestData =
      Def1_RetrieveTaxLiabilityAdjustmentsRequestData(
        Nino("AA123456A"),
        TaxYear.fromMtd("2026-27")
      )

  }

}
