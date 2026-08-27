package com.agentplatform.backend.agent.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 敏感信息过滤器。
 *
 * <p>输入在进入模型前先脱敏，输出在返回前再次过滤，降低手机号、
 * 邮箱、身份证号和配置密钥被模型链路传播的风险。</p>
 */
@Component
public class SensitiveContentFilter {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");
    private static final Pattern SECRET_PATTERN =
            Pattern.compile("(?i)(api[_-]?key|secret|password)\\s*[:=]\\s*[^\\s,;]+");

    private final String configuredTerms;

    public SensitiveContentFilter(
            @Value("${app.security.sensitive-terms:身份证,银行卡,密码,secret,api_key}")
            String configuredTerms
    ) {
        this.configuredTerms = configuredTerms == null ? "" : configuredTerms;
    }

    public String filter(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String filtered = PHONE_PATTERN.matcher(content).replaceAll("[手机号已脱敏]");
        filtered = EMAIL_PATTERN.matcher(filtered).replaceAll("[邮箱已脱敏]");
        filtered = ID_CARD_PATTERN.matcher(filtered).replaceAll("[证件号已脱敏]");
        filtered = SECRET_PATTERN.matcher(filtered).replaceAll("$1=[敏感配置已脱敏]");

        for (String term : configuredTerms.split(",")) {
            String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);
            if (!normalizedTerm.isBlank()) {
                filtered = filtered.replaceAll(
                        "(?i)" + Pattern.quote(normalizedTerm),
                        "[敏感词已过滤]"
                );
            }
        }
        return filtered;
    }
}
