package com.example.demo.services;

import com.example.demo.utils.ApplePayConfig;
import feign.Client;
import feign.hystrix.HystrixFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author: gaokun
 * @date: 2020/3/23 2:46 下午
 * @description: 苹果支付校验
 */
@Configuration
public class ApplePayVerification {


    @Autowired
    ApplePayConfig applePayConfig;

    @Bean
    public ApplePayAPI init() {

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return HystrixFeign.builder()
                .client(new Client.Default(sc.getSocketFactory(), null))
                .contract(new SpringMvcContract())
                .target(ApplePayVerification.ApplePayAPI.class, applePayConfig.api);
    }

    @Bean
    public AppleSendBoxPayAPI initSendBox() {

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return HystrixFeign.builder()
                .client(new Client.Default(sc.getSocketFactory(), null))
                .contract(new SpringMvcContract())
                .target(ApplePayVerification.AppleSendBoxPayAPI.class, applePayConfig.sandboxApi);
    }


    private static class TrustAnyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * 正式环境
     */
    public interface ApplePayAPI {

        @PostMapping("/verifyReceipt")
        String verifyReceipt(@RequestBody String body);

    }

    /**
     * 沙盒环境
     */
    public interface AppleSendBoxPayAPI {

        @PostMapping("/verifyReceipt")
        String verifyReceipt(@RequestBody String body);

    }
}
