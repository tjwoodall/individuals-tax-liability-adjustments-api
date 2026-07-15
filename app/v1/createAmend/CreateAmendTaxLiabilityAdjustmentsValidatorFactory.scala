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

import api.controllers.validators.Validator
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json.JsValue
import v1.createAmend.def1.Def1_CreateAmendTaxLiabilityAdjustmentsValidator
import v1.createAmend.model.request.CreateAmendTaxLiabilityAdjustmentsRequestData

import javax.inject.Singleton

@Singleton
class CreateAmendTaxLiabilityAdjustmentsValidatorFactory {

  def validator(nino: String,
                taxYear: String,
                body: JsValue,
                temporalValidationEnabled: Boolean): Validator[CreateAmendTaxLiabilityAdjustmentsRequestData] = {
    val schema = CreateAmendTaxLiabilityAdjustmentsSchema.schemaFor(taxYear, temporalValidationEnabled)

    schema match {
      case Valid(CreateAmendTaxLiabilityAdjustmentsSchema.Def1) =>
        new Def1_CreateAmendTaxLiabilityAdjustmentsValidator(nino, taxYear, body)
      case Invalid(errors) =>
        Validator.returningErrors(errors)
    }
  }

}
