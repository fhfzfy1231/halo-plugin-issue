package team.foxbridge.issue.entity;

import team.foxbridge.issue.extension.IssueTemplate;
import team.foxbridge.issue.vo.ContributorVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 功能描述
 *
 * @author: Akagi_Zen
 * @date: 2025年03月17日 11:50
 */
@Data
@Builder
public class ListedIssueTemplate {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueTemplate issueTemplate;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ContributorVO contributorVo;

    private String subjectDisplayName;

}
