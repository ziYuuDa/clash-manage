package clash.manage.controller;

import clash.manage.dao.*;
import clash.manage.model.*;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.expression.engine.spel.SpELEngine;
import cn.hutool.setting.yaml.YamlUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * clash接口
 */
@Slf4j
@RestController
public class ClashController {

    @Autowired
    private ClashBasicMapper basicMapper;
    @Autowired
    private ClashProxiesMapper proxiesMapper;
    @Autowired
    private ClashProxyGroupsMapper proxyGroupsMapper;
    @Autowired
    private ClashRulesMapper rulesMapper;
    @Autowired
    private CdnSpeedMapper cdnSpeedMapper;
    @Autowired
    private CdnConfigMapper cdnConfigMapper;
    @Autowired
    private ClashSubscribeConfigMapper subscribeConfigMapper;
    @Autowired
    private ScheduledTask scheduledTask;

    private static final List<String> direct_rule = Arrays.asList("REJECT", "DIRECT");

    private static final String first_parent_code = "0";

    @GetMapping(value = "/api/client/subscribe")
    public ResponseEntity subClash(Integer[] ver, String refresh) {
        if (Objects.equals("1", refresh)) {
            log.info("clash订阅,强制刷新clash节点");
            scheduledTask.refreshSubscribe();
        }
        String context = this.generateClashContext(CollUtil.addAll(ListUtil.toList(0), ver));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
        return new ResponseEntity<>(context, headers, HttpStatus.OK);
    }

    private List<ClashBasic> getNextList(ClashBasic basic) {
        String nextParentCode = first_parent_code.equals(basic.getCode()) ? first_parent_code :
                (first_parent_code.equals(basic.getParentCode()) ? basic.getCode() : basic.getParentCode() + "," + basic.getCode());
        return basicMapper.list(nextParentCode).stream().sorted(Comparator.comparing(ClashBasic::getSortOrId)).collect(Collectors.toList());
    }

    private final Map<String, Function<ClashBasic, Object>> getBasicConfigValueMap = new HashMap<String, Function<ClashBasic, Object>>() {{
        put("int", basic -> Integer.valueOf(basic.getValue()));
        put("boolean", basic -> Boolean.valueOf(basic.getValue()));
        put("string", ClashBasic::getValue);
        put("list", basic -> getNextList(basic).stream().map(ClashBasic::getValue).collect(Collectors.toList()));
        put("object", basic -> getBasicConfig(getNextList(basic), new JSONObject(16, true)));
    }};

    /**
     * 生成basic基本配置
     */
    private JSONObject getBasicConfig(List<ClashBasic> list, JSONObject result) {
        list.forEach(basic -> result.put(basic.getCode(), getBasicConfigValueMap.get(basic.getValueType().toLowerCase()).apply(basic)));
        return result;
    }

    private String generateClashContext(Collection<Integer> verList) {
        Map<String, CdnConfig> cdnConfigMap = cdnConfigMapper.list().stream().collect(Collectors.toMap(CdnConfig::getName, Function.identity()));
        Map<String, ClashSubscribeConfig> subscribeConfigMap = subscribeConfigMapper.list().stream().collect(Collectors.toMap(ClashSubscribeConfig::getName, Function.identity()));
        //---------基础配置-------------
        List<ClashBasic> clashBasicQueryParam = Collections.singletonList(new ClashBasic().setCode(first_parent_code).setValueType("object"));
        JSONObject result = getBasicConfig(clashBasicQueryParam, new JSONObject()).getJSONObject(first_parent_code);
        //---------代理配置-------------
        List<ClashProxies> clashProxies = proxiesMapper.list();
        clashProxies.stream().filter(x -> StrUtil.isNotBlank(x.getSubscribeName())).forEach(x -> x.setName(x.getSubscribeName() + "_" + x.getName()));
        clashProxies.forEach(x -> x.setRealName(x.getName()));
        clashProxies = this.dealCDN(clashProxies, cdnConfigMap);
        clashProxies.forEach(ClashProxies::changeNameToValue);
        clashProxies.sort(Comparator.comparing(ClashProxies::getName));
        List<JSONObject> proxies = clashProxies.stream().map(ClashProxies::convertValueToJson).collect(Collectors.toList());
        result.put("proxies", proxies);
        //---------组配置-------------
        List<ClashProxyGroups> proxyGroupsList = proxyGroupsMapper.list();
        proxyGroupsList.forEach(x -> x.handleSubscribeInfo(subscribeConfigMap));
        Map<String, ClashProxyGroups> codeAndProxyGroupsList = proxyGroupsList.stream().collect(Collectors.toMap(ClashProxyGroups::getCode, x -> x));
        result.put("proxy-groups", generateProxyGroup(clashProxies, proxyGroupsList, codeAndProxyGroupsList));
        //---------路由配置-------------
        result.put("rules", generateRuleGroup(verList, codeAndProxyGroupsList));
        StringWriter stringWriter = new StringWriter();
        YamlUtil.dump(result, stringWriter);
        return stringWriter.toString();
    }

