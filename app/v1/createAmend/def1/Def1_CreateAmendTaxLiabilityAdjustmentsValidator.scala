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

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveNino, ResolveNonEmptyJsonObject}
import api.models.domain.TaxYear
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits.catsSyntaxTuple2Semigroupal
import play.api.libs.json.JsValue
import v1.createAmend.def1.Def1_CreateAmendTaxLiabilityAdjustmentsRulesValidator.validateBusinessRules
import v1.createAmend.def1.model.request.{Def1_CreateAmendTaxLiabilityAdjustmentsRequestBody, Def1_CreateAmendTaxLiabilityAdjustmentsRequestData}
import v1.createAmend.model.request.CreateAmendTaxLiabilityAdjustmentsRequestData

import javax.inject.Inject

class Def1_CreateAmendTaxLiabilityAdjustmentsValidator @Inject() (nino: String, taxYear: String, body: JsValue)
    extends Validator[CreateAmendTaxLiabilityAdjustmentsRequestData] {

  private lazy val resolveJson = new ResolveNonEmptyJsonObject[Def1_CreateAmendTaxLiabilityAdjustmentsRequestBody]()

  override def validate: Validated[Seq[MtdError], CreateAmendTaxLiabilityAdjustmentsRequestData] =
    (
      ResolveNino(nino),
      resolveJson(body)
    ).mapN { (validNino, validBody) =>
      Def1_CreateAmendTaxLiabilityAdjustmentsRequestData(
        nino = validNino,
        taxYear = TaxYear.fromMtd(taxYear),
        body = validBody
      )
    } andThen validateBusinessRules

}
