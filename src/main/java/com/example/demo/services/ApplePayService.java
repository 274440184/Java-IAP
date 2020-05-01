package com.example.demo.services;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;


/**
 * @author: gaokun
 * @date: 2020/3/20 2:20 下午
 * @description: 具体实现！！！
 */
@Slf4j
@Service
public class ApplePayService {


    @Autowired
    ApplePayVerification.ApplePayAPI applePayAPI;
    @Autowired
    ApplePayVerification.AppleSendBoxPayAPI appleSendBoxPayAPI;


    /**
     * Apple Pay连续订阅事件的回调  具体内容参考文档
     * https://developer.apple.com/documentation/appstoreservernotifications/responsebody
     *
     * @return
     */
    public String continuousSubscription(JSONObject jsonObject) {

        //监听用户的取消续订
        if ("CANCEL".equals(jsonObject.getString("notification_type"))) {
            JSONObject latest_receipt_info = jsonObject.getJSONObject("latest_receipt_info");
            String original_transaction_id = latest_receipt_info.getString("original_transaction_id");
            String product_id = latest_receipt_info.getString("product_id");
        }

        return "success";
    }

    /**
     * 苹果支付验证订单  根据链接文档字段处理
     *https://developer.apple.com/documentation/appstorereceipts
     * @param receipt
     * @return
     */
    public String applePayOrder(long userId, String receipt, long orderId) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("receipt-data", receipt);
        jsonObject.put("password", "xxxx");
        String verifyResult = null;
        try {//注意 苹果这个是国外的，可能会慢一点，最好做超时处理
            verifyResult = applePayAPI.verifyReceipt(jsonObject.toJSONString());
        } catch (Exception e) {
            return "请求超时请重试!";
        }

        log.info("验证签名参数：{}", verifyResult);

        // 苹果验证有返回结果------------------
        JSONObject job = JSONObject.parseObject(verifyResult);
        String states = job.getString("status");

        // TODO 这边有个问题就是应用审核的时候，
        //  apple那边走的是沙盒账号，如果付款失败可能被拒绝，同时我们后台也要处理下毕竟人家是大爷！

        //测试环境的话再次去测试环境验证签名
        if ("21007".equals(states)) {
            verifyResult = appleSendBoxPayAPI.verifyReceipt(jsonObject.toJSONString());
        }
        //生产环境的话再次去生产环境验证签名
        if ("21008".equals(states)) {
            verifyResult = applePayAPI.verifyReceipt(jsonObject.toJSONString());

        }

        job = JSONObject.parseObject(verifyResult);
        states = job.getString("status");

        // 验证成功
        if (!"0".equals(states)) {
            return "验证签名失败!";
        }



        // TODO 目前我这边的处理逻辑是先看有没有订单
        //查找订单
        JSONObject orderDTO = new JSONObject();


        //如果有订单，如果连续订阅第二个月是没有订单的需要后台自己处理
        if (orderDTO != null) {
            //订单验证过
            if (orderDTO.getInteger("state") == 1) {
                return "该订单已成功验证!" ;
            }
            //普通续费
            if ("普通续费".equals(orderDTO.getString("type"))) {

                //获取创建的订单对应的product_id
                String product_id = orderDTO.getString("product_id");
                JSONObject r_receipt = job.getJSONObject("receipt");
                // 订单号
                String transaction_id = null;
                if (r_receipt == null) {
                    return "receipt中缺少数据!";
                }
                //读取数据,用于轮询获取订单信息
                JSONArray jsonArray = r_receipt.getJSONArray("in_app");
                if (jsonArray == null || jsonArray.size() == 0) {
                    return "in_app缺少数据!";
                }
                for (Object o : jsonArray) {
                    JSONObject transactionObject = (JSONObject) o;

                    if (!product_id.equals(transactionObject.getString("product_id"))) {
                        //如果product_id不一样则跳过
                        continue;
                    }
                    if ("验证订单id流水在自己的系统订单中是否存在".equals("transactionObject.getString(\"transaction_id\")")) {
                        continue;
                    }
                    transaction_id = transactionObject.getString("transaction_id");

                }
                if (StrUtil.isEmpty(transaction_id)) {
                    return "该签名验证失败！";
                }

                {
                    //说明存在一笔流水号transaction_id的订单，这个时候插入自己的数据库中
                }

            } else {//自动续费

                //获取创建的订单对应的product_id
                String product_id = orderDTO.getString("product_id");
                JSONArray array = job.getJSONArray("latest_receipt_info");
                JSONObject latest_receipt_info = null;
                log.info("自动续费的latest_receipt_info{}", array.toJSONString());

                //自动订阅数据处理
                solvePendingRenewalInfo(job.getJSONArray("pending_renewal_info"), receipt, userId);
                if (array != null && array.size() > 0) {
                    //找到对应的
                    for (Object o : array) {
                        JSONObject transactionObject = (JSONObject) o;
                        //查找符合条件的订阅信息
                        if (!product_id.equals(transactionObject.getString("product_id"))) {
                            //如果product_id不一样则跳过
                            continue;
                        }
                        if ("验证订单id流水在自己的系统订单中是否存在".equals("transactionObject.getString(\"transaction_id\")")) {
                            continue;
                        }
                        latest_receipt_info = transactionObject;
                        break;
                    }

                    if (latest_receipt_info == null) {
                        return "验证签名数据有误";
                    }
                    String transaction_id = latest_receipt_info.getString("transaction_id");
                    //是否免费
                    String is_in_intro_offer_period = latest_receipt_info.getString("is_in_intro_offer_period");
                    //是否享受优惠
                    String is_trial_period = latest_receipt_info.getString("is_trial_period");

                    {
                        //说明存在一笔流水号transaction_id的订单，这个时候插入自己的数据库中
                    }


                }
            }
        } else {//如果没带订单id说明是连续订阅验证参数，这个时候需要把历史订单中没处理的都统一处理下

            JSONArray array = job.getJSONArray("latest_receipt_info");

            if (array == null || array.size() == 0) {
                return "签名中没有数据";
            }
            for (Object o : array) {
                JSONObject transactionObject = (JSONObject) o;
                //查找符合条件的订阅产品的信息

                String is_in_intro_offer_period = transactionObject.getString("is_in_intro_offer_period");
                String is_trial_period = transactionObject.getString("is_trial_period");


                if ("验证订单id流水在自己的系统订单中是否存在".equals("transactionObject.getString(\"transaction_id\")")) {
                    createAppleOrder( transactionObject.getString("transaction_id"));
                }
            }
            //自动订阅数据处理
            solvePendingRenewalInfo(job.getJSONArray("pending_renewal_info"), receipt, userId);
        }

        return "success";
    }

    /**
     * 连续订阅信息记录 如果后台有需求可以记录下当前用户的连续订阅状态和订阅的产品。
     * 我存了最后的一个receipt签名，然后定时查询获取最新的订阅状态
     *
     * @param pending_renewal_info
     * @param userId
     */
    public void solvePendingRenewalInfo(JSONArray pending_renewal_info,  String receipt, long userId) {

        if (pending_renewal_info != null) {

            for (Object o : pending_renewal_info) {
                JSONObject jsonObject = (JSONObject) o;
                String product_id = jsonObject.getString("product_id");
                String original_transaction_id = jsonObject.getString("original_transaction_id");
                String auto_renew_status = jsonObject.getString("auto_renew_status");
                //存储最新的验证签名同步数据用


            }
        }
    }

    /**
     * 苹果支付验证签名后生成订单
     *
     */
    @Transactional
    public void createAppleOrder( String transactionId) {
        //项目中创建订单的操作
    }


}
