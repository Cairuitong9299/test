package com.example.test.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.bdp.idmapping.common.Constant;
import com.bdp.idmapping.core.IdCodeEnum;
import com.bdp.idmapping.response.Response;
import com.bdp.idmapping.service.IdMappingService;
import com.bdp.idmapping.utils.DESTools;
import com.bdp.idmapping.utils.UniqueSsoidImeiDataUtil;
import com.example.test.config.ApplicationConfig;
import com.example.test.exception.ServiceException;
import com.example.test.utils.ExecutorServiceUtils;
import com.example.test.utils.TypeDomain;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

/**
 * @Auther: CAI
 * @Date: 2022/11/2 - 11 - 02 - 20:43
 * @Description: com.example.test.controller
 * @version: 1.0
 */
@RequestMapping("/hid/relation")
@RestController
public class HidController {

    private static final Logger logger = LoggerFactory.getLogger(HidController.class);

    @Autowired
    private IdMappingService idMappingService;

    @Autowired
    private ApplicationConfig applicationConfig;

    @RequestMapping(value = "/mapping/{sourceType}/{targetType}", method = RequestMethod.POST)
    public Response<String> mapping(@PathVariable("sourceType") String sourceType,
                                    @PathVariable("targetType") String targetType,
                                    @RequestParam("bizName") String bizName,
                                    @RequestParam("ts") String ts,
                                    @RequestParam("sign") String sign,
                                    @RequestBody String content) {
        try {
            //判断是否有数据为空 如果有则返回错误
            if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType) || StringUtils.isBlank(bizName)
                    || StringUtils.isBlank(ts) || StringUtils.isBlank(sign) || StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
            //判断请求是否超时 是则返回错误
            if (isTimeOut(Long.valueOf(ts))) {
                return Response.fail(Constant.CODE_TIME_ERROR, Constant.CODE_TIME_ERROR_MSG);
            }


            TypeDomain typeDomain = new TypeDomain(sourceType, targetType);
            //判断sourceType, targetType是否为有效的参数
            validateType(typeDomain);
            //如果类型无效则，返回错误
            if (!typeDomain.isValid()) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_TYPE_PARAM_ERROR_MSG);
            }
            //是否为多个ID
            boolean isMultipledId = typeDomain.isMultipledId();
            //判断签名是非一致，不同则返回错误
            if (!sign.equals(DigestUtils.md5Hex(ts + bizName))) {
                return Response.fail(Constant.CODE_SIGN_ERROR, Constant.CODE_SIGN_ERROR_MSG);
            }
            //解密
            content = decodeDesStr(bizName + ts, content);
            //判断解密的数据是否为空，是则返回错误
            if (StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
            //取到数据
            Set<String> targetValue = mappingWithTrace(sourceType, content, targetType, bizName, isMultipledId);
            if (applicationConfig.isOpenLog() && targetValue == null) {
                logger.info("bizName:{} sourceKV:{}:{} targetKV:{}:{}", bizName, sourceType, content, targetType, targetValue);
            }
            Response<String> response;
            if (targetValue != null && !targetValue.isEmpty()) {
                response = Response.success(JSON.toJSONString(targetValue), Constant.CODE_SUCCESS, Constant.CODE_SUCCESS_MSG);
            } else {
                response = Response.fail(Constant.CODE_NOT_EXIST, Constant.CODE_NOT_EXIST_MSG);
            }
            return response;
        } catch (Exception e) {
            logger.error("mapping", e);
            return Response.fail(Constant.CODE_SERVER_ERROR, Constant.CODE_SERVER_ERROR_MSG);
        }
    }

    //批量
    public Response<Set<Pair<String, Set<String>>>> batchMappingPair(@PathVariable("sourceType") String sourceType,
                                                                     @PathVariable("targetType") String targetType,
                                                                     @RequestParam("bizName") String bizName,
                                                                     @RequestParam("ts") String ts,
                                                                     @RequestParam("sign") String sign,
                                                                     @RequestBody String content) {
        try {
            //判断非空
            if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType) || StringUtils.isBlank(bizName)
                    || StringUtils.isBlank(ts) || StringUtils.isBlank(sign) || StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
            //判断超时
            if (isTimeOut(Long.valueOf(ts))) {
                return Response.fail(Constant.CODE_TIME_ERROR, Constant.CODE_TIME_ERROR_MSG);
            }
            //判断类型
            TypeDomain typeDomain = new TypeDomain(sourceType, targetType);
            validateType(typeDomain);
            if (!typeDomain.isValid()) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_TYPE_PARAM_ERROR_MSG);
            }
            //判断多重
            boolean isMultipledId = typeDomain.isMultipledId();
            //签名
            if (!sign.equals(DigestUtils.md5Hex(ts + bizName))) {
                return Response.fail(Constant.CODE_SIGN_ERROR, Constant.CODE_SIGN_ERROR_MSG);
            }
            //解密
            content = decodeDesStr(bizName + ts, content);
            if (StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
            //
            Set<Pair<String, Set<String>>> targetValue = batchMappingPariWithTrace(sourceType, content, targetType, bizName, isMultipledId);
            if (applicationConfig.isOpenLog() && targetValue == null) {
                logger.info("bizName:{} sourceKV:{}:{} targetKV:{}:{}", bizName, sourceType, content, targetType, targetValue);
            }
            Response<Set<Pair<String, Set<String>>>> response;
            if (targetValue != null && !targetValue.isEmpty()) {
                response = Response.success(targetValue, Constant.CODE_SUCCESS, Constant.CODE_SUCCESS_MSG);
            } else {
                response = Response.fail(Constant.CODE_NOT_EXIST, Constant.CODE_NOT_EXIST_MSG);
            }
            return response;
        } catch (Exception e) {
            logger.error("batchMappingPair", e);
            return Response.fail(Constant.CODE_SERVER_ERROR, Constant.CODE_SERVER_ERROR_MSG);
        }
    }

    private void validateType(TypeDomain typeDomain) {
        String sourceType = typeDomain.getSourceType();
        String targetType = typeDomain.getTargetType();
        //判断sourceType targetType 是否为空 如果为空则把typeDomain设置为无效的
        if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType)) {
            typeDomain.setValid(false);
            return;
        }
        //如果二个数值是错误的则也设置为无效的
        if (!validateType(sourceType, targetType)) {
            typeDomain.setValid(false);
            return;
        }
        //都不是上面的情况则设置为生效
        typeDomain.setValid(true);
    }

    //验证sourceType targetType 是否同时为枚举类中的类型
    private boolean validateType(String sourceType, String targetType) {
        return IdCodeEnum.isValidCode(sourceType) && IdCodeEnum.isValidCode(targetType);
    }

    private static String decodeDesStr(String key, String value) {
        String result = null;
        try {
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                return null;
            }
            result = DESTools.decryptStr(key, value, null);
        } catch (Exception e) {
            logger.error("解密数据出错. key:{},value:{}", key, value, e);
        }
        return result;
    }

    public Set<String> mappingWithTrace(String sourceType, String content, String targetType, String bizName, boolean isMultipleId) {
        return doMapping(sourceType, content, targetType, bizName, isMultipleId);
    }

    //多重获取
    private Set<Pair<String, Set<String>>> batchMappingPariWithTrace(String sourceType, String content, String targetType, String bizName, boolean isMultipleId) {
        try {
            Set<Pair<String, Set<String>>> targetValues = new HashSet<>();
            Set<String> exists = new HashSet<>();
            //将content转换为JSON
            JSONArray jsonArray = JSONArray.parseArray(content);
            int size = jsonArray.size();
            //判断是否超出批量处理的最大值 是则抛出错误
            if (size > applicationConfig.getBatchSize()) {
                throw new ServiceException(String.format("%s|%s|%s|", Constant.CODE_BATCH_SIZE_INVALID_MSG, bizName, size));
            }

            CompletionService<Pair<String, Set<String>>> completionService = new ExecutorCompletionService<>(ExecutorServiceUtils.INSTANCE.getExecutorService());
            //迭代器
            Iterator iterator = jsonArray.iterator();

            while (iterator.hasNext()) {
                //取代值
                String sourceValue = iterator.next().toString();
                //判断是否已经包含了该值
                if (!exists.contains(sourceValue)) {
                    //添加到exists集合中去
                    exists.add(sourceValue);
                    //
                    completionService.submit(() -> {
                        Set<String> stringSet = doMapping(sourceType, sourceValue, targetType, bizName, isMultipleId);
                        if (stringSet != null && stringSet.size() != 0) {
                            return Pair.of(sourceValue, stringSet);
                        }
                        return null;
                    });
                }
            }
            for (int i = 0; i < exists.size(); i++) {
                try {
                    Future<Pair<String, Set<String>>> future = completionService.take();
                    Pair<String, Set<String>> stringSetPair = future.get();
                    if (stringSetPair != null) {
                        targetValues.add(stringSetPair);
                    }
                } catch (Exception e) {
                    logger.error("find effective targets error", e);
                }
            }
            return targetValues;
        } catch (Exception e) {
            logger.error("mappingWithTrace", e);
            throw e;
        }
    }

    private Set<String> doMapping(String sourceType, String sourceValue, String targetType, String bizName, boolean isMultipleId) {
        try {
            Set<String> res = new HashSet<>();
            //判断sourceType targetType 的类型是否是目标类型 是则进行查询并返回
            if (sourceType.equals(IdCodeEnum.IMEI.getCode()) && targetType.equals(IdCodeEnum.UNIQUE_SSOID.getCode())) {
                return UniqueSsoidImeiDataUtil.getImeiToSsoidRelation(res, sourceValue);
            }
            //查询数据并把数据放到res中并返回
            res = idMappingService.convertId(sourceType, sourceValue, targetType, bizName, isMultipleId);
            return res;
        } catch (Exception e) {
            logger.error("doMapping", e);
            throw e;
        }
    }

    private boolean isTimeOut(long ts) {
        if (System.currentTimeMillis() + 60 * 1000 < ts || System.currentTimeMillis() - 60 * 1000 > ts) {
            return true;
        }
        return false;
    }
}
