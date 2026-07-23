package team.foxbridge.issue.entity;

import team.foxbridge.issue.extension.IssueComment;
import team.foxbridge.issue.vo.ContributorVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
* @description: issue 评论
* @className: ListedIssueComment
* @author: Akagi_Zen
* @date: 2025年05月26日 10:57
*/
@Data
@Builder
public class ListedIssueComment {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueComment issueComment;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ContributorVO contributorVo;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Stats stats;

}