    /**
     * 生成Rule路由配置
     */
    private List<String> generateRuleGroup(Collection<Integer> verList, Map<String, ClashProxyGroups> codeAndProxyGroupsList) {
        List<String> res = new ArrayList<>();
        List<ClashRules> rulesList = rulesMapper.list(verList).stream().sorted(Comparator.comparing(ClashRules::getSortOrId)).collect(Collectors.toList());
        Map<String, StringBuilder> skipRule = new HashMap<>();
        for (ClashRules rules : rulesList) {
            String groupCode = rules.getProxiesGroupCode();
            ClashProxyGroups clashProxyGroups1 = codeAndProxyGroupsList.get(groupCode);
            if (clashProxyGroups1 == null && !direct_rule.contains(groupCode)) {
                StringBuilder skip = skipRule.getOrDefault(groupCode, new StringBuilder());
                skip.append(rules.getHost() + ",");
                skipRule.put(groupCode, skip);
                continue;
            }
            String val = clashProxyGroups1 == null ? groupCode : clashProxyGroups1.getName();
            StringBuilder sb = new StringBuilder();
            sb.append(rules.getType()).append(",");
            sb.append((rules.getHost() == null ? "" : rules.getHost() + ","));
            sb.append(val);
            sb.append(CollUtil.join(StrUtil.split(rules.getExtendConfig(), ",", true, true), ",", ",", null));
            res.add(sb.toString());
        }
        if (!skipRule.isEmpty()) {
            log.info("节点订阅,因缺少分组，路由规则已取消：{}", JSON.toJSONString(skipRule));
        }
        return res;
    }

