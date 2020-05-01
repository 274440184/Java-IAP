package com.example.demo.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * @author: gaokun
 * @date: 2020/4/23 10:20 上午
 * @description: ApplePayUtils
 */
@Slf4j
@Component
public class ApplePayUtils {

    @Autowired
    ApplePayConfig applePayConfig;

    /**
     * 生成订阅的bean
     *
     * @return
     */
    public String appleSubSign() {

        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = System.currentTimeMillis() + "";
        String payload = payloadCreate(new String[]{
                applePayConfig.appBundleID,
                applePayConfig.keyIdentifier,
                applePayConfig.appleSubscribeProductId,
                applePayConfig.offerIdentifier,
                "userId" + "",
                nonce,
                timestamp
        });

        log.info("applePay待签名字符串{}", payload);
        String sign = jdkECDSA(payload, applePayConfig.keyIdentifierContent);
        log.info("applePay签名字符串{}", sign);


        return sign;
    }

    /**
     * 拼接字符串
     * @param strings
     * @return
     */
    public static String payloadCreate(String[] strings) {

        if (strings == null) {
            return "";
        }
        String stringBuilder = "";
        for (int i = 0; i < strings.length; i++) {
            stringBuilder = stringBuilder + strings[i];
            if (i < strings.length - 1) {
                stringBuilder = stringBuilder + "\u2063";
            }
        }
        return stringBuilder;
    }


    public static String jdkECDSA(String str, String keyIdentifier) {

        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyIdentifier));
        try {
            // 2.执行签名
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(str.getBytes("utf-8"));

            byte[] sign = signature.sign();


//      根据苹果文档可以验证解密。该注释内容为解密验证签名公钥私钥
//            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEX7+pZFQO7gWH4NCGdVfkrgez5MGJLfo6/XtQjGA1Bno/lbZ3LnT6WlCmgthqVagIOkvYkWQx0L9Ujoir6IVSnA=="));
//            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
//            signature = Signature.getInstance("SHA256withECDSA");
//            signature.initVerify(publicKey);
//            signature.update(str.getBytes());
//            boolean bool = signature.verify(sign);
//            System.out.println(bool);

//            return new String(getBASE64(sign).getBytes("utf-8"));
            return getBASE64(sign);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * 用BASE64加密
     *
     * @param b
     * @return
     */

    public static String getBASE64(byte[] b) {

        return new sun.misc.BASE64Encoder().encode(b);
    }
}
