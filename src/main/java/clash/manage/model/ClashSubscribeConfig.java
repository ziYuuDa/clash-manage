package clash.manage.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class ClashSubscribeConfig {
    private Integer id;

    private String name;

    private String url;

    private String websiteUrl;

    private Long flowTotal;

    private Long flowUsedUpload;

    private Long flowUsedDownload;

    private Long flowSurplus;

    private Date expireTime;

    private Integer refreshFlag;

    private String filterKeyWord;
}