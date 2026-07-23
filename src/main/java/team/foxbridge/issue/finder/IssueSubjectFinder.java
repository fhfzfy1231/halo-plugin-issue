package team.foxbridge.issue.finder;

import team.foxbridge.issue.entity.IssueSubjectStats;
import team.foxbridge.issue.extension.IssueSubject;
import team.foxbridge.issue.vo.IssueSubjectVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * @author: Akagi_Zen
 * @date: 2025年06月08日 10:54
 * @description:
 */
public interface IssueSubjectFinder {

    Mono<IssueSubjectVO> get(String issueSubjectName);

    Mono<IssueSubjectBasicInfo> getSubjectBasicInfo(String issueSubjectName);

    Mono<IssueSubjectStats> getSubjectStats(String subjectName);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class IssueSubjectBasicInfo{
        private String name;
        private IssueSubject.SubjectType subjectType;
        private String title;
        private List<IssueTemplateInfo> issueTemplates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class IssueTemplateInfo{
        private String templateName;
        private String metadataName;
        private String description;
    }

}
