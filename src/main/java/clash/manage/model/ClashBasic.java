package clash.manage.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ClashBasic {
    private Integer id;

    private Integer sort;

    public int getSortOrId() {
        return sort == null ? id : sort;
    }

    private String parentCode;

    private String code;

    private String valueType;

    private String value;

    private Integer del;

    private String desc;
}