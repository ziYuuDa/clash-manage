package clash.manage.controller;

import clash.manage.dao.ClashProxiesMapper;
import clash.manage.dao.ClashSubscribeConfigMapper;
import clash.manage.model.ClashProxies;
import clash.manage.model.ClashSubscribeConfig;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.setting.yaml.YamlUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 定时任务
 */
@Slf4j
@Component
public class ScheduledTask {

    @Autowired
    private ClashProxiesMapper proxiesMapper;
    @Autowired
    private ClashSubscribeConfigMapper subscribeConfigMapper;

    private final static String taskName = "刷新订阅";

    /**
     * 每天1点2分0秒执行
     */
    @Scheduled(cron = "0 2 1 * * ?")
    public void refreshSubscribe() {
        TimeInterval timer = DateUtil.timer();
        CompletableFuture<?>[] completableFutureList = subscribeConfigMapper.list().stream()
                .filter(x -> x.getRefreshFlag() == 1)
                .filter(x -> x.getExpireTime() == null || DateUtil.between(x.getExpireTime(),new Date(), DateUnit.DAY, false) < 5)
                .map(x -> CompletableFuture.runAsync(() -> refresh(timer, x)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutureList).join();
        log.info("{},刷新完成,处理{}条,耗时{}ms", taskName, completableFutureList.length, timer.interval());
    }

    private void refresh(TimeInterval timer, ClashSubscribeConfig config) {
        String subscribeName = config.getName();
        String body = null;
        try (HttpResponse response = HttpRequest.get(config.getUrl())
                .header("user-agent", "ClashforWindows/0.20.39")
                .timeout(15_000)
                .execute()) {
            if (!response.isOk()) {
                log.error("{},{},httpCode失败:{}", taskName, subscribeName, response.getStatus());
                return;
            }
            body = response.body();
            if (StrUtil.isBlank(body)) {
                log.error("{},{},响应结果为空,不再处理,header:{}", taskName, subscribeName, response.headers());
                return;
            }
            JSONObject jsonObject = Optional.ofNullable(YamlUtil.load(new StringReader(body), JSONObject.class)).orElse(new JSONObject());
            JSONArray proxies = jsonObject.getJSONArray("proxies");
            if (ObjectUtil.isEmpty(proxies)) {
                log.error("{},{},proxies为空,不再处理,header:{},boy:{}", taskName, subscribeName, response.headers(), jsonObject);
                return;
            }
            //log.info("{},{},获取成功,header:{}", taskName, subscribeName, response.headers());
            this.updateClashSubscribeConfig(config, response);
            Map<String, ClashProxies> proxiesMap = proxiesMapper.listBySubscribeName(subscribeName).stream().collect(Collectors.toMap(ClashProxies::getName, x -> x));
            Collection<ClashProxies> deleteList;
            Collection<ClashProxies> updateList = new ArrayList<>();
            Collection<ClashProxies> insertList = new ArrayList<>();
            //配置文件
            List<String> ignoreProxy = new ArrayList<>();
            List<String> ignoreKeyword = StrUtil.split(config.getFilterKeyWord(), ',', true, true);
            for (int i = 0; i < proxies.size(); i++) {
                JSONObject pxy = proxies.getJSONObject(i);
                String name = pxy.getString("name");
                if (ignoreKeyword.stream().anyMatch(name::contains)) {
                    ignoreProxy.add(name);
                } else {
                    dealData(config, proxiesMap.remove(name), updateList, insertList, name, pxy.toString(), subscribeName);
                }
            }
            deleteList = proxiesMap.values().stream().filter(x -> x.getDel() == 1).peek(x -> x.recordModifyLog("删").setDel(0)).collect(Collectors.toList());
            insertList.forEach(clashProxies -> proxiesMapper.insert(clashProxies));
            updateList.forEach(clashProxies -> proxiesMapper.update(clashProxies));
            deleteList.forEach(clashProxies -> proxiesMapper.update(clashProxies));
            log.info("{},{},更新完成,耗时:{}ms,新增:{},修改:{},删除:{},过滤不处理:{}", taskName, subscribeName, timer.interval(),
                    formatChangeInfo(insertList), formatChangeInfo(updateList), formatChangeInfo(deleteList), ignoreProxy);
        } catch (Exception e) {
            log.error("{},{},body:{},异常:", taskName, subscribeName, body, e);
        }
    }

    private String formatChangeInfo(Collection<ClashProxies> list) {
        return StrUtil.format("{}条:{}", list.size(), list.stream().map(ClashProxies::getName).collect(Collectors.toList()));
    }

    private void dealData(ClashSubscribeConfig config, ClashProxies dbData, Collection<ClashProxies> updateList, Collection<ClashProxies> insertList,
                          String name, String value, String subscribeName) {
        ClashProxies clashProxies = new ClashProxies()
                .setExpiredTime(config.getExpireTime())
                .setName(name)
                .setSubscribeName(subscribeName)
                .setValue(value)
                .setDel(1);
        if (dbData == null) {
            //新增
            insertList.add(clashProxies.recordModifyLog("增"));
            return;
        }
        clashProxies.setId(dbData.getId()).setModifyLog(dbData.getModifyLog());
        if (dbData.getDel() == 0) {
            updateList.add(clashProxies.recordModifyLog("启"));
            return;
        }
        if (DateUtil.compare(config.getExpireTime(), dbData.getExpiredTime()) != 0
                || !Objects.equals(dbData.getValue(), value)) {
            if (!Objects.equals(dbData.getValue(), value)) {
                clashProxies = clashProxies.recordModifyLog("改");
            }
            updateList.add(clashProxies);
        }
    }

    private void updateClashSubscribeConfig(ClashSubscribeConfig config, HttpResponse response) {
        String userInfo = response.header("subscription-userinfo");
        if (StrUtil.isBlank(userInfo)) {
            return;
        }
        Map<String, String> map = Arrays.stream(userInfo.split(";"))
                .map(x -> x.split("="))
                .filter(x -> x.length == 2 && ObjectUtil.isAllNotEmpty(x[0], x[1]))
                .collect(Collectors.toMap(x -> x[0].trim(), x -> x[1].trim()));
        Long upload = MapUtil.getLong(map, "upload");
        Long download = MapUtil.getLong(map, "download");
        Long total = MapUtil.getLong(map, "total");
        Long expire = MapUtil.getLong(map, "expire");
        config.setFlowTotal(total);
        config.setFlowUsedUpload(upload);
        config.setFlowUsedDownload(download);
        if (total != null) {
            config.setFlowSurplus(NumberUtil.sub(total, upload, download).longValue());
        }
        config.setExpireTime(expire == null ? null : new Date(expire * 1000));
        String webPageUrl = response.header("profile-web-page-url");
        if (StrUtil.isNotBlank(webPageUrl)) {
            config.setWebsiteUrl(webPageUrl);
        }
        subscribeConfigMapper.update(config);
    }
}
