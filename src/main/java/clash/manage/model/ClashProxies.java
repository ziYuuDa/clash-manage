package clash.manage.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.StringReader;
import java.util.Date;

@Data
@Accessors(chain = true)
public class ClashProxies implements Cloneable {
    private Integer id;

    private String name;

    private Date expiredTime;

    private String tag;

    private String cdnName;

    private Integer del;

    private String value;

    private String modifyLog;

    private String realName;

    private String subscribeName;

    @Override
    @SneakyThrows
    public ClashProxies clone() {
        return (ClashProxies) super.clone();
    }

    public JSONObject convertValueToJson() {
        return YamlUtil.load(new StringReader(this.value), JSONObject.class);
    }

    public ClashProxies toOrigin() {
        JSONObject jsonObject = convertValueToJson();
        JSONObject wsOpts = jsonObject.getJSONObject("ws-opts");
        String host = wsOpts.getJSONObject("headers").getString("Host");
        jsonObject.put("server", host);
        wsOpts.remove("headers");
        jsonObject.put("ws-opts", wsOpts);
        this.value = jsonObject.toJSONString();
        return this;
    }

    public ClashProxies changeConfig(String key, String value) {
        JSONObject jsonObject = convertValueToJson();
        jsonObject.put(key, value);
        this.value = jsonObject.toJSONString();
        return this;
    }

    public ClashProxies changeNameToValue() {
        return changeConfig("name", this.name);
    }


    public ClashProxies recordModifyLog(String title) {
        String time = DateUtil.formatDate(new Date());
        String log = String.format("【%s】%s：%s%s", title, time, this.getValue(), this.getModifyLog() == null ? "" : "\n" + this.getModifyLog());
        this.setModifyLog(log);
        return this;
    }
}