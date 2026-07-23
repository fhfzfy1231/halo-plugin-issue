package team.foxbridge.issue.service;

import team.foxbridge.issue.entity.IssueLabelOptions;
import team.foxbridge.issue.entity.ListedIssueLabel;
import team.foxbridge.issue.extension.IssueLabel;
import team.foxbridge.issue.query.IssueLabelQuery;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * @author: Akagi_Zen
 * @date: 2025年06月26日 09:53
 * @description:
 */
public interface IssueLabelService {

    Mono<ListResult<ListedIssueLabel>> listIssueLabels(IssueLabelQuery query);

    Mono<IssueLabel> create(IssueLabel issueLabel);

    Mono<IssueLabelOptions> listSubjectIssueLabels(String subjectName, String keyword);
}
