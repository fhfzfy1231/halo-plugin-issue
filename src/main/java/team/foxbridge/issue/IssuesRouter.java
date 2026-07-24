package team.foxbridge.issue;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;
import team.foxbridge.issue.entity.IssueSubjectStats;
import team.foxbridge.issue.entity.IssueTemplateOptions;
import team.foxbridge.issue.extension.Issue;
import team.foxbridge.issue.extension.IssueLabel;
import team.foxbridge.issue.extension.IssueTemplate;
import team.foxbridge.issue.finder.IssueFinder;
import team.foxbridge.issue.service.RoleService;
import team.foxbridge.issue.service.SettingConfigGetter;
import team.foxbridge.issue.vo.IssueVO;
import run.halo.app.infra.ExternalUrlSupplier;

/**
 * 全局 Issue 前台路由。
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class IssuesRouter {

    private final SettingConfigGetter settingConfigGetter;
    private final IssueFinder issueFinder;
    private final TemplateNameResolver templateNameResolver;
    private final PluginContext pluginContext;
    private final RoleService roleService;
    private final ReactiveExtensionClient client;
    private final ExternalUrlSupplier externalUrlSupplier;

    @Bean
    RouterFunction<ServerResponse> issueRouterFunction() {
        return route(GET("/issues"), this::renderIssueList)
            .andRoute(GET("/issues/page/{page:\\d+}"), this::renderIssueList)
            .andRoute(GET("/issues/new"), this::renderNewIssue)
            .andRoute(GET("/issues/{issueName}"), this::renderIssueDetail);
    }

    private Mono<ServerResponse> renderIssueList(ServerRequest request) {
        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "issues")
            .flatMap(templateName -> {
                Map<String, Object> model = new HashMap<>();
                model.put("issueItems", issuePageList(request));
                model.put("issueStats", globalIssueStats());
                model.put("issueTemplates", listGlobalTemplates());
                buildCommonVariables(model);
                return ServerResponse.ok().render(templateName, model);
            });
    }

    private Mono<ServerResponse> renderIssueDetail(ServerRequest request) {
        String issueName = request.pathVariable("issueName");
        return issueFinder.get(issueName)
            .flatMap(issue -> templateNameResolver
                .resolveTemplateNameOrDefault(request.exchange(), "issue")
                .flatMap(templateName -> {
                    Map<String, Object> model = new HashMap<>();
                    model.put("issueVO", issue);
                    model.put("issueComments", issueFinder.listAllIssueComments(issueName));
                    model.put("issueStats", globalIssueStats());
                    model.put("issueTemplates", listGlobalTemplates());
                    model.put("availableIssueLabels", listAvailableLabels());
                    model.put("issueCosedComment", getIssueClosedComment());
                    buildCommonVariables(model);
                    return ServerResponse.ok().render(templateName, model);
                }))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> renderNewIssue(ServerRequest request) {
        return roleService.getCurrentUser()
            .flatMap(currentUser -> {
                if ("anonymousUser".equals(currentUser.getName())) {
                    String redirect = "/login?redirect_uri=" + request.uri();
                    return ServerResponse.status(HttpStatus.FOUND)
                        .location(URI.create(redirect))
                        .build();
                }
                return templateNameResolver
                    .resolveTemplateNameOrDefault(request.exchange(), "newIssue")
                    .flatMap(templateName -> {
                        Map<String, Object> model = new HashMap<>();
                        model.put("issueStats", globalIssueStats());
                        model.put("issueTemplates", listGlobalTemplates());
                        buildCommonVariables(model);
                        return ServerResponse.ok().render(templateName, model);
                    });
            });
    }

    private Mono<UrlContextListResult<IssueVO>> issuePageList(ServerRequest request) {
        int pageNum = NumberUtils.toInt(request.pathVariables().get("page"), 1);
        String issueState = request.queryParam("issueState").orElse("all");
        String path = request.path();
        return settingConfigGetter.getIssuesBasic()
            .map(SettingConfigGetter.IssuesBasic::getPageSize)
            .filter(pageSize -> pageSize > 0)
            .defaultIfEmpty(10)
            .flatMap(pageSize -> issueFinder.list(pageNum, pageSize, issueState)
                .map(list -> new UrlContextListResult.Builder<IssueVO>()
                    .listResult(list)
                    .nextUrl(PageUrlUtils.nextPageUrl(path, totalPage(list)))
                    .prevUrl(PageUrlUtils.prevPageUrl(path))
                    .build()));
    }

    private Mono<IssueSubjectStats> globalIssueStats() {
        ListOptions options = ListOptions.builder()
            .fieldQuery(equal("spec.approved", Boolean.TRUE.toString()))
            .build();
        return client.listAll(Issue.class, options,
                Sort.by("spec.releaseTime").descending())
            .collectList()
            .map(issues -> IssueSubjectStats.builder()
                .totalIssue(issues.size())
                .progressIssue((int) issues.stream()
                    .filter(issue -> issue.getStatus() != null
                        && issue.getStatus().getState() == Issue.IssueState.PROGRESS)
                    .count())
                .awaitIssue((int) issues.stream()
                    .filter(issue -> issue.getStatus() != null
                        && issue.getStatus().getState() == Issue.IssueState.AWAIT)
                    .count())
                .closedIssue((int) issues.stream()
                    .filter(issue -> issue.getStatus() != null
                        && issue.getStatus().getState() == Issue.IssueState.CLOSED)
                    .count())
                .labels(0)
                .awaitApproved(0)
                .build())
            .defaultIfEmpty(IssueSubjectStats.empty());
    }

    private Mono<IssueTemplateOptions> listGlobalTemplates() {
        ListOptions options = ListOptions.builder()
            .fieldQuery(equal("spec.scope", IssueTemplate.IssueTemplateScope.GLOBAL.name()))
            .build();
        return client.listAll(IssueTemplate.class, options,
                Sort.by("metadata.creationTimestamp").descending())
            .map(IssueTemplateOptions.IssueTemplateItem::from)
            .collectList()
            .map(items -> {
                IssueTemplateOptions result = new IssueTemplateOptions();
                result.setIssueTemplateOptions(items);
                return result;
            });
    }

    private Mono<java.util.List<IssueLabel>> listAvailableLabels() {
        var options = ListOptions.builder()
            .fieldQuery(equal("spec.scope", IssueLabel.LabelScope.GLOBAL.name()))
            .build();
        return client.listAll(IssueLabel.class, options,
                Sort.by("metadata.creationTimestamp").ascending())
            .collectList();
    }

    private void buildCommonVariables(Map<String, Object> model) {
        model.put("pluginVersion", pluginContext.getVersion());
        model.put("siteHomeUrl", externalUrlSupplier.get().toString());
        model.put("issueAvatarMode", getSetting(
            SettingConfigGetter.IssuesBasic::getDefaultAvatarMode, "default"));
        model.put("contentStyle", getSetting(
            SettingConfigGetter.IssuesBasic::getContentStyle, "tailwind"));
        model.put("diceBarAvatarStyle", getSetting(
            SettingConfigGetter.IssuesBasic::getDiceBarAvatarStyle, "initials"));
        model.put("diceBarAvatarSize", getIntegerSetting());
    }

    private Mono<String> getIssueClosedComment() {
        return getSetting(SettingConfigGetter.IssuesBasic::getDefaultClosedComment,
            "Issue已解决");
    }

    private Mono<String> getSetting(
        java.util.function.Function<SettingConfigGetter.IssuesBasic, String> getter,
        String fallback) {
        return settingConfigGetter.getIssuesBasic()
            .flatMap(settings -> Mono.justOrEmpty(getter.apply(settings)))
            .filter(value -> !value.isBlank())
            .defaultIfEmpty(fallback);
    }

    private Mono<Integer> getIntegerSetting() {
        return settingConfigGetter.getIssuesBasic()
            .flatMap(settings -> Mono.justOrEmpty(settings.getDiceBarAvatarSize()))
            .filter(size -> size > 0)
            .defaultIfEmpty(36);
    }
}
