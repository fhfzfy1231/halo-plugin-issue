package team.foxbridge.issue.entity;

import team.foxbridge.issue.extension.IssueTemplate;
import lombok.Data;
import java.util.List;

/**
 * @description:
 * @className: IssueTemplateRender
 * @author: Akagi_Zen
 * @date: 2025年07月13日 15:18
 */
@Data
public class IssueTemplateRender {

    private String displayName;

    private List<IssueTemplate.TemplateField> components;

    private List<String> annotationFields;

}
