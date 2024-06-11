package clash.manage.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * mock数据接口
 */
@RestController
public class MockController {

    @GetMapping(value = "/api/client/subscribe/example")
    public ResponseEntity subClash(HttpServletRequest request, String name) {
        ExampleConfig config = exampleConfigMap.getOrDefault(name, exampleConfigMap.get("订阅A"));
        InputStream inputStream = ResourceUtil.getStream("example/" + config.getExampleYamlName());
        String context = IoUtil.readUtf8(inputStream);
        return new ResponseEntity<>(context, config.getHeader().toMultiValueMap(request.getHeader(HttpHeaders.USER_AGENT)), HttpStatus.OK);
    }

    private static final Map<String, ExampleConfig> exampleConfigMap = Stream.of(
            new ExampleConfig("订阅A", "subscribeExample1.yml", new ClashHeader(
                    1L << 27,
                    1L << 28,
                    1L << 30,
                    DateUtil.parseDate("2030-1-1").getTime() / 1000,
                    24,
                    "订阅A配置文件",
                    "https://www.baidu.com")),
            new ExampleConfig("订阅B", "subscribeExample2.yml", new ClashHeader(
                    1L << 28,
                    1L << 29,
                    1L << 31,
                    null,
                    48,
                    "订阅B配置文件",
                    "https://www.google.com"))
    ).collect(Collectors.toMap(ExampleConfig::getName, Function.identity()));

    @Data
    @AllArgsConstructor
    private static class ExampleConfig {

        private String name;

        private String exampleYamlName;

        private ClashHeader header;
    }

    @Data
    @AllArgsConstructor
    private static class ClashHeader {
        /**
         * 上传流量
         */
        private Long upload;
        /**
         * 下载流量
         */
        private Long download;
        /**
         * 总流量
         */
        private Long total;
        /**
         * 过期时间
         */
        private Long expire;
        /**
         * 刷新频率,单位小时
         */
        private Integer profileUpdateInterval;
        /**
         * 配置文件名称
         */
        private String contentDisposition;
        /**
         * 网站地址
         */
        private String profileWebPageUrl;

        private String format(Object data) {
            return data == null ? "" : data.toString();
        }

        public MultiValueMap<String, String> toMultiValueMap(String userAgent) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("subscription-userinfo", StrUtil.format("upload={}; download={}; total={}; expire={}",
                    format(upload), format(download), format(total), format(expire)));
            if (profileUpdateInterval != null) {
                headers.add("profile-update-interval", profileUpdateInterval.toString());
            }
            UserAgent ua = UserAgentUtil.parse(userAgent);
            if (ua.getBrowser().isUnknown() && contentDisposition != null) {
                headers.add("content-disposition", "attachment;filename*=UTF-8''" + URLUtil.encode(contentDisposition));
            }
            if (profileWebPageUrl != null) {
                headers.add("profile-web-page-url", profileWebPageUrl);
            }
            headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
            return headers;
        }
    }
}
