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
package com.itman.datastream.engine.route;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class RouteAspect {
    public RouteAspect() {
    }

    private final String POINT_CUT = "execution(* com.itman.datastream.admin.service.*.*(..))";

    @Pointcut(POINT_CUT)
    public void pointCut() {
    }

    @Before(value = "pointCut()")
    public void doBefore(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        String method = joinPoint.getSignature().getName();
        Class<?>[] classZ = target.getClass().getInterfaces();
        Class<?>[] parameterTypes = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterTypes();
        Object[] parameterValue = joinPoint.getArgs();

        try {
            if (classZ != null && classZ.length > 0) {
                Method m = classZ[0].getMethod(method, parameterTypes);
                if (m != null && m.isAnnotationPresent(RouteSource.class)) {
                    RouteSource data = m.getAnnotation(RouteSource.class);

                    String routeKey = data.value();

                    if (parameterValue.length > 0) {
                        routeKey = routeKey + "_" + parameterValue[0];
                    }
                    RouteHolder.setRouteKey(routeKey);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After(value = "pointCut()")
    public void doAfter(JoinPoint joinPoint) {
        RouteHolder.removeRouteKey();
    }

    @AfterReturning(value = "pointCut()", returning = "result")
    public void doAfter(JoinPoint joinPoint, Object result) {
    }

    @AfterThrowing(value = "pointCut()", throwing = "exception")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable exception) {
    }

    @Around(value = "pointCut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Object o = pjp.proceed(args);
        return o;
    }
}