    /**
     * 生成ProxyGroup代理组配置
     */
    private List<JSONObject> generateProxyGroup(List<ClashProxies> proxiesList, List<ClashProxyGroups> proxyGroupsList, Map<String, ClashProxyGroups> codeAndProxyGroupsList) {
        List<JSONObject> res = new ArrayList<>();
        Map<String, String> groupCodeAndName = codeAndProxyGroupsList.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().getName()));
        direct_rule.forEach(x -> groupCodeAndName.put(x, x));
        for (ClashProxyGroups groups : proxyGroupsList) {
            if (res.stream().anyMatch(x -> x.getString("name").equals(groups.getName()))) {
                log.info("clash订阅,出现重复的group名称,code:{},name:{},跳过不处理", groups.getCode(), groups.getName());
                continue;
            }
            JSONObject proxies = new JSONObject(16, true);
            proxies.put("name", groups.getName());
            proxies.put("type", groups.getType());
            if (StrUtil.isNotBlank(groups.getUrl())) {
                proxies.put("url", groups.getUrl());
            }
            if (groups.getInterval() != null) {
                proxies.put("interval", groups.getInterval());
            }
            StrUtil.split(groups.getExtendConfig(), ",", true, true).stream()
                    .map(x -> x.split(":"))
                    .forEach(cfg -> proxies.put(cfg[0], cfg[1]));
            Set<String> pxyList = new LinkedHashSet<>();
            StrUtil.split(groups.getOtherGroupCode(), ",", true, true).stream().map(groupCodeAndName::get).filter(Objects::nonNull).forEach(pxyList::add);
            proxiesList.stream().filter(x -> isProxyInThisGroup(groups, x)).forEach(x -> pxyList.add(x.getName()));
            proxies.put("proxies", pxyList);
            res.add(proxies);
        }
        Set<String> removeEmptyList = new HashSet<>();
        this.removeEmptyProxyGroup(res, proxiesList, removeEmptyList);
        if (!removeEmptyList.isEmpty()) {
            log.info("因为不存在节点,这些策略组已删除:{}", removeEmptyList);
            Iterator<Map.Entry<String, ClashProxyGroups>> iterator = codeAndProxyGroupsList.entrySet().iterator();
            while (iterator.hasNext()) {
                if (removeEmptyList.contains(iterator.next().getValue().getName())) {
                    iterator.remove();
                }
            }
        }
        return res;
    }

    /**
     * 检查哪个组的节点信息是空，是的话则将这个组移除
     */
    public void removeEmptyProxyGroup(List<JSONObject> proxyGroup, List<ClashProxies> proxiesList, Collection<String> removeList) {
        boolean isRemoveFlag = false;
        Iterator<JSONObject> iterator = proxyGroup.iterator();
        while (iterator.hasNext()) {
            JSONObject next = iterator.next();
            List<String> proxies = next.getObject("proxies", ArrayList.class);
            proxies.removeIf(removeList::contains);
            if (proxies.isEmpty()) {
                isRemoveFlag = true;
                removeList.add(next.getString("name"));
                iterator.remove();
            }
            next.put("proxies", proxies);
        }
        if (isRemoveFlag || removeNotExistProxyGroup(proxyGroup, proxiesList)) {
            removeEmptyProxyGroup(proxyGroup, proxiesList, removeList);
        }
    }

    /**
     * 删除代理组中不存在的节点
     */
    public boolean removeNotExistProxyGroup(List<JSONObject> proxyGroup, List<ClashProxies> proxiesList) {
        boolean isRemoveFlag = false;
        for (JSONObject next : proxyGroup) {
            List<String> proxies = next.getObject("proxies", ArrayList.class);
            Iterator<String> iteratorProxies = proxies.iterator();
            while (iteratorProxies.hasNext()) {
                if (!isProxyGroupExist(proxyGroup, proxiesList, iteratorProxies.next())) {
                    isRemoveFlag = true;
                    iteratorProxies.remove();
                }
            }
            next.put("proxies", proxies);
        }
        return isRemoveFlag;
    }

    public boolean isProxyGroupExist(List<JSONObject> proxyGroup, List<ClashProxies> proxiesList, String groupName) {
        if (direct_rule.contains(groupName)) {
            return true;
        }
        for (ClashProxies clashProxies : proxiesList) {
            if (clashProxies.getName().equals(groupName)) {
                return true;
            }
        }
        for (JSONObject jsonObject : proxyGroup) {
            if (groupName.equals(jsonObject.getString("name"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 这个节点是否要加入到这个组
     *
     * @param group
     * @param clashProxy
     * @return
     */
    private boolean isProxyInThisGroup(ClashProxyGroups group, ClashProxies clashProxy) {
        Map<String, Object> map = new HashMap<>();
        map.put("subscribeFlag", StrUtil.isNotBlank(clashProxy.getSubscribeName()));
        map.put("subscribeName", clashProxy.getSubscribeName());
        map.put("cdnFlag", StrUtil.isNotBlank(clashProxy.getCdnName()));
        map.put("cdnName", clashProxy.getCdnName());
        map.put("name", clashProxy.getRealName());
        map.put("id", clashProxy.getId());
        map.put("tagList", StrUtil.split(clashProxy.getTag(), ',', true, true));
        try {
            Boolean flag = (Boolean) new SpELEngine().eval(group.getMatchProxyNodeExpression(), map, null);
            //log.info("表达式判断,节点名称:{},表达式:{},结果:{}", clashProxy.getName(), group.getMatchProxyNodeExpression(), flag);
            return flag;
        } catch (Exception e) {
            log.error("表达式处理异常,expression:{},param:{},msg:{}", group.getMatchProxyNodeExpression(), map, e.getMessage());
            return false;
        }
    }

    private List<ClashProxies> dealCDN(List<ClashProxies> clashProxies, Map<String, CdnConfig> cdnConfigMap) {
        Map<String, List<CdnSpeed>> cdnMap = new HashMap<>();
        for (CdnConfig cfg : cdnConfigMap.values()) {
            String cdnName = cfg.getName();
            List<CdnSpeed> cdnList = new ArrayList<>();
            cdnList.addAll(cdnSpeedMapper.listBestIp(cdnName));
            cdnList.addAll(cdnSpeedMapper.listGoodIp(cdnName, cfg.getGoodIpQuerySize()));
            cdnMap.put(cdnName, cdnList);
        }

        List<ClashProxies> res = new ArrayList<>();
        for (ClashProxies clashProxy : clashProxies) {
            String cdnName = clashProxy.getCdnName();
            if (StrUtil.isBlank(cdnName)) {
                res.add(clashProxy);
                continue;
            }
            this.addCdnNode(res, clashProxy, cdnMap.get(cdnName), cdnConfigMap.get(cdnName));
        }
        return res;
    }

    private void addCdnNode(List<ClashProxies> res, ClashProxies clashProxy, List<CdnSpeed> cdnList, CdnConfig cdnConfig) {
        res.add(clashProxy.clone().setName(clashProxy.getName() + "_origin").toOrigin());
        if (CollectionUtils.isEmpty(cdnList)) {
            return;
        }
        int g = 0;
        int b = 0;
        for (CdnSpeed speed : cdnList) {
            ClashProxies clone = clashProxy.clone();
            clone.setName(clashProxy.getName() + "_" + (speed.isBeastCdn() ? "b" + (++b) : "g" + (++g)));
            clone.changeConfig("server", speed.getIp());
            res.add(clone);
        }
    }
}
