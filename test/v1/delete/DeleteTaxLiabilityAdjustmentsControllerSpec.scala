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

package v1.delete

import api.controllers.ControllerTestRunner
import api.models.audit.*
import api.models.domain.TaxYear
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v1.delete.def1.model.request.Def1_DeleteTaxLiabilityAdjustmentsRequestData
import v1.delete.model.request.DeleteTaxLiabilityAdjustmentsRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteTaxLiabilityAdjustmentsControllerSpec
    extends ControllerTestRunner
    with MockDeleteTaxLiabilityAdjustmentsService
    with MockDeleteTaxLiabilityAdjustmentsValidatorFactory {

  private val taxYear = "2026-27"

  protected val requestData: DeleteTaxLiabilityAdjustmentsRequestData =
    Def1_DeleteTaxLiabilityAdjustmentsRequestData(parsedNino, TaxYear.fromMtd(taxYear))

  "delete" should {
    "return 204 NO_CONTENT" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteTaxLiabilityAdjustmentsService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeExpectedResponseBody = None)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))
        runErrorTestWithAudit(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteTaxLiabilityAdjustmentsService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleOutsideAmendmentWindowError, None))))

        runErrorTestWithAudit(RuleOutsideAmendmentWindowError)
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: DeleteTaxLiabilityAdjustmentsController = new DeleteTaxLiabilityAdjustmentsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockDeleteTaxLiabilityAdjustmentsService,
      validatorFactory = mockDeleteTaxLiabilityAdjustmentsValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.delete(validNino, taxYear)(fakeRequest)

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteTaxLiabilityAdjustments",
        transactionName = "delete-tax-liability-adjustments",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = apiVersion.name,
          params = Map("nino" -> validNino, "taxYear" -> taxYear),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns true

  }

}
