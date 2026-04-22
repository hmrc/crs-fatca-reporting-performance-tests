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

import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.core.session.Expression
import uk.gov.hmrc.performance.conf.ServicesConfiguration

import scala.concurrent.duration.DurationInt

object Requests extends ServicesConfiguration {

  val upscanTimer: Int  = 25
  val statusChecks: Int = 12
  val refreshes: Int    = 5

  val baseUrl: String     = baseUrlFor("crs-fatca-reporting-frontend")
  val baseUrlAuth: String = baseUrlFor("auth-frontend")
  val route: String       = "/report-for-crs-and-fatca/report"
  val authRoute: String   = "/auth-login-stub/gg-sign-in"
  val amazonUrlPattern    = """action="(.*?)""""

  def inputSelectionByName(name: String): Expression[String] = s"input[name='$name']"

  val getAuthLoginPage: HttpRequestBuilder =
    http("Get Auth login page")
      .get(baseUrlAuth + authRoute)
      .check(status.is(200))

  val postAuthLoginDetails: HttpRequestBuilder =
    http("Enter Auth login details")
      .post(baseUrlAuth + authRoute)
      .formParam("authorityId", "")
      .formParam("redirectionUrl", baseUrl + route)
      .formParam("credentialStrength", "strong")
      .formParam("confidenceLevel", "50")
      .formParam("affinityGroup", "Organisation")
      .formParam("enrolment[0].name", "HMRC-FATCA-ORG")
      .formParam("enrolment[0].taxIdentifier[0].name", "FATCAID")
      .formParam("enrolment[0].taxIdentifier[0].value", "XE2ATCA0009234567")
      .formParam("enrolment[0].state", "Activated")
      .check(status.is(303))
      .check(header("Location").is(baseUrl + route).saveAs("LandingPage"))

  val getUploadPage: HttpRequestBuilder =
    http("Get Upload an XML file")
      .get(baseUrl + route + "/upload-file")
      .check(form("#uploadForm").saveAs("CRSFATCAUploadForm"))
      .check(saveFileUploadUrl)
      .check(status.is(200))

  val postCRSValidFileUpload: HttpRequestBuilder =
    http("Post a valid file Upload")
      .post("#{fileUploadAmazonUrl}")
      .asMultipartForm
      .form("#{CRSFATCAUploadForm}")
      .bodyPart(RawFileBodyPart("file", "data/valid-crs-slowresponseaccepted-xml.xml"))
      .check(status.is(303))
      .check(header("location").saveAs("Status"))

  val postFATCAValidFileUpload: HttpRequestBuilder =
    http("Post a valid file Upload")
      .post("#{fileUploadAmazonUrl}")
      .asMultipartForm
      .form("#{CRSFATCAUploadForm}")
      .bodyPart(RawFileBodyPart("file", "data/valid-fatca-fastresponseaccepted-xml.xml"))
      .check(status.is(303))
      .check(header("location").saveAs("Status"))

  val postCRSInvalidSchemaFileUpload: HttpRequestBuilder =
    http("Post an Invalid Crs Schema error file upload")
      .post("#{fileUploadAmazonUrl}")
      .asMultipartForm
      .form("#{CRSFATCAUploadForm}")
      .bodyPart(RawFileBodyPart("file", "data/crs-schemaerrors.xml"))
      .check(status.is(303))
      .check(header("location").saveAs("Status"))

  val postCRSInvalidBRFileUpload: HttpRequestBuilder =
    http("Post an Invalid Crs business rules file upload")
      .post("#{fileUploadAmazonUrl}")
      .asMultipartForm
      .form("#{CRSFATCAUploadForm}")
      .bodyPart(RawFileBodyPart("file", "data/valid-crs-rules-errors-slowresponserejected-xml.xml"))
      .check(status.is(303))
      .check(header("location").saveAs("Status"))

  val getUploadIdStatus: HttpRequestBuilder =
    http("Get Status Page")
      .get("#{Status}")
      .check(status.is(303))
      .check(header("Location").saveAs("validationRedirect"))

