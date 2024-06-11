package clash.manage.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class ClashProxyGroups {
    private Integer id;

    private String code;

    private String name;

    private String url;

    private Integer interval;

    private String type;

    private String otherGroupCode;

    private String matchProxyNodeExpression;

    private String extendConfig;

    private String subscribeInfo;

    private Integer del;

    public void handleSubscribeInfo(Map<String, ClashSubscribeConfig> subscribeConfigMap) {
        if (StrUtil.isBlank(this.subscribeInfo)) {
            return;
        }
        ClashSubscribeConfig clashSubscribeConfig = subscribeConfigMap.get(this.subscribeInfo);
        if (clashSubscribeConfig == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (clashSubscribeConfig.getFlowSurplus() != null) {
            sb.append("剩余");
            sb.append(DataSizeUtil.format(clashSubscribeConfig.getFlowSurplus()));
        }
        if (clashSubscribeConfig.getExpireTime() != null) {
            sb.append("到期");
            sb.append(DateUtil.formatDate(clashSubscribeConfig.getExpireTime()));
        }
        if (sb.length() > 0) {
            sb.insert(0, "【").append("】");
            this.setName(this.getName() + sb);
        }
    }
}