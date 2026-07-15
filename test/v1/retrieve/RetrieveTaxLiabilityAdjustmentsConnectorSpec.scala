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

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{DownstreamErrorCode, DownstreamErrors}
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v1.retrieve.def1.fixture.Def1_RetrieveTaxLiabilityAdjustmentsFixture.response
import v1.retrieve.def1.model.request.Def1_RetrieveTaxLiabilityAdjustmentsRequestData
import v1.retrieve.model.request.RetrieveTaxLiabilityAdjustmentsRequestData
import v1.retrieve.model.response.RetrieveTaxLiabilityAdjustmentsResponse

import java.net.URL
import scala.concurrent.Future

class RetrieveTaxLiabilityAdjustmentsConnectorSpec extends ConnectorSpec {

  private val nino: String       = "AA123456A"
  private val taxYear: String    = "2026-27"
  private val downstreamUrl: URL = url"$baseUrl/itsa/income-tax/v1/26-27/adjustments/tax/$nino"

  trait Test extends ConnectorTest {

    val connector: RetrieveTaxLiabilityAdjustmentsConnector =
      new RetrieveTaxLiabilityAdjustmentsConnector(http = mockHttpClient, appConfig = mockAppConfig)

    val requestData: RetrieveTaxLiabilityAdjustmentsRequestData =
      Def1_RetrieveTaxLiabilityAdjustmentsRequestData(Nino(nino), TaxYear.fromMtd(taxYear))

  }

  "RetrieveTaxLiabilityAdjustmentsConnector" should {
    "return a valid response" when {
      "a valid request is made" in new HipTest with Test {

        willGet(url = downstreamUrl).returns(
          Future.successful(Right(ResponseWrapper(correlationId, response)))
        )

        await(connector.retrieveTaxLiabilityAdjustments(requestData)).shouldBe(
          Right(ResponseWrapper(correlationId, response))
        )
      }
    }
    "return an error" when {
      "downstream returns an error for a request" in new HipTest with Test {

        val outcome: Left[ResponseWrapper[DownstreamErrors], Nothing] =
          Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode("SOME_ERROR"))))

        willGet(url = downstreamUrl).returns(Future.successful(outcome))

        val result: DownstreamOutcome[RetrieveTaxLiabilityAdjustmentsResponse] =
          await(connector.retrieveTaxLiabilityAdjustments(requestData))

        result shouldBe outcome
      }
    }

  }

}
