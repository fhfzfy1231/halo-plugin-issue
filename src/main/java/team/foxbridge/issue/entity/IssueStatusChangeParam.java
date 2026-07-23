package team.foxbridge.issue.entity;

import team.foxbridge.issue.extension.Issue;
import lombok.Data;

/**
 * @description:
 * @className: IssueStatusChangeParam
 * @author: Akagi_Zen
 * @date: 2025年07月31日 13:24
 */
@Data
public class IssueStatusChangeParam{

    private String changeComment;

    private String issueName;

    private Issue.IssueState issueState;
}
