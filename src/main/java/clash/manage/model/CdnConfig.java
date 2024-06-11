package clash.manage.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class CdnConfig {

    private Integer id;

    private String name;

    private Long goodIpSaveSize;

    private Integer goodIpQuerySize;

    private BigDecimal goodIpSpeedLimit;
}