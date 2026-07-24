package team.foxbridge.issue.endpoint.uc;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import team.foxbridge.issue.entity.IssueStatusChangeParam;
import team.foxbridge.issue.entity.IssueTemplateRender;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.extension.IssueLabel;
import team.foxbridge.issue.query.IssueQuery;
import team.foxbridge.issue.service.IssueService;
import team.foxbridge.issue.service.IssueTemplateService;
import team.foxbridge.issue.service.RoleService;
import team.foxbridge.issue.util.AuthorityUtils;
import team.foxbridge.issue.entity.ListedIssue;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Comparator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.endpoint.CustomEndpoint;

import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 个人发布 issue 留言的 API
 * @author: Akagi_Zen
 * @date: 2025年03月06日 14:42
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcIssueEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/Issue";

    private final IssueService issueService;

    private final RoleService roleService;

    private final IssueTemplateService issueTemplateService;

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issues", this::listMyIssue, builder -> {
                builder.operationId("ListMyIssues")
                    .description("List My issues.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssue.class))
                    );
                IssueQuery.buildParameters(builder);
            })
            .GET("issues/content", this::fetchIssueContent, builder ->
                builder.operationId("FetchIssueContent")
                    .description("fetch issue content.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("issueName")
                        .in(ParameterIn.QUERY)
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(Issue.IssueContent.class)
                    )
            )
            .GET("issuetemplates/{templateName}", this::fetchIssueTemplateData, builder ->
                builder.operationId("FetchIssueTemplateData")
                    .description("fetch issue template.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("templateName")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(IssueTemplateRender.class)
                    )
            )
            .GET("issues/{name}", this::getMyIssue,
                builder -> builder.operationId("GetMyIssue")
                    .description("Get a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .POST("issues", this::createMyIssue,
                builder -> builder.operationId("CreateMyIssue")
                    .description("Create a My Issue.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Issue.class))
                        ))
                    .response(responseBuilder().implementation(Issue.class))
            )
            .PUT("issues/{name}", this::updateMyIssue,
                builder -> builder.operationId("UpdateMyIssue")
                    .description("Update a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Issue.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .PUT("issues/{name}/labels", this::updateIssueLabels,
                builder -> builder.operationId("UpdateIssueLabels")
                    .description("Update labels for an issue. Issue management permission is required.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(IssueLabelsUpdateParam.class))
                        ))
                    .response(responseBuilder().implementation(Issue.class))
            )
            .PUT("issues/{name}/assignees", this::updateIssueAssignees,
                builder -> builder.operationId("UpdateIssueAssignees")
                    .description("Update assignees for an issue. Issue management permission is required.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(IssueAssigneesUpdateParam.class))
                        ))
                    .response(responseBuilder().implementation(Issue.class))
            )
            .DELETE("issues/{name}", this::deleteMyIssue,
                builder -> builder.operationId("DeleteMyIssue")
                    .description("Delete a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder().implementation(Issue.class))
            )
            .GET("labels", this::listMyLabels, builder -> builder.operationId("ListMyLabels")
                    .description("List all issue labels.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.QUERY)
                        .description("Label name to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementationArray(String.class)
                    ))
            .GET("users", this::searchAssignableUsers,
                builder -> builder.operationId("SearchAssignableUsers")
                    .description("Search users that can be assigned to an issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("keyword")
                        .in(ParameterIn.QUERY)
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementationArray(AssignableUser.class)
                    ))
            .PUT("issuestatus", this::updateMyIssueStatus,
                builder -> builder.operationId("UpdateMyIssueStatus")
                    .description("Update a My Issue status.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(IssueStatusChangeParam.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .build();
    }

    private Mono<ServerResponse> listMyIssue(ServerRequest request) {
        return getCurrentUser()
            .map(user -> new IssueQuery(request.exchange(), user.getName()))
            .flatMap(issueService::listIssue)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<ServerResponse> fetchIssueContent(ServerRequest request) {
        String issueName = request.queryParam("issueName").get();
        return issueService.getIssueContent(issueName)
            .flatMap(issueContent -> ServerResponse.ok().bodyValue(issueContent));
    }

    private Mono<ServerResponse> fetchIssueTemplateData(ServerRequest request) {
        String issueTemplate = request.pathVariable("templateName");
        return issueTemplateService.buildTemplateData(issueTemplate)
            .flatMap(templateData -> ServerResponse.ok().bodyValue(templateData));
    }

    private Mono<ServerResponse> deleteMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return  getMyIssueDetail(name)
            .flatMap(issue -> roleService.getCurrentUser()
                .flatMap(curUser -> {
                    var roles = AuthorityUtils.authoritiesToRoles(curUser.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.ISSUE_MESSAGE_MANAGEMENT_ROLE_NAME,
                                AuthorityUtils.SUPER_ROLE_NAME))
                        .flatMap(result -> {
                            if (result) {
                                return issueService.deleteBy(issue);
                            }else{
                                if (curUser.getName().equals(issue.getSpec().getOwner())) {
                                    return issueService.deleteBy(issue);
                                } else {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only issue owner can delete it"));
                                }
                            }
                        });
                }))
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<ServerResponse> updateMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(oldIssue -> {
                Issue.IssueSpec oldSpec = oldIssue.getSpec();
                return request.bodyToMono(Issue.class)
                    .doOnNext(newIssue -> {
                        Issue.IssueSpec newSpec = newIssue.getSpec();
                        newSpec.setOwner(oldSpec.getOwner());
                        newSpec.setReleaseTime(oldSpec.getReleaseTime());
                        // Every update needs to be re-reviewed. 暂时去掉 后续加入配置根据使用者情况自定义更新issue后是否重新审核
                        // newSpec.setApproved(false);
                    })
                    .flatMap(issueService::updateBy);
            })
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<ServerResponse> updateIssueLabels(ServerRequest request) {
        var name = request.pathVariable("name");
        return requireIssueManagementPermission()
            .then(request.bodyToMono(IssueLabelsUpdateParam.class))
            .flatMap(param -> {
                var labels = param.getLabels() == null
                    ? Set.<String>of()
                    : new LinkedHashSet<>(param.getLabels());
                return validateExtensionNames(IssueLabel.class, labels, "Unknown issue label: ")
                    .then(client.get(Issue.class, name))
                    .flatMap(issue -> {
                        issue.getSpec().setLabels(labels);
                        return issueService.updateBy(issue);
                    });
            })
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<ServerResponse> updateIssueAssignees(ServerRequest request) {
        var name = request.pathVariable("name");
        return requireIssueManagementPermission()
            .then(request.bodyToMono(IssueAssigneesUpdateParam.class))
            .flatMap(param -> {
                var assignees = param.getAssignees() == null
                    ? Set.<String>of()
                    : new LinkedHashSet<>(param.getAssignees());
                return validateExtensionNames(User.class, assignees, "Unknown user: ")
                    .then(client.get(Issue.class, name))
                    .flatMap(issue -> {
                        issue.getSpec().setAssignees(assignees);
                        return issueService.consoleUpdateIssue(issue);
                    });
            })
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<Void> requireIssueManagementPermission() {
        return roleService.getCurrentUser()
            .flatMap(currentUser -> {
                var roles = AuthorityUtils.authoritiesToRoles(currentUser.getAuthorities());
                return roleService.joint(roles,
                    Set.of(AuthorityUtils.ISSUE_MESSAGE_MANAGEMENT_ROLE_NAME,
                        AuthorityUtils.SUPER_ROLE_NAME));
            })
            .flatMap(hasPermission -> hasPermission
                ? Mono.<Void>empty()
                : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Issue management permission is required")));
    }

    private <T extends run.halo.app.extension.Extension> Mono<Void> validateExtensionNames(
        Class<T> extensionType, Set<String> names, String messagePrefix) {
        return Flux.fromIterable(names)
            .concatMap(name -> client.fetch(extensionType, name)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    messagePrefix + name))))
            .then();
    }

    private Mono<ServerResponse> getMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(issueMessage -> ServerResponse.ok().bodyValue(issueMessage));
    }

    private Mono<Issue> getMyIssueDetail(String issueName) {
        return getCurrentUser()
            .flatMap(user -> issueService.getByUsername(issueName, user.getName())
                .switchIfEmpty(
                    Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The issue message was not found or deleted"))
                )
            );
    }

    private Mono<ServerResponse> createMyIssue(ServerRequest request) {
        return getCurrentUser()
            .flatMap(user -> request.bodyToMono(Issue.class)
                .flatMap(issueMessage -> {
                    issueMessage.getSpec().setApproved(false);
                    issueMessage.getSpec().setOwner(user.getName());
                    var roles = AuthorityUtils.authoritiesToRoles(user.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.ISSUE_PUBLISH_APPROVAL_ROLE_NAME,
                                AuthorityUtils.SUPER_ROLE_NAME))
                        .doOnNext(result -> {
                            if (result) {
                                // If it is a user with audit authority, there is no need to review.
                                issueMessage.getSpec().setApproved(true);
                                issueMessage.getSpec().setApprovedTime(Instant.now());
                                // 拥有发布issue无需审核权限的时候 则自动生成访问链接
                                issueMessage.getStatus().setPermalink("/issues/" + issueMessage.getMetadata().getName());
                            }
                        })
                        .thenReturn(issueMessage);
                })
            )
            .flatMap(issueService::create)
            .flatMap(createdIssue -> ServerResponse.ok().bodyValue(createdIssue));
    }

    private Mono<Authentication> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication);
    }

    private Mono<ServerResponse> listMyLabels(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return getCurrentUser()
            .map(user -> new IssueQuery(request.exchange(), user.getName()))
            .flatMapMany(issueService::listAllLabels)
            .filter(labelName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(labelName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> searchAssignableUsers(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("").trim();
        return requireIssueManagementPermission()
            .thenMany(client.list(User.class,
                user -> {
                    String name = user.getMetadata().getName();
                    String displayName = user.getSpec().getDisplayName();
                    return StringUtils.isBlank(keyword)
                        || StringUtils.containsIgnoreCase(name, keyword)
                        || StringUtils.containsIgnoreCase(displayName, keyword);
                },
                Comparator.comparing(user -> user.getMetadata().getName())))
            .take(50)
            .map(AssignableUser::from)
            .collectList()
            .flatMap(users -> ServerResponse.ok().bodyValue(users));
    }

    private Mono<ServerResponse> updateMyIssueStatus(ServerRequest request){
        // 从请求体获取 IssueClosedParam 对象
        return request.bodyToMono(IssueStatusChangeParam.class)
            .flatMap(issueStatusChangeParam -> {
                String issueName = issueStatusChangeParam.getIssueName();
                if(issueStatusChangeParam.getIssueState().equals(Issue.IssueState.CLOSED)){
                    if (StringUtils.isBlank(issueStatusChangeParam.getChangeComment())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "未填写关闭原因"));
                    }
                    return client.fetch(Issue.class, issueName)
                        .flatMap(issue -> roleService.getCurrentUser()
                            .flatMap(curUser -> {
                                return hasIssueManagementPermission(curUser, issue)
                                    .flatMap(hasPermission -> {
                                        if (hasPermission) {
                                            return issueService.closeIssue(issue, issueStatusChangeParam.getChangeComment(), curUser.getName());
                                        } else {
                                            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only issue owner, assignee or administrator can close it"));
                                        }
                                    });
                            }));
                }else if (issueStatusChangeParam.getIssueState().equals(Issue.IssueState.PROGRESS)){
                    return client.get(Issue.class, issueName)
                        .flatMap(issue -> roleService.getCurrentUser()
                            .flatMap(curUser -> {
                                return hasIssueManagementPermission(curUser, issue)
                                    .flatMap(hasPermission -> {
                                        if (hasPermission) {
                                            return issueService.reopenIssue(issue, curUser.getName());
                                        } else {
                                            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only issue owner, assignee or administrator can reopen it"));
                                        }
                                    });
                            }));
                }else{
                    // 添加默认情况，返回错误响应
                    return client.get(Issue.class, issueName)
                        .flatMap(issue -> roleService.getCurrentUser()
                            .flatMap(curUser -> {
                                boolean isAssignedOwner = issue.getSpec().getAssignees().size() > 0 && issue.getSpec().getAssignees().contains(curUser.getName());
                                if (curUser.getName().equals(issue.getSpec().getOwner()) || isAssignedOwner) {
                                    return issueService.setAwaitIssue(issue, curUser.getName());
                                } else {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only issue owner and assignee can set await status"));
                                }
                            }));
                }
            })
            .flatMap(updatedRes -> ServerResponse.ok().bodyValue(updatedRes));
    }




    /**
     * 判断 issue 状态操作权限。
     * 创建者、被分配处理人、issue 管理员、超级管理员均可操作。
     */
    private Mono<Boolean> hasIssueManagementPermission(Authentication user, Issue issue) {
        boolean isOwner = user.getName().equals(issue.getSpec().getOwner());
        boolean isAssignee = issue.getSpec().getAssignees() != null
            && issue.getSpec().getAssignees().contains(user.getName());

        var roles = AuthorityUtils.authoritiesToRoles(user.getAuthorities());
        return roleService.joint(roles,
                Set.of(AuthorityUtils.ISSUE_MESSAGE_MANAGEMENT_ROLE_NAME,
                    AuthorityUtils.SUPER_ROLE_NAME))
            .map(isManager -> isOwner || isAssignee || isManager);
    }

    @Data
    public static class IssueLabelsUpdateParam {
        private Set<String> labels = new LinkedHashSet<>();
    }

    @Data
    public static class IssueAssigneesUpdateParam {
        private Set<String> assignees = new LinkedHashSet<>();
    }

    @Data
    public static class AssignableUser {
        private String name;
        private String displayName;
        private String avatar;

        static AssignableUser from(User user) {
            var result = new AssignableUser();
            result.setName(user.getMetadata().getName());
            result.setDisplayName(StringUtils.defaultIfBlank(
                user.getSpec().getDisplayName(), user.getMetadata().getName()));
            result.setAvatar(user.getSpec().getAvatar());
            return result;
        }
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.issue.foxbridge.team/v1alpha1");
    }



}
