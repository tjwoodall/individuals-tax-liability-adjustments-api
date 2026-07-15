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

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.ResolveParsedNumber
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits.*
import v1.createAmend.def1.model.request.*

object Def1_CreateAmendTaxLiabilityAdjustmentsRulesValidator extends RulesValidator[Def1_CreateAmendTaxLiabilityAdjustmentsRequestData] {

  private val resolveNonNegativeParsedNumber = ResolveParsedNumber()

  def validateBusinessRules(
      parsed: Def1_CreateAmendTaxLiabilityAdjustmentsRequestData): Validated[Seq[MtdError], Def1_CreateAmendTaxLiabilityAdjustmentsRequestData] = {
    import parsed.*

    combine(
      validateCarryBackLossesDecrease(body.carryBackLossesDecrease),
      validateTaxRefundedOrSetOff(body.taxRefundedOrSetOff)
    ).onSuccess(parsed)
  }

  private def validateCarryBackLossesDecrease(carryBackLossesDecrease: Option[CarryBackLossesDecrease]): Validated[Seq[MtdError], Unit] = {
    carryBackLossesDecrease.fold(valid) { carryBackLossesDecrease =>
      List(
        (carryBackLossesDecrease.incomeTax, "/carryBackLossesDecrease/incomeTax"),
        (carryBackLossesDecrease.class4, "/carryBackLossesDecrease/class4"),
        (carryBackLossesDecrease.capitalGainsTax, "/carryBackLossesDecrease/capitalGainsTax")
      ).traverse_ { case (value, path) =>
        resolveNonNegativeParsedNumber(value, path)
      }
    }
  }

  private def validateTaxRefundedOrSetOff(taxRefundedOrSetOff: Option[TaxRefundedOrSetOff]): Validated[Seq[MtdError], Unit] = {
    taxRefundedOrSetOff.fold(valid) { taxRefundedOrSetOff =>
      List(
        (taxRefundedOrSetOff.amount, "/taxRefundedOrSetOff/amount")
      ).traverse_ { case (value, path) =>
        resolveNonNegativeParsedNumber(value, path)
      }
    }
  }

}
