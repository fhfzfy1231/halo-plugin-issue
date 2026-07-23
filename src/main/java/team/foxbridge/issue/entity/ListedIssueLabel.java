package team.foxbridge.issue.entity;

import team.foxbridge.issue.extension.IssueLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @description:
 * @className: ListedIssueLbael
 * @author: Akagi_Zen
 * @date: 2025年06月26日 09:56
 */
@Data
@Builder
public class ListedIssueLabel {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueLabel issueLabel;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer issueNumber;

    @Schema(description = "主体显示名称")
    private String subjectDisplayName;

}
