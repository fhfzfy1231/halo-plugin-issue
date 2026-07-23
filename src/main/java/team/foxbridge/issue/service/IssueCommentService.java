package team.foxbridge.issue.service;

import team.foxbridge.issue.entity.ListedIssueComment;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.extension.IssueComment;
import team.foxbridge.issue.query.IssueCommentQuery;
import team.foxbridge.issue.vo.IssueCommentVO;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * issue 评论的相关业务接口
 * @author: Akagi_Zen
 * @date: 2025年05月26日 09:25
 * @description:
 */
public interface IssueCommentService {

    Mono<IssueComment> create(IssueComment issueComment);

    Mono<ListResult<ListedIssueComment>> listIssueComment(IssueCommentQuery query);

    Mono<IssueComment> updateBy(IssueComment issueComment);

    Mono<IssueComment> deleteBy(IssueComment issueComment);

    Mono<IssueComment> getByUsername(String issueCommentName, String name);

    Mono<IssueComment.IssueCommentContent> getIssueCommentContent(String issueCommentName);
}
