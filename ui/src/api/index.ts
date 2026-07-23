import { axiosInstance } from "@halo-dev/api-client";
import {
  IssueSubjectV1alpha1Api,
  ConsoleApiIssueSubjectFoxbridgeTeamV1alpha1IssueSubjectApi,
  IssueV1alpha1Api,
  ConsoleApiIssueFoxbridgeTeamV1alpha1IssueApi,
  UcApiIssueFoxbridgeTeamV1alpha1IssueApi,
  IssueCommentV1alpha1Api,
  ConsoleApiIssueCommentFoxbridgeTeamV1alpha1IssueApi,
  UcApiIssueCommentFoxbridgeTeamV1alpha1IssueCommentApi,
  IssueTemplateV1alpha1Api,
  ConsoleApiIssueTemplateFoxbridgeTeamV1alpha1IssueTemplateApi,
  IssueLabelV1alpha1Api,
  ConsoleApiIssueLabelFoxbridgeTeamV1alpha1IssueLabelApi
} from "./generated";

const issueSubjectApiClient = {
  issueSubject: new IssueSubjectV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueSubjectApiClient = {
  issueSubject: new ConsoleApiIssueSubjectFoxbridgeTeamV1alpha1IssueSubjectApi(undefined, "", axiosInstance),
};

const issueApiClient = {
  issue: new IssueV1alpha1Api(undefined, "", axiosInstance),
};


const issueCommentApiClient = {
  issueComment: new IssueCommentV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueCommentApiClient = {
  issueComment: new ConsoleApiIssueCommentFoxbridgeTeamV1alpha1IssueApi(undefined, "", axiosInstance),
};

const ucIssueCommentApiClient = {
  issueComment: new UcApiIssueCommentFoxbridgeTeamV1alpha1IssueCommentApi(undefined, "", axiosInstance),
};

const issueTemplateApiClient = {
  issueTemplate: new IssueTemplateV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueTemplateApiClient = {
  issueTemplate: new ConsoleApiIssueTemplateFoxbridgeTeamV1alpha1IssueTemplateApi(undefined, "", axiosInstance),
};

const consoleIssueApiClient = {
  issue: new ConsoleApiIssueFoxbridgeTeamV1alpha1IssueApi(undefined, "", axiosInstance),
};

const ucIssueApiClient = {
  issue: new UcApiIssueFoxbridgeTeamV1alpha1IssueApi(undefined, "", axiosInstance),
};

const issueLabelApiClient = {
  issueLabel: new IssueLabelV1alpha1Api(undefined, "", axiosInstance),
};

const consoleIssueLabelApiClient = {
  issueLabel: new ConsoleApiIssueLabelFoxbridgeTeamV1alpha1IssueLabelApi(undefined, "", axiosInstance),
};

export {
  issueSubjectApiClient,
  consoleIssueSubjectApiClient,
  issueApiClient,
  consoleIssueApiClient,
  ucIssueApiClient,
  issueCommentApiClient,
  consoleIssueCommentApiClient,
  ucIssueCommentApiClient,
  issueTemplateApiClient,
  consoleIssueTemplateApiClient,
  issueLabelApiClient,
  consoleIssueLabelApiClient
};
