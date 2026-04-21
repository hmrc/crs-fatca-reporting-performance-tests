/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.perftests.tests

import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.tests.Requests._

class Simulation extends PerformanceTestRunner {

  setup("AuthLogin", "Logging in via Auth").withActions(
    getAuthLoginPage,
    postAuthLoginDetails
  )

  setup("ValidCRSFileUpload", "Uploading a valid CRS file").withActions(
    getUploadPage,
    postCRSValidFileUpload,
    getUploadIdStatus,
    getValidation,
    getReportElectionsPage,
    postReportElectionsYesPage,
    getElectionsCrsContractsPage,
    postElectionsCrsContractsPage,
    getElectionsCrsDormantAccountsPage,
    postElectionsCrsDormantAccountsPage,
    getElectionsCrsThresholdsPage,
    postElectionsCrsThresholdsPage,
    getCheckYourFileDetailsPage,
    getSendYourFilePage,
    postSendYourFilePage
  )

  setup("SendFile", "Sending File")
    .withActions(
      getStillCheckingYourFilePage
    )
    .withActions(
      getSecondStatus: _*
    )

  setup("RefreshStillCheckingYourFilePage", "Refresh Still Checking Your File Page - Success")
    .withActions(
      refreshOnStillCheckingYourFilePage: _*
    )
    .withActions(
      getFilePassedChecksPage
    )

  setup("ConfirmationPage", "Get File Confirmation Page").withActions(
    getFilePassedChecksPage,
    getFileConfirmationPage
  )

  setup("InvalidCrsBusinessRulesFileUpload", "Uploading an invalid CRS file for Business rules").withActions(
    getUploadPage,
    postCRSInvalidBRFileUpload,
    getUploadIdStatus,
    getValidation,
    getReportElectionsPage,
    postReportElectionsNoPage,
    getCheckYourFileDetailsPage,
    getSendYourFilePage,
    postSendYourFilePage
  )

  setup("RefreshStillCheckingYourFilePageForErrors", "Refresh Still Checking Your File Page - Failed")
    .withActions(
      refreshOnStillCheckingYourFilePage: _*
    )
    .withActions(
      getFileFailedChecksPage
    )

  setup("RuleErrorsPage", "get rules-errors page").withActions(
    getFileFailedChecksPage,
    getBusinessRulesErrorsPage
  )

  setup("InvalidCrsFileUpload", "Uploading an invalid CRS file for Schema errors").withActions(
    getUploadPage,
    postCRSInvalidSchemaFileUpload,
    getUploadIdStatus,
    getValidationRedirect
  )

  setup("SchemaErrorsPage", "Get schema errors page").withActions(
    getSchemaErrorPage
  )

  runSimulation()
}
