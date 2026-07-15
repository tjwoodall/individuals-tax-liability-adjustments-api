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

import api.controllers.ControllerTestRunner
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{ErrorWrapper, InternalError, NinoFormatError}
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.mvc.Result
import v1.retrieve.def1.fixture.Def1_RetrieveTaxLiabilityAdjustmentsFixture.*
import v1.retrieve.def1.model.request.Def1_RetrieveTaxLiabilityAdjustmentsRequestData
import v1.retrieve.model.request.RetrieveTaxLiabilityAdjustmentsRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveTaxLiabilityAdjustmentsControllerSpec
    extends ControllerTestRunner
    with MockRetrieveTaxLiabilityAdjustmentsService
    with MockRetrieveTaxLiabilityAdjustmentsValidatorFactory {

  private val taxYear = "2026-27"

  "RetrieveTaxLiabilityAdjustmentsController" should {
    "return 200 (OK) status" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveTaxLiabilityAdjustmentsService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(responseJson))
      }
    }

    "return validation error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveTaxLiabilityAdjustmentsService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, InternalError))))

        runErrorTest(InternalError)
      }
    }
  }

  trait Test extends ControllerTest {

    protected val controller: RetrieveTaxLiabilityAdjustmentsController = new RetrieveTaxLiabilityAdjustmentsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveTaxLiabilityAdjustmentsValidatorFactory,
      service = mockRetrieveTaxLiabilityAdjustmentsService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns true

    protected def callController(): Future[Result] = controller.retrieve(validNino, taxYear)(fakeGetRequest)

    protected val requestData: RetrieveTaxLiabilityAdjustmentsRequestData =
      Def1_RetrieveTaxLiabilityAdjustmentsRequestData(Nino(validNino), TaxYear.fromMtd(taxYear))

  }

}
