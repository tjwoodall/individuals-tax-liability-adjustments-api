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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{ErrorWrapper, NinoFormatError, RuleOutsideAmendmentWindowError}
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.test.Helpers.PUT
import v1.createAmend.def1.fixture.Def1_CreateAmendTaxLiabilityAdjustmentsFixture.*
import v1.createAmend.def1.model.request.*
import v1.createAmend.model.request.CreateAmendTaxLiabilityAdjustmentsRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendTaxLiabilityAdjustmentsControllerSpec
    extends ControllerTestRunner
    with MockCreateAmendTaxLiabilityAdjustmentsService
    with MockCreateAmendTaxLiabilityAdjustmentsValidatorFactory {

  private val taxYear = "2026-27"

  "CreateAmendTaxLiabilityAdjustmentsController" should {
    "return 204 (NO_CONTENT) status" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))
        MockCreateAmendTaxLiabilityAdjustmentsService
          .createAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))
        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }

    "return validation error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))
        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))
        MockCreateAmendTaxLiabilityAdjustmentsService
          .createAmend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleOutsideAmendmentWindowError))))

        runErrorTestWithAudit(RuleOutsideAmendmentWindowError, Some(requestBodyJson))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendTaxLiabilityAdjustments",
        transactionName = "create-amend-tax-liability-adjustments",
        detail = GenericAuditDetail(
          userType = "Individual",
          versionNumber = apiVersion.name,
          agentReferenceNumber = None,
          params = Map("nino" -> validNino, "taxYear" -> taxYear),
          `X-CorrelationId` = correlationId,
          requestBody = Some(requestBodyJson),
          auditResponse = auditResponse
        )
      )

    protected val controller: CreateAmendTaxLiabilityAdjustmentsController =
      new CreateAmendTaxLiabilityAdjustmentsController(
        authService = mockEnrolmentsAuthService,
        lookupService = mockMtdIdLookupService,
        service = mockCreateAmendTaxLiabilityAdjustmentsService,
        validatorFactory = mockCreateAmendTaxLiabilityAdjustmentsValidatorFactory,
        auditService = mockAuditService,
        cc = cc,
        idGenerator = mockIdGenerator
      )

    MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns true

    protected def callController(): Future[Result] =
      controller.createAmend(validNino, taxYear)(fakeRequest.withBody(Json.toJson(requestBodyModel)).withMethod(PUT))

    protected val requestData: CreateAmendTaxLiabilityAdjustmentsRequestData =
      Def1_CreateAmendTaxLiabilityAdjustmentsRequestData(Nino(validNino), TaxYear.fromMtd(taxYear), requestBodyModel)

  }

}
