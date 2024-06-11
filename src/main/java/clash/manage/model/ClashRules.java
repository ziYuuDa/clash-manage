package clash.manage.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ClashRules {
    private Integer id;

    private Integer sort;

    public int getSortOrId() {
        return sort == null ? id : sort;
    }

    private String type;

    private String host;

    private String remark;

    private String proxiesGroupCode;

    private Integer ver;

    private String extendConfig;

    private Integer del;
}