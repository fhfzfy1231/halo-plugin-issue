package team.foxbridge.issue.event;

import team.foxbridge.issue.extension.Issue;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.List;

/**
 * @description: issue创建事件
 * @className: HasNewIssueEvent
 * @author: Akagi_Zen
 * @date: 2025年05月27日 11:08
 */
@Getter
public class IssueCreatedEvent extends ApplicationEvent {

    private final String issueName;

    public IssueCreatedEvent(Object source, String issueName) {
        super(source);
        this.issueName = issueName;
    }

}



