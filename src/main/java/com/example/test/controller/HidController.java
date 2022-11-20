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
            if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType) || StringUtils.isBlank(bizName)
                    || StringUtils.isBlank(ts) || StringUtils.isBlank(sign) || StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
            if (isTimeOut(Long.valueOf(ts))) {
                return Response.fail(Constant.CODE_TIME_ERROR, Constant.CODE_TIME_ERROR_MSG);
            }
            TypeDomain typeDomain = new TypeDomain(sourceType, targetType);
            validateType(typeDomain);
            if (!typeDomain.isValid()) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_TYPE_PARAM_ERROR_MSG);
            }
            boolean isMultipledId = typeDomain.isMultipledId();
            if (!sign.equals(DigestUtils.md5Hex(ts + bizName))) {
                return Response.fail(Constant.CODE_SIGN_ERROR, Constant.CODE_SIGN_ERROR_MSG);
            }
            content = decodeDesStr(bizName + ts, content);
            if (StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
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

    public Response<Set<Pair<String, Set<String>>>> batchMappingPair(@PathVariable("sourceType") String sourceType,
                                                                     @PathVariable("targetType") String targetType,
                                                                     @RequestParam("bizName") String bizName,
                                                                     @RequestParam("ts") String ts,
                                                                     @RequestParam("sign") String sign,
                                                                     @RequestBody String content) {
        try {
            if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType) || StringUtils.isBlank(bizName)
                    || StringUtils.isBlank(ts) || StringUtils.isBlank(sign) || StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
            if (isTimeOut(Long.valueOf(ts))) {
                return Response.fail(Constant.CODE_TIME_ERROR, Constant.CODE_TIME_ERROR_MSG);
            }
            TypeDomain typeDomain = new TypeDomain(sourceType, targetType);
            validateType(typeDomain);
            if (!typeDomain.isValid()) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_TYPE_PARAM_ERROR_MSG);
            }
            boolean isMultipledId = typeDomain.isMultipledId();
            if (!sign.equals(DigestUtils.md5Hex(ts + bizName))) {
                return Response.fail(Constant.CODE_SIGN_ERROR, Constant.CODE_SIGN_ERROR_MSG);
            }
            content = decodeDesStr(bizName + ts, content);
            if (StringUtils.isBlank(content)) {
                return Response.fail(Constant.CODE_PARAM_ERROR, Constant.CODE_PARAM_ERROR_MSG);
            }
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
        if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(targetType)) {
            typeDomain.setValid(false);
            return;
        }
        if (!validateType(sourceType, targetType)) {
            typeDomain.setValid(false);
            return;
        }
        typeDomain.setValid(true);
    }

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

    private Set<Pair<String, Set<String>>> batchMappingPariWithTrace(String sourceType, String content, String targetType, String bizName, boolean isMultipleId) {
        try {
            Set<Pair<String, Set<String>>> targetValues = new HashSet<>();
            Set<String> exists = new HashSet<>();
            JSONArray jsonArray = JSONArray.parseArray(content);
            int size = jsonArray.size();
            if (size > applicationConfig.getBatchSize()) {
                throw new ServiceException(String.format("%s|%s|%s|", Constant.CODE_BATCH_SIZE_INVALID_MSG, bizName, size));
            }
            CompletionService<Pair<String, Set<String>>> completionService = new ExecutorCompletionService<>(ExecutorServiceUtils.INSTANCE.getExecutorService());
            Iterator iterator = jsonArray.iterator();
            while (iterator.hasNext()) {
                String sourceValue = iterator.next().toString();
                if (!exists.contains(sourceValue)) {
                    exists.add(sourceValue);
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
            if (sourceType.equals(IdCodeEnum.IMEI.getCode()) && targetType.equals(IdCodeEnum.UNIQUE_SSOID.getCode())) {
                return UniqueSsoidImeiDataUtil.getImeiToSsoidRelation(res, sourceValue);
            }
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
