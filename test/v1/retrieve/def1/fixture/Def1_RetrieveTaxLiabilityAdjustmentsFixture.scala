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

package v1.retrieve.def1.fixture

import api.models.domain.Timestamp
import play.api.libs.json.{JsValue, Json}
import v1.retrieve.def1.model.response.{CarryBackLossesDecrease, Def1_RetrieveTaxLiabilityAdjustmentsResponse, TaxRefundedOrSetOff}
import v1.retrieve.model.response.RetrieveTaxLiabilityAdjustmentsResponse

object Def1_RetrieveTaxLiabilityAdjustmentsFixture {

  val responseJson: JsValue = Json.parse(
    """
      |{
      |  "submittedOn": "2026-08-24T14:15:22.544Z",
      |  "carryBackLossesDecrease": {
      |    "incomeTax": 5000.99,
      |    "class4": 5000.99,
      |    "capitalGainsTax": 5000.99
      |  },
      |  "taxRefundedOrSetOff": {
      |     "amount": 5000.99
      |  }
      |}
    """.stripMargin
  )

  val carryBackLossesDecrease: CarryBackLossesDecrease = CarryBackLossesDecrease(
    incomeTax = Some(5000.99),
    class4 = Some(5000.99),
    capitalGainsTax = Some(5000.99)
  )

  val taxRefundedOrSetOff: TaxRefundedOrSetOff = TaxRefundedOrSetOff(amount = Some(5000.99))

  val response: RetrieveTaxLiabilityAdjustmentsResponse = Def1_RetrieveTaxLiabilityAdjustmentsResponse(
    submittedOn = Timestamp("2026-08-24T14:15:22.544Z"),
    carryBackLossesDecrease = Some(carryBackLossesDecrease),
    taxRefundedOrSetOff = Some(taxRefundedOrSetOff))

}
