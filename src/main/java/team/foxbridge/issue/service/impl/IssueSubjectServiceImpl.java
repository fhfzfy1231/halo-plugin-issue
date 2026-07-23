package team.foxbridge.issue.service.impl;

import team.foxbridge.issue.entity.IssueSubjectStats;
import team.foxbridge.issue.entity.ListedIssueSubject;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.extension.IssueSubject;
import team.foxbridge.issue.notify.NotificationSubscriptionHelper;
import team.foxbridge.issue.query.IssueSubjectQuery;
import team.foxbridge.issue.service.IssueSubjectService;
import team.foxbridge.issue.service.RoleService;
import team.foxbridge.issue.vo.ContributorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.notification.UserIdentity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static run.halo.app.extension.index.query.Queries.equal;

/**
 * 功能描述
 *
 * @author: Akagi_Zen
 * @date: 2025年05月03日 18:23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueSubjectServiceImpl implements IssueSubjectService {

    private final ReactiveExtensionClient client;

    private final RoleService roleService;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    @Override
    public Mono<IssueSubject> create(IssueSubject issueSubject) {
        return roleService.getContextUser()
            .flatMap(user -> {
                issueSubject.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issueSubject);
            });
    }

    @Override
    public Mono<ListResult<ListedIssueSubject>> listIssueSubject(IssueSubjectQuery query) {
        return client.listBy(IssueSubject.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueSubject)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    private Mono<ListedIssueSubject> toListedIssueSubject(IssueSubject issueSubject) {
        ListedIssueSubject.ListedIssueSubjectBuilder issueSubjectBuilder = ListedIssueSubject.builder()
            .issueSubject(issueSubject);
        return Mono.just(issueSubjectBuilder)
            .map(ListedIssueSubject.ListedIssueSubjectBuilder::build)
            .flatMap(li -> fetchIssueSubjectStats(issueSubject.getMetadata().getName())
                .doOnNext(li::setIssueSubjectStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issueSubject.getSpec().getOwner(), li))
            .flatMap(li -> setParticipateUsers(issueSubject.getSpec().getParticipateUsers(), li));
    }

    /**
     * 设置归属者
     * @param owner
     * @param issueSubject
     * @return
     */
    private Mono<ListedIssueSubject> setOwner(String owner, ListedIssueSubject issueSubject) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issueSubject::setCreateOwner)
            .thenReturn(issueSubject);
    }

    /**
     * 查询参与用户信息
     * @param participateUsers
     * @param issueSubject
     * @return
     */
    private Mono<ListedIssueSubject> setParticipateUsers(List<String> participateUsers, ListedIssueSubject issueSubject){
        return Flux.fromIterable(participateUsers)
            .flatMap(participateUser -> client.fetch(User.class, participateUser)
                .map(user -> ContributorVO.from(user))
            )
            .collectList()
            .doOnNext(issueSubject::setParticipateUsers)
            .thenReturn(issueSubject);
    }

    /**
     * issue主体数据统计
     * @param issueSubjectName
     * @return
     */
    @Override
    public Mono<IssueSubjectStats> fetchIssueSubjectStats(String issueSubjectName) {
        Assert.notNull(issueSubjectName, "The issueSubject must not be null.");

        return client.listAll(Issue.class, ListOptions.builder().fieldQuery(equal("spec.subjectName", issueSubjectName))
                    .build(), Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .filter(issue -> issue.getSpec().getApproved())
            .collectList()
            .map(issues -> {
                int progress = (int) issues.stream()
                    .filter(i -> Issue.IssueState.PROGRESS == i.getStatus().getState() && i.getSpec()
                        .getApproved())
                    .count();
                int await = (int) issues.stream()
                    .filter(i -> Issue.IssueState.AWAIT == i.getStatus().getState() && i.getSpec().getApproved())
                    .count();
                int closed = (int) issues.stream()
                    .filter(i -> Issue.IssueState.CLOSED == i.getStatus().getState() && i.getSpec().getApproved())
                    .count();
                int awaitApproved = (int) issues.stream()
                    .filter(i -> !i.getSpec().getApproved())
                    .count();

                // 新增标签统计逻辑
                Set<String> uniqueLabels = issues.stream()
                    .filter(issue -> issue.getSpec().getLabels() != null)
                    .flatMap(issue -> issue.getSpec().getLabels().stream())
                    .collect(Collectors.toSet());
                return IssueSubjectStats.builder()
                    .totalIssue(issues.size())
                    .progressIssue(progress)
                    .awaitIssue(await)
                    .closedIssue(closed)
                    .awaitApproved(awaitApproved)
                    .labels(uniqueLabels.size())
                    .build();
            })
            .defaultIfEmpty(IssueSubjectStats.empty());
    }

    @Override
    public Mono<IssueSubject> updateIssueSubject(IssueSubject issueSubject) {
        return client.fetch(IssueSubject.class, issueSubject.getMetadata().getName())
            .doOnNext(oldIssueSubject -> {
                // 比对下Issue主体的的参与者
                Set<String> oldParticipateUsers = new HashSet<>(oldIssueSubject.getSpec().getParticipateUsers());
                Set<String> newParticipateUsers = new HashSet<>(issueSubject.getSpec().getParticipateUsers());
                // 找出新增的参与人
                if (newParticipateUsers != null && newParticipateUsers.size() > 0) {
                    Set<String> addedParticipateUsers = new HashSet<>(newParticipateUsers);
                    if (oldParticipateUsers != null && oldParticipateUsers.size() > 0) {
                        addedParticipateUsers.removeAll(oldParticipateUsers);
                    }
                    for (String addedAssignee : addedParticipateUsers) {
                        // 在这里处理新增的assignee
                        notificationSubscriptionHelper.reactiveSubscribeNewIssue(UserIdentity.of(addedAssignee));
                    }
                }
            })
            .flatMap(oldIssue -> client.update(issueSubject));
    }


}
