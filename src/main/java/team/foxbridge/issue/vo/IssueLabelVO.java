package team.foxbridge.issue.vo;

import lombok.Builder;
import lombok.Value;

/**
 *
 * @author: Akagi_Zen
 * @date: 2025年03月10日 14:51
 */
@Value
@Builder
public class IssueLabelVO {

    String name;

    String permalink;

    Integer momentCount;

}