  val pollUntilValidated: List[ActionBuilder] =
    asLongAsDuring(
      session => session("validationRedirect").as[String].contains("status"),
      upscanTimer.seconds
    ) {
      exec(
        http("Poll Status")
          .get("#{Status}")
          .check(status.is(303))
          .check(header("Location").saveAs("validationRedirect"))
          .silent
      ).pause(3.seconds)
    }.actionBuilders

  val getValidationRedirect: HttpRequestBuilder =
    http("Get Validation Redirect")
      .get(baseUrl + "#{validationRedirect}")
      .check(status.in(200, 303))
      .check(header("Location").optional.saveAs("finalRedirect"))

  val getValidation: HttpRequestBuilder =
    http("Get Validation - Valid")
      .get(baseUrl + "/report-for-crs-and-fatca/file-validation")
      .check(status.is(303))
      .check(header("Location").is(route + "/elections/report-elections").saveAs("ReportElections"))

  val getValidationSchemaErrors: HttpRequestBuilder =
    http("Get Validation - Invalid - Schema Errors")
      .get(baseUrl + "/report-for-crs-and-fatca/file-validation")
      .check(status.is(303))
      .check(header("Location").is(route + "/problem/data-errors").saveAs("SchemaErrorsPage"))

  val getSchemaErrorPage: HttpRequestBuilder =
    http("get schema error page")
      .get(s"$baseUrl$route/problem/data-errors")
      .check(status.in(200) )
      .check(bodyString.saveAs("errorPageBody"))

  val getReportElectionsPage: HttpRequestBuilder =
    http("Get Report Elections")
      .get(baseUrl + "#{ReportElections}")
      .check(status.is(200))
      .check(css(inputSelectionByName("csrfToken"), "value").saveAs("csrfToken"))

  val postReportElectionsYesPage: HttpRequestBuilder =
    http("post report elections-yes")
      .post(baseUrl + "#{ReportElections}")
      .formParam("csrfToken", "#{csrfToken}")
      .formParam("value", "true")
      .check(status.is(303))
      .check(header("Location").is(route + "/elections/crs/contracts").saveAs("crsContracts"))

  val postReportElectionsNoPage: HttpRequestBuilder =
    http("post report elections-yes")
      .post(baseUrl + "#{ReportElections}")
      .formParam("csrfToken", "#{csrfToken}")
      .formParam("value", "false")
      .check(status.is(303))
      .check(header("Location").is(route + "/check-your-file-details").saveAs("checkYourFileDetails"))

  val getElectionsCrsContractsPage: HttpRequestBuilder =
    http("Get CRS Contracts")
      .get(baseUrl + "#{crsContracts}")
      .check(status.is(200))
      .check(css(inputSelectionByName("csrfToken"), "value").saveAs("csrfToken"))

  val postElectionsCrsContractsPage: HttpRequestBuilder =
    http("post crs contracts-yes")
      .post(baseUrl + "#{crsContracts}")
      .formParam("csrfToken", "#{csrfToken}")
      .formParam("value", "true")
      .check(status.is(303))
      .check(header("Location").is(route + "/elections/crs/dormant-accounts").saveAs("crsDormantAccounts"))

  val getElectionsCrsDormantAccountsPage: HttpRequestBuilder =
    http("Get CRS Dormant Accounts")
      .get(baseUrl + "#{crsDormantAccounts}")
      .check(status.is(200))
      .check(css(inputSelectionByName("csrfToken"), "value").saveAs("csrfToken"))

  val postElectionsCrsDormantAccountsPage: HttpRequestBuilder =
    http("post crs dormant accounts-yes")
      .post(baseUrl + "#{crsDormantAccounts}")
      .formParam("csrfToken", "#{csrfToken}")
      .formParam("value", "true")
      .check(status.is(303))
      .check(header("Location").is(route + "/elections/crs/thresholds").saveAs("crsThresholds"))

