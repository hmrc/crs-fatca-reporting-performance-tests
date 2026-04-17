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

  setup("AuthLogin", "Logging in via Auth") withRequests (
    getAuthLoginPage,
    postAuthLoginDetails,

  )

  setup("ValidCRSFileUpload", "Uploading a valid CRS file") withRequests(
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
    postSendYourFilePage,
  )

  setup("InvalidCrsFileUpload", "Uploading an invalid CRS file for Schema errors") withRequests(
    getUploadPage,
    postCRSInvalidSchemaFileUpload,
    getUploadIdStatus,
  )

  setup("InvalidCrsBusinessRulesFileUpload", "Uploading an invalid CRS file for Business rules") withRequests(
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

  setup("SchemaErrorsPage", "Get schema errors page") withRequests getSchemaErrorPage

  setup("SendFile", "Sending File")
    .withRequests(getStillCheckingYourFilePage) .withActions(getSecondStatus: _*)

  setup("RefreshStillCheckingYourFilePage", "Refresh Still Checking Your File Page - Success")
    .withActions(refreshOnStillCheckingYourFilePage: _*)withRequests
    getFilePassedChecksPage

  setup("RefreshStillCheckingYourFilePageForErrors", "Refresh Still Checking Your File Page - Failed")
    .withActions(refreshOnStillCheckingYourFilePage: _*)withRequests(getFileFailedChecksPage)

  setup("RuleErrorsPage", "get rules-errors page")withRequests(getFileFailedChecksPage, getBusinessRulesErrorsPage)

  setup("ConfirmationPage", "Get File Confirmation Page") withRequests(getFilePassedChecksPage,getFileConfirmationPage)
  runSimulation()
}
