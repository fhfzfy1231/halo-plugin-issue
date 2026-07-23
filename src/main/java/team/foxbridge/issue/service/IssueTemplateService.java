package team.foxbridge.issue.service;

import team.foxbridge.issue.entity.IssueTemplateOptions;
import team.foxbridge.issue.entity.IssueTemplateRender;
import team.foxbridge.issue.extension.IssueSubject;
import team.foxbridge.issue.extension.IssueTemplate;
import team.foxbridge.issue.query.IssueTemplateQuery;
import team.foxbridge.issue.entity.ListedIssueTemplate;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 接口功能: issue留言模版接口
 * @author: Akagi_Zen
 * @date: 2025年03月17日 11:39
 */
public interface IssueTemplateService {

    Mono<IssueTemplate> create(IssueTemplate issueTemplate);

    Mono<ListResult<ListedIssueTemplate>> listIssueTemplate(IssueTemplateQuery query);

    /**
     * 根据主题类型过滤 Issue应有的模版
     * @param subjectTypeName
     * @return
     */
    Mono<IssueTemplateOptions> listIssueTemplateOptions(String subjectTypeName, String subjectName);

    /**
     * 构建 Issue 模版数据
     * @param templateName
     * @return
     */
    Mono<IssueTemplateRender> buildTemplateData(String templateName);


}
