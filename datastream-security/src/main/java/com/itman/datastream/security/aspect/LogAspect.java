/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itman.datastream.security.aspect;

import com.itman.datastream.security.annotation.LogOperate;
import com.itman.datastream.security.domain.SystemLog;
import com.itman.datastream.engine.event.SystemLogEvent;
import com.itman.datastream.security.utils.DsServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Slf4j
@Aspect
@Component
public class LogAspect {
    private final static ExpressionParser expressionParser = new SpelExpressionParser();
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private String getParsedDescription(String desc, EvaluationContext ctx) {
        try {
            return expressionParser.parseExpression(desc).getValue(ctx).toString();
        } catch (Exception e) {
            return desc;
        }
    }

    private EvaluationContext getEvaluationContext(ProceedingJoinPoint joinPoint) {
        // 将方法的参数名和参数值一一对应的放入上下文中
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        List<String> paramNameList = Arrays.asList(methodSignature.getParameterNames());
        List<Object> paramList = Arrays.asList(joinPoint.getArgs());
        EvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < paramNameList.size(); i++) {
            ctx.setVariable(paramNameList.get(i), paramList.get(i));
        }

        return ctx;
    }

    @Pointcut("@annotation(com.itman.datastream.security.annotation.LogOperate)")
    public void operatorPointCut() {
    }

    @Around("operatorPointCut()")
    public Object aroundOperator(ProceedingJoinPoint joinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        long finishTime = 0L;
        Object returnObject = null;
        Throwable throwable = null;
        try {
            returnObject = joinPoint.proceed();
        } catch (Throwable e) {
            throwable = e;
        } finally {
            finishTime = System.currentTimeMillis();
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        if (method.isAnnotationPresent(LogOperate.class)) {
            EvaluationContext ctx = this.getEvaluationContext(joinPoint);
            this.recordOperateLog(methodSignature, ctx, throwable, finishTime - beginTime);
        }

        if (null != throwable) {
            throw throwable;
        }

        return returnObject;
    }

    private void recordOperateLog(MethodSignature methodSignature, EvaluationContext ctx,
                                  Throwable throwable, long elapse) {
        Method method = methodSignature.getMethod();
        LogOperate logAnnotation = method.getAnnotation(LogOperate.class);
        Integer operateType = logAnnotation.operateType();
        String username = "null";
        if (operateType.equals(1)) {
            username = getParsedDescription(logAnnotation.username(), ctx);
        } else if (operateType.equals(2) && !Objects.isNull(SecurityContextHolder.getContext().getAuthentication())) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        SystemLog systemLogEntity = SystemLog.builder()
                .type(operateType)
                .username(username)
                .ipAddress(DsServletUtils.getIpAddr())
                .moduleName(logAnnotation.moduleName())
                .content(getParsedDescription(logAnnotation.description(), ctx))
                .urlPath(DsServletUtils.getPathUri())
                .userAgent(DsServletUtils.getUserAgent())
                .build();
        applicationEventPublisher.publishEvent(new SystemLogEvent(systemLogEntity));
    }
}
