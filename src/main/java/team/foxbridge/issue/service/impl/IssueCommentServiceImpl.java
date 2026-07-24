package team.foxbridge.issue.service.impl;

import team.foxbridge.issue.entity.ListedIssueComment;
import team.foxbridge.issue.entity.Stats;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.extension.IssueComment;
import team.foxbridge.issue.query.IssueCommentQuery;
import team.foxbridge.issue.service.IssueCommentService;
import team.foxbridge.issue.service.RoleService;
import team.foxbridge.issue.util.MeterUtils;
import team.foxbridge.issue.vo.ContributorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import java.util.Objects;

/**
 * @description: issue 评论的相关接口实现类
 * @className: IssueCommentServiceImpl
 * @author: Akagi_Zen
 * @date: 2025年05月26日 09:28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueCommentServiceImpl implements IssueCommentService {

    private final ReactiveExtensionClient client;

    private final RoleService roleService;

    @Override
    public Mono<IssueComment> create(IssueComment issueComment) {
        return roleService.getContextUser()
            .flatMap(user -> {
                issueComment.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issueComment);
            });
    }

    @Override
    public Mono<ListResult<ListedIssueComment>> listIssueComment(IssueCommentQuery query) {
        return client.listBy(IssueComment.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueComment)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<IssueComment> updateBy(IssueComment issue) {
        return client.update(issue);
    }

    @Override
    public Mono<IssueComment> deleteBy(IssueComment issue) {
        return client.delete(issue);
    }

    @Override
    public Mono<IssueComment> getByUsername(String issueCommentName, String name) {
        return client.get(IssueComment.class, issueCommentName)
            .filter(post -> post.getSpec() != null)
            .filter(post -> !Boolean.TRUE.equals(post.getSpec().getSystemEvent()))
            .filter(post -> Objects.equals(name, post.getSpec().getOwner()));
    }

    @Override
    public Mono<IssueComment.IssueCommentContent> getIssueCommentContent(String issueCommentName) {
        return client.fetch(IssueComment.class, issueCommentName).map(issueComment -> issueComment.getSpec().getContent());
    }

    private Mono<ListedIssueComment> toListedIssueComment(IssueComment issueComment) {
        ListedIssueComment.ListedIssueCommentBuilder issueBuilder = ListedIssueComment.builder()
            .issueComment(issueComment);
        return Mono.just(issueBuilder)
            .map(ListedIssueComment.ListedIssueCommentBuilder::build)
            .flatMap(li -> fetchIssueCommentStats(issueComment)
                .doOnNext(li::setStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issueComment.getSpec().getOwner(), li));
    }

    private Mono<ListedIssueComment> setOwner(String owner, ListedIssueComment issue) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issue::setContributorVo)
            .thenReturn(issue);
    }

    private Mono<Stats> fetchIssueCommentStats(IssueComment issueComment) {
        Assert.notNull(issueComment, "The issue must not be null.");
        String name = issueComment.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(IssueComment.class, name))
            .map(counter -> Stats.builder()
                .upvote(counter.getUpvote())
                .downvote(counter.getDownvote())
                .build())
            .defaultIfEmpty(Stats.empty());
    }

}
