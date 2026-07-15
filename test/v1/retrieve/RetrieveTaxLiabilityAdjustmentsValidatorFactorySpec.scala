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

import api.controllers.validators.AlwaysErrorsValidator
import api.utils.UnitSpec
import v1.retrieve.def1.Def1_RetrieveTaxLiabilityAdjustmentsValidator

class RetrieveTaxLiabilityAdjustmentsValidatorFactorySpec extends UnitSpec {

  private def validatorFor(taxYear: String) =
    new RetrieveTaxLiabilityAdjustmentsValidatorFactory().validator(
      nino = "ignoredNino",
      taxYear = taxYear
    )

  "RetrieveTaxLiabilityAdjustmentsValidatorFactory" when {
    "given a request corresponding to a Def1 schema" should {
      "return a Def1 validator" in {
        validatorFor("2026-27") shouldBe a[Def1_RetrieveTaxLiabilityAdjustmentsValidator]
      }
    }

    "given a request where no valid schema could be determined" should {
      "return a validator returning the errors" in {
        validatorFor("BAD_TAX_YEAR") shouldBe an[AlwaysErrorsValidator]
      }
    }
  }

}
