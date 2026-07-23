package team.foxbridge.issue.service;

import team.foxbridge.issue.entity.IssueSubjectStats;
import team.foxbridge.issue.entity.ListedIssueSubject;
import team.foxbridge.issue.extension.IssueSubject;
import team.foxbridge.issue.query.IssueSubjectQuery;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * issue 主体相关接口
 * @author: Akagi_Zen
 * @date: 2025年05月03日 18:18
 */
public interface IssueSubjectService {

    Mono<IssueSubject> create(IssueSubject issueSubject);

    Mono<ListResult<ListedIssueSubject>> listIssueSubject(IssueSubjectQuery query);

    Mono<IssueSubjectStats> fetchIssueSubjectStats(String issueSubjectName);

    Mono<IssueSubject> updateIssueSubject(IssueSubject issueSubject);
}