  val getElectionsCrsThresholdsPage: HttpRequestBuilder =
    http("Get CRS Thresholds")
      .get(baseUrl + "#{crsThresholds}")
      .check(status.is(200))
      .check(css(inputSelectionByName("csrfToken"), "value").saveAs("csrfToken"))

  val postElectionsCrsThresholdsPage: HttpRequestBuilder =
    http("post crs thresholds-yes")
      .post(baseUrl + "#{crsThresholds}")
      .formParam("csrfToken", "#{csrfToken}")
      .formParam("value", "true")
      .check(status.is(303))
      .check(header("Location").is(route + "/check-your-file-details").saveAs("checkYourFileDetails"))

  val getCheckYourFileDetailsPage: HttpRequestBuilder =
    http("Get Check Your File Details Page")
      .get(baseUrl + "#{checkYourFileDetails}")
      .check(status.is(200))

  val getSendYourFilePage: HttpRequestBuilder =
    http("Get Send Your File Page")
      .get(baseUrl + route + "/send-your-file")
      .check(css(inputSelectionByName("csrfToken"), "value").saveAs("csrfToken"))
      .check(status.is(200))

  val postSendYourFilePage: HttpRequestBuilder =
    http("Post send your file page")
      .post(baseUrl + route + "/send-your-file")
      .formParam("csrfToken", "#{csrfToken}")
      .check(status.is(303))
      .check(header("Location").is(route + "/still-checking-your-file").saveAs("stillCheckingYourFile"))

  val getSecondStatus: List[ActionBuilder] =
    repeat(statusChecks) {
      exec(
        http("check the status")
          .get(baseUrl + "/report-for-crs-and-fatca/check-status")
          .check(status.in(200, 204))
      )
        .pause(3)
    }.actionBuilders

  val getStillCheckingYourFilePage: HttpRequestBuilder =
    http("Get still checking your file page")
      .get(baseUrl + route + "/still-checking-your-file")
      .check(status.is(200))

  val refreshOnStillCheckingYourFilePage: List[ActionBuilder] =
    repeat(refreshes) {
      exec(
        http("get still checking your file page")
          .get(baseUrl + route + "/still-checking-your-file")
          .check(status.is(303))
      )
        .pause(1)
    }.actionBuilders

  val getFilePassedChecksPage: HttpRequestBuilder =
    http("Get file passed checks page")
      .get(baseUrl + route + "/file-passed-checks")
      .check(
        css("a#submit", "href")
          .transform(url => url.replaceAll("/report-for-crs-and-fatca/report/file-confirmation/", ""))
          .saveAs("uuid")
      )
      .check(status.is(200))

  val getFileFailedChecksPage: HttpRequestBuilder =
    http("Get file failed checks page")
      .get(baseUrl + route + "/file-failed-checks")
      .check(
        css("a#submit", "href")
          .transform(url => url.replaceAll("/report-for-crs-and-fatca/report/problem/rules-errors/", ""))
          .saveAs("uuid")
      )
      .check(status.is(200))

  val getBusinessRulesErrorsPage: HttpRequestBuilder =
    http("Get business rules errors page")
      .get(baseUrl + route + "/problem/rules-errors/" + "#{uuid}")
      .check(status.is(200))

  val getFileConfirmationPage: HttpRequestBuilder =
    http("Get file confirmation page")
      .get(baseUrl + route + "/file-confirmation/" + "#{uuid}")
      .check(status.is(200))

  def saveFileUploadUrl: CheckBuilder[RegexCheckType, String] =
    regex(_ => amazonUrlPattern).saveAs("fileUploadAmazonUrl")

  val pollingStatus: List[ActionBuilder] = {
    val pollRequest: ChainBuilder =
      asLongAsDuring(
        session => !session("status").asOption[Int].contains(200),
        upscanTimer.seconds
      ) {
        exec(
          http("poll status")
            .get(baseUrl + route + "/report/status")
            .check(status.is(303))
            .check(header("status").saveAs("status"))
            .silent
        )
          .pause(5)

      }
    pollRequest.actionBuilders
  }
}
