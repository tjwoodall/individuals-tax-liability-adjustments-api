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

package v1.createAmend.def1.fixture

import play.api.libs.json.{JsValue, Json}
import v1.createAmend.def1.model.request.{CarryBackLossesDecrease, Def1_CreateAmendTaxLiabilityAdjustmentsRequestBody, TaxRefundedOrSetOff}

object Def1_CreateAmendTaxLiabilityAdjustmentsFixture {

  val carryBackLossesDecrease: CarryBackLossesDecrease =
    CarryBackLossesDecrease(
      incomeTax = Some(5000.99),
      class4 = Some(5000.99),
      capitalGainsTax = Some(5000.99)
    )

  val requestBodyJson: JsValue = Json.parse(
    """
      |{
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

  val taxRefundedOrSetOff: TaxRefundedOrSetOff = TaxRefundedOrSetOff(amount = Some(5000.99))

  val requestBodyModel: Def1_CreateAmendTaxLiabilityAdjustmentsRequestBody =
    Def1_CreateAmendTaxLiabilityAdjustmentsRequestBody(
      carryBackLossesDecrease = Some(carryBackLossesDecrease),
      taxRefundedOrSetOff = Some(taxRefundedOrSetOff))

}
