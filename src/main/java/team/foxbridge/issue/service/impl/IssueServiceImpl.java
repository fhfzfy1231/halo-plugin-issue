package team.foxbridge.issue.service.impl;

import team.foxbridge.issue.Constant;
import team.foxbridge.issue.entity.IssueStats;
import team.foxbridge.issue.entity.IssueTemplateOptions;
import team.foxbridge.issue.extension.IssueComment;
import team.foxbridge.issue.extension.IssueSubject;
import team.foxbridge.issue.extension.IssueTemplate;
import team.foxbridge.issue.finder.IssueSubjectFinder;
import team.foxbridge.issue.notify.NotificationSubscriptionHelper;
import team.foxbridge.issue.service.RoleService;
import team.foxbridge.issue.util.MeterUtils;
import team.foxbridge.issue.exception.NotFoundException;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.query.IssueQuery;
import team.foxbridge.issue.service.IssueService;
import team.foxbridge.issue.util.ReasonDataConverterUtils;
import team.foxbridge.issue.vo.ContributorVO;
import team.foxbridge.issue.entity.ListedIssue;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static run.halo.app.extension.MetadataUtil.nullSafeAnnotations;
import static run.halo.app.extension.index.query.Queries.equal;

/**
 * Issue extensions for apis implemention
 * @author: Akagi_Zen
 * @date: 2025年03月06日 14:54
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final ReactiveExtensionClient client;

    private final RoleService roleService;

    private final NotificationReasonEmitter notificationReasonEmitter;

    private final ExternalLinkProcessor externalLinkProcessor;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    @Override
    public Mono<ListResult<ListedIssue>> listIssue(IssueQuery query) {
        return client.listBy(Issue.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssue)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<Issue> create(Issue issue) {
        if (Objects.isNull(issue.getSpec().getReleaseTime())) {
            issue.getSpec().setReleaseTime(Instant.now());
        }

        return roleService.getContextUser()
            .flatMap(user -> {
                issue.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issue);
            });
    }

    @Override
    public Flux<String> listAllLabels(IssueQuery query) {
        return client.listAll(Issue.class, query.toListOptions(),
                Sort.by("metadata.name").descending())
            .flatMapIterable(issue -> {
                var labels = issue.getSpec().getLabels();
                return Objects.requireNonNullElseGet(labels, List::of);
            })
            .distinct();
    }

    @Override
    public Mono<ListedIssue> findIssueByName(String name) {
        return client.fetch(Issue.class, name)
            .switchIfEmpty(Mono.error(new NotFoundException("Issue not found.")))
            .flatMap(this::toListedIssue);
    }

    @Override
    public Mono<Issue> getByUsername(String issueName, String username) {
        return client.get(Issue.class, issueName)
            .filter(post -> post.getSpec() != null)
            .filter(post -> Objects.equals(username, post.getSpec().getOwner()));
    }

    @Override
    public Mono<Issue> updateBy(Issue issue) {
        return client.update(issue);
    }

    @Override
    public Mono<Issue> deleteBy(Issue issue) {
        return client.listAll(IssueComment.class, ListOptions.builder()
                    .fieldQuery(equal("spec.issueName", issue.getMetadata().getName())).build(),
                Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
            .flatMapMany(Flux::fromIterable)
            .flatMap(comment -> client.delete(comment))
            .then(client.delete(issue));
    }

    private Mono<ListedIssue> toListedIssue(Issue issue) {
        ListedIssue.ListedIssueBuilder issueBuilder = ListedIssue.builder()
            .issue(issue);
        return Mono.just(issueBuilder)
            .map(ListedIssue.ListedIssueBuilder::build)
            .flatMap(li -> fetchIssueStats(issue)
                .doOnNext(li::setIssueStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issue.getSpec().getOwner(), li));
    }

    private Mono<ListedIssue> setOwner(String owner, ListedIssue issue) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issue::setContributorVo)
            .thenReturn(issue);
    }

    private Mono<IssueStats> fetchIssueStats(Issue issue) {
        Assert.notNull(issue, "The issue must not be null.");
        String issueName = issue.getMetadata().getName();

        // 保留原有 Counter 查询，用于 upvote 和 downvote
        Mono<IssueStats> counterStatsMono = client.fetch(Counter.class, MeterUtils.nameOf(Issue.class, issueName))
            .map(counter -> IssueStats.builder()
                .upvote(counter.getUpvote())
                .downvote(counter.getDownvote())
                .build())
            .defaultIfEmpty(IssueStats.builder().upvote(0).downvote(0).build());

        // 新增 IssueComment 查询，用于统计评论
        Mono<IssueStats> commentStatsMono = client.listAll(IssueComment.class, ListOptions.builder()
                    .fieldQuery(equal("spec.issueName", issueName)).build(),
                Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
            .map(comments -> {
                var userComments = comments.stream()
                    .filter(comment -> !Boolean.TRUE.equals(comment.getSpec().getSystemEvent()))
                    .toList();
                int totalComment = userComments.size();
                long approvedComment = userComments.stream()
                    .filter(comment -> Boolean.TRUE.equals(comment.getSpec().getApproved()))
                    .count();
                long awaitApprovedComment = userComments.stream()
                    .filter(comment -> Boolean.FALSE.equals(comment.getSpec().getApproved()))
                    .count();
                return IssueStats.builder()
                    .totalIssueComment(totalComment)
                    .approvedIssueComment((int) approvedComment)
                    .awaitApproveIssueComment((int) awaitApprovedComment)
                    .build();
            });

        // 合并两个结果
        return Mono.zip(counterStatsMono, commentStatsMono)
            .map(tuple -> {
                IssueStats counterStats = tuple.getT1();
                IssueStats commentStats = tuple.getT2();
                return IssueStats.builder()
                    .visit(counterStats.getVisit())
                    .upvote(counterStats.getUpvote())
                    .downvote(counterStats.getDownvote())
                    .totalIssueComment(commentStats.getTotalIssueComment())
                    .approvedIssueComment(commentStats.getApprovedIssueComment())
                    .awaitApproveIssueComment(commentStats.getAwaitApproveIssueComment())
                    .build();
            });
    }


    @Override
    public Mono<Issue> closeIssue(Issue issue, String closedComment, String closedOwner) {
        Issue.StateTransition stateTransition = new Issue.StateTransition();
        Issue.IssueState oldState = issue.getStatus().getState();
        stateTransition.setFromState(oldState);
        stateTransition.setToState(Issue.IssueState.CLOSED);
        stateTransition.setTime(Instant.now());
        stateTransition.setOperator(closedOwner);
        stateTransition.setComment(closedComment);
        issue.getStatus().getTransitions().add(stateTransition);
        issue.getStatus().setState(Issue.IssueState.CLOSED);
        issue.getSpec().setClosedAt(Instant.now());

        var issueAnnotations = nullSafeAnnotations(issue);
        var newIssueNotified = issueAnnotations.getOrDefault(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "false");
        Set<String> issueWatchers = new HashSet<>();
        if (issue.getSpec().getAssignees() != null) {
            issueWatchers.addAll(issue.getSpec().getAssignees());
        }
        issueWatchers.add(issue.getSpec().getOwner());

        if (Objects.equals(newIssueNotified, "false")) {
            issueAnnotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "true");
            return client.update(issue)
                .flatMap(updatedIssue -> createSystemEvent(
                        updatedIssue, closedOwner,
                        IssueComment.IssueSystemEventType.STATUS_CLOSED,
                        "关闭了 Issue，原因：" + closedComment)
                    .then(sendClosedIssueNotification(
                        updatedIssue, issueWatchers, "全站 Issue", "Issue",
                        closedComment, closedOwner))
                    .thenReturn(updatedIssue));
        }
        return client.update(issue)
            .flatMap(updatedIssue -> createSystemEvent(
                    updatedIssue, closedOwner,
                    IssueComment.IssueSystemEventType.STATUS_CLOSED,
                    "关闭了 Issue，原因：" + closedComment)
                .thenReturn(updatedIssue));
    }

    @Override
    public Mono<Issue.IssueContent> getIssueContent(String issueName) {
        return client.fetch(Issue.class, issueName).map(issue -> issue.getSpec().getContent());
    }

    @Override
    public Mono<Issue> reopenIssue(Issue issue, String reopenOwner) {
        Issue.StateTransition stateTransition = new Issue.StateTransition();
        Issue.IssueState oldState = issue.getStatus().getState();
        stateTransition.setFromState(oldState);
        stateTransition.setToState(Issue.IssueState.PROGRESS);
        stateTransition.setTime(Instant.now());
        stateTransition.setOperator(reopenOwner);

        boolean openingFromAwait = oldState == Issue.IssueState.AWAIT;
        String transitionComment = openingFromAwait ? "打开Issue" : "重新打开Issue";
        stateTransition.setComment(transitionComment);
        issue.getStatus().getTransitions().add(stateTransition);
        issue.getStatus().setState(Issue.IssueState.PROGRESS);
        issue.getSpec().setClosedAt(null);

        var issueAnnotations = nullSafeAnnotations(issue);
        issueAnnotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "false");

        var eventType = openingFromAwait
            ? IssueComment.IssueSystemEventType.STATUS_OPENED
            : IssueComment.IssueSystemEventType.STATUS_REOPENED;
        String eventText = openingFromAwait
            ? "将 Issue 从等待中切换为处理中"
            : "重新打开了 Issue";

        return client.update(issue)
            .flatMap(updatedIssue -> createSystemEvent(updatedIssue, reopenOwner, eventType, eventText)
                .thenReturn(updatedIssue));
    }

    @Override
    public Mono<Issue> setAwaitIssue(Issue issue, String setAwaitOwner) {
        Issue.StateTransition stateTransition = new Issue.StateTransition();
        Issue.IssueState oldState = issue.getStatus().getState();
        stateTransition.setFromState(oldState);
        stateTransition.setToState(Issue.IssueState.AWAIT);
        stateTransition.setTime(Instant.now());
        stateTransition.setOperator(setAwaitOwner);
        stateTransition.setComment("设置Issue为等待中");
        issue.getStatus().getTransitions().add(stateTransition);
        issue.getStatus().setState(Issue.IssueState.AWAIT);
        if (oldState == Issue.IssueState.CLOSED) {
            var issueAnnotations = nullSafeAnnotations(issue);
            issueAnnotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "false");
            issue.getSpec().setClosedAt(null);
        }
        return client.update(issue)
            .flatMap(updatedIssue -> createSystemEvent(
                    updatedIssue, setAwaitOwner,
                    IssueComment.IssueSystemEventType.STATUS_AWAIT,
                    "将 Issue 设置为等待中")
                .thenReturn(updatedIssue));
    }

    /**
     * console端更新issue。保留旧接口兼容现有调用。
     */
    @Override
    public Mono<Issue> consoleUpdateIssue(Issue issue) {
        return client.fetch(Issue.class, issue.getMetadata().getName())
            .doOnNext(oldIssue -> {
                Set<String> oldAssignees = oldIssue.getSpec().getAssignees();
                Set<String> newAssignees = issue.getSpec().getAssignees();
                if (newAssignees != null && !newAssignees.isEmpty()) {
                    Set<String> addedAssignees = new HashSet<>(newAssignees);
                    if (oldAssignees != null && !oldAssignees.isEmpty()) {
                        addedAssignees.removeAll(oldAssignees);
                    }
                    for (String addedAssignee : addedAssignees) {
                        notificationSubscriptionHelper.reactiveSubscribeComment(UserIdentity.of(addedAssignee));
                    }
                }
            })
            .flatMap(oldIssue -> client.update(issue));
    }

    @Override
    public Mono<Issue> updateAssignees(String issueName, Set<String> assignees, String operator) {
        Set<String> targetAssignees = assignees == null
            ? new LinkedHashSet<>()
            : new LinkedHashSet<>(assignees);

        return client.fetch(Issue.class, issueName)
            .switchIfEmpty(Mono.error(new NotFoundException("Issue not found.")))
            .flatMap(issue -> {
                Set<String> oldAssignees = issue.getSpec().getAssignees() == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(issue.getSpec().getAssignees());

                Set<String> addedAssignees = new LinkedHashSet<>(targetAssignees);
                addedAssignees.removeAll(oldAssignees);

                Set<String> removedAssignees = new LinkedHashSet<>(oldAssignees);
                removedAssignees.removeAll(targetAssignees);

                issue.getSpec().setAssignees(targetAssignees);

                Mono<Void> subscribeAdded = Flux.fromIterable(addedAssignees)
                    .concatMap(assignee -> notificationSubscriptionHelper
                        .reactiveSubscribeComment(UserIdentity.of(assignee)))
                    .then();

                return subscribeAdded
                    .then(client.update(issue))
                    .flatMap(updatedIssue -> {
                        Mono<Void> addedEvents = Flux.fromIterable(addedAssignees)
                            .concatMap(assignee -> createSystemEvent(
                                updatedIssue, operator,
                                IssueComment.IssueSystemEventType.ASSIGNEE_ADDED,
                                "分配了经办人 @" + assignee))
                            .then();
                        Mono<Void> removedEvents = Flux.fromIterable(removedAssignees)
                            .concatMap(assignee -> createSystemEvent(
                                updatedIssue, operator,
                                IssueComment.IssueSystemEventType.ASSIGNEE_REMOVED,
                                "移除了经办人 @" + assignee))
                            .then();
                        return addedEvents.then(removedEvents).thenReturn(updatedIssue);
                    });
            });
    }

    private Mono<Void> createSystemEvent(
        Issue issue, String operator, IssueComment.IssueSystemEventType eventType, String message) {
        IssueComment event = new IssueComment();
        Metadata metadata = new Metadata();
        metadata.setGenerateName("ic-system-");
        event.setMetadata(metadata);

        IssueComment.IssueCommentSpec spec = new IssueComment.IssueCommentSpec();
        spec.setIssueName(issue.getMetadata().getName());
        spec.setQuoteCommentUid("");
        spec.setUserAgent("IssueSystemEvent");
        spec.setIpAddress("");
        spec.setOwner(operator);
        spec.setApproved(true);
        spec.setApprovedTime(Instant.now());
        spec.setAllowNotification(false);
        spec.setTop(false);
        spec.setHidden(false);
        spec.setSystemEvent(true);
        spec.setSystemEventType(eventType);

        IssueComment.IssueCommentContent content = new IssueComment.IssueCommentContent();
        content.setRaw(message);
        content.setHtml(HtmlUtils.htmlEscape(message));
        content.setMedium(List.of());
        spec.setContent(content);
        event.setSpec(spec);

        return client.create(event).then();
    }

    /**
     * 列出当前主体下可供选择的模版
     * @param subjectName
     * @return
     */
    @Override
    public Mono<IssueTemplateOptions> listIssueSelectTemplateOptions(String subjectName) {
        return client.fetch(IssueSubject.class, subjectName)
            .flatMap(issueSubject ->  Flux.fromIterable(issueSubject.getSpec().getIssueTemplates())
                .flatMap(templateName -> client.fetch(IssueTemplate.class, templateName)
                    .map(issueTemplate -> IssueTemplateOptions.IssueTemplateItem.from(issueTemplate))
                ).collectList()
                .map(templates -> {
                    IssueTemplateOptions options = new IssueTemplateOptions();
                    options.setIssueTemplateOptions(templates);
                    return options;
                })
            );
    }

    /**
     * 关闭issue的时候发送通知
     * @param issue
     * @param closedComment
     * @param closedOwner
     * @return
     */
    private Mono<Void> sendClosedIssueNotification(Issue issue, Set<String> participateUsers, String subjectDisplayName, String subjectType, String closedComment, String closedOwner) {
        Boolean approved = issue.getSpec().getApproved();
        String contentUrl;
        if(approved){
            contentUrl = externalLinkProcessor.processLink(issue.getStatus().getPermalink());
        }else{
            contentUrl = externalLinkProcessor.processLink(
                "/console/issues/list?approved=false");
        }
        return Flux.fromIterable(participateUsers).flatMap(participateUser -> {
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(issue.getApiVersion())
                .kind(issue.getKind())
                .name(issue.getMetadata().getName())
                .title(issue.getSpec().getTitle())
                .url(contentUrl)
                .build();
            String owner = issue.getSpec().getOwner();
            var emitReasonMono = notificationReasonEmitter.emit(Constant.MANAGER_CLOSED_ISSUE,
                builder -> {
                    var attributes = IssueClosedReasonData.builder()
                        .issueTitle(issue.getSpec().getTitle())
                        .issueClosedTime(issue.getSpec().getClosedAt()
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")))
                        .closedComment(closedComment)
                        .issuePermalink(contentUrl)
                        .issueOwner(issue.getSpec().getOwner())
                        .closedOwner(closedOwner)
                        .receiveOwner(participateUser)
                        .subjectDisplayName(subjectDisplayName)
                        .subjectType(subjectType)
                        .build();
                    builder.attributes(ReasonDataConverterUtils.toAttributeMap(attributes))
                        .author(UserIdentity.of(owner))
                        .subject(reasonSubject);
                });
            Mono<Void> subscribeMono = notificationSubscriptionHelper.subscribeClosedIssueNotify(UserIdentity.of(participateUser));
            return Mono.when(subscribeMono).then(emitReasonMono);
        }).then();

    }

    @Builder
    record IssueClosedReasonData(String issueTitle, String issueClosedTime, String closedComment, String issuePermalink, String issueOwner,String closedOwner,
                                 String receiveOwner, String subjectDisplayName, String subjectType) {
    }

}
