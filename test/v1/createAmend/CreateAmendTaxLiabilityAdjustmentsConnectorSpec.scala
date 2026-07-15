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

import api.connectors.ConnectorSpec
import api.models.domain.{Nino, TaxYear}
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v1.createAmend.def1.fixture.Def1_CreateAmendTaxLiabilityAdjustmentsFixture.requestBodyModel
import v1.createAmend.def1.model.request.Def1_CreateAmendTaxLiabilityAdjustmentsRequestData
import v1.createAmend.model.request.CreateAmendTaxLiabilityAdjustmentsRequestData

import scala.concurrent.Future

class CreateAmendTaxLiabilityAdjustmentsConnectorSpec extends ConnectorSpec {

  private val nino          = "AA123456A"
  private val taxYear       = TaxYear.fromMtd("2026-27")
  private val downstreamUrl = url"$baseUrl/itsa/income-tax/v1/${taxYear.asTysDownstream}/adjustments/tax/$nino"

  trait Test extends ConnectorTest {

    val connector: CreateAmendTaxLiabilityAdjustmentsConnector = new CreateAmendTaxLiabilityAdjustmentsConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val requestData: CreateAmendTaxLiabilityAdjustmentsRequestData =
      Def1_CreateAmendTaxLiabilityAdjustmentsRequestData(Nino(nino), taxYear, requestBodyModel)

    "CreateAmendTaxLiabilityAdjustmentsConnector" should {
      "return a valid response" when {
        "a valid request is made" in new HipTest with Test {
          val outcome: Future[Right[Nothing, ResponseWrapper[Unit]]] =
            Future.successful(Right(ResponseWrapper(correlationId, ())))
          willPut(url = downstreamUrl, body = requestBodyModel).returns(outcome)
          await(connector.createAmendTaxLiabilityAdjustments(requestData)) shouldBe outcome
        }
      }
    }

  }

}
