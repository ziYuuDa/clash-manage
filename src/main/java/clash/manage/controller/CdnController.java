package clash.manage.controller;

import clash.manage.dao.CdnConfigMapper;
import clash.manage.dao.CdnSpeedMapper;
import clash.manage.model.CdnConfig;
import clash.manage.model.CdnSpeed;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * cdn相关接口
 */
@Slf4j
@RestController
public class CdnController {

    @Autowired
    private CdnSpeedMapper cdnSpeedMapper;
    @Autowired
    private CdnConfigMapper cdnConfigMapper;

    @PostMapping(value = "/api/upload/cdn/speed")
    public void subClash(String name, MultipartFile resultCsv) throws IOException {
        if (ObjectUtil.hasEmpty(name, resultCsv)) {
            log.error("缺少参数,name:{},resultCsv:{}", name, resultCsv);
            return;
        }
        CdnConfig cdnConfig = cdnConfigMapper.selectByName(name);
        if (cdnConfig == null) {
            log.error("cdnConfig配置不存在,name:{}", name);
            return;
        }
        List<String> insertList = new ArrayList<>();
        List<String> updateList = new ArrayList<>();
        Set<String> ipList = new HashSet<>();
        try (InputStream inputStream = resultCsv.getInputStream()) {
            String data = IoUtil.readUtf8(inputStream);
            Arrays.stream(data.replaceAll("\r", "").split("\n")).map(line -> {
                        String[] split = line.split(",");
                        if (split.length != 6) {
                            return null;
                        }
                        String ip = split[0];
                        String speedStr = split[5];
                        if (!Validator.isIpv4(ip) && !Validator.isIpv6(ip) && !NumberUtil.isNumber(speedStr)) {
                            return null;
                        }
                        if (!ipList.add(ip)) {
                            log.error("ip:{}请求重复,跳过", ip);
                            return null;
                        }
                        BigDecimal speed = new BigDecimal(speedStr);
                        if (speed.compareTo(cdnConfig.getGoodIpSpeedLimit()) < 0) {
                            log.info("ip:{},speed:{},小于配置的最小:{},跳过", ip, speed, cdnConfig.getGoodIpSpeedLimit());
                            return null;
                        }
                        CdnSpeed cdnSpeed = new CdnSpeed();
                        CdnSpeed dbCdnSpeed = cdnSpeedMapper.selectByIp(ip);
                        cdnSpeed.setSpeed(speed);
                        cdnSpeed.setIp(ip);
                        if (dbCdnSpeed != null) {
                            // 修改
                            cdnSpeed.setId(dbCdnSpeed.getId());
                            cdnSpeed.setCount(dbCdnSpeed.getCount() + 1);
                        } else {
                            // 新增
                            cdnSpeed.setCdnName(name);
                        }
                        return cdnSpeed;
                    }).filter(Objects::nonNull)
                    .limit(cdnConfig.getGoodIpSaveSize()).forEach(cdnSpeed -> {
                                if (cdnSpeed.getId() == null) {
                                    insertList.add(cdnSpeed.getIp());
                                    cdnSpeedMapper.insert(cdnSpeed);
                                } else {
                                    updateList.add(cdnSpeed.getIp());
                                    cdnSpeedMapper.update(cdnSpeed);
                                }
                            }
                    );
            log.info("刷新cdn速度,入参:{}", data);
            log.info("刷新cdn速度,结束,name:{},新增{}条:{},修改{}条:{}", name, insertList.size(), insertList, updateList.size(), updateList);
        }
    }
}
