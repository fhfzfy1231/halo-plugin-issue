package team.foxbridge.issue.service;

import team.foxbridge.issue.entity.IssueTemplateOptions;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.query.IssueQuery;
import team.foxbridge.issue.entity.ListedIssue;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * issue 相关的接口
 * @author: Akagi_Zen
 * @date: 2025年03月06日 14:54
 */
public interface IssueService {

    Mono<ListResult<ListedIssue>> listIssue(IssueQuery query);

    Mono<Issue> create(Issue issue);

    Flux<String> listAllLabels(IssueQuery query);

    Mono<ListedIssue> findIssueByName(String name);

    Mono<Issue> getByUsername(String issueName, String username);

    Mono<Issue> updateBy(Issue issue);

    Mono<Issue> deleteBy(Issue issue);

    Mono<Issue> closeIssue(Issue issue,  String closedComment, String closedOwner);

    Mono<Issue.IssueContent> getIssueContent(String issueName);

    Mono<Issue> reopenIssue(Issue issue, String reopenOwner);

    Mono<Issue> setAwaitIssue(Issue issue, String setAwaitOwner);

    Mono<Issue> consoleUpdateIssue(Issue issue);

    Mono<Issue> updateAssignees(String issueName, java.util.Set<String> assignees, String operator);

    Mono<IssueTemplateOptions> listIssueSelectTemplateOptions(String subjectName);
}
