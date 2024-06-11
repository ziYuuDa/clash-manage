package clash.manage.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class CdnSpeed {

    private Integer id;

    private String ip;

    private String cdnName;

    private BigDecimal speed;

    private Integer count;

    private Integer del;

    /**
     * 是否是最佳ip
     */
    public boolean isBeastCdn() {
        return id < 1;
    }
}