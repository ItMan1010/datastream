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
package com.itman.datastream.security.jwt;

import com.itman.datastream.security.constant.SecurityConstant;
import com.itman.datastream.engine.systemlog.ISystemLogService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.itman.datastream.security.constant.SecurityConstant.EXPIRATION;

@Component
@Slf4j
public class DsJwtToken {
    @Resource
    ISystemLogService ISystemLogService;

    public String createToken(String username, String authorities) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(SecurityConstant.ROLE_CLAIMS, authorities);
        String token = Jwts.builder().signWith(SignatureAlgorithm.HS512, SecurityConstant.SECRET)
                .setClaims(map)
                .setIssuer(SecurityConstant.ISS)
                .setSubject(username)
                .setIssuedAt(new Date())
                .compact();
        ISystemLogService.appendSystemSession(token, username, 1, EXPIRATION);
        return token;
    }

    public void deleteToken(String token) {
        ISystemLogService.refreshSystemSessionState(token, 2);
    }

    public static String getUsername(String token) {
        return getTokenBody(token).getSubject();
    }

    public static List<String> getUserAuthorities(String token) {
        Object authorities = getTokenBody(token).get(SecurityConstant.ROLE_CLAIMS);
        if (authorities == null) {
            return new ArrayList<>();
        }
        String authorityStr = String.valueOf(authorities);
        if (authorityStr == null || authorityStr.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(authorityStr.split(",")));
    }

    public boolean isExpiration(String token) {
        try {
            return ISystemLogService.isTokenExpiration(token, EXPIRATION);
        } catch (Exception e) {
            log.error("JwtTokenUtils.isExpiration.error: ", e);
            return true;
        }
    }

    private static Claims getTokenBody(String token) {
        return Jwts.parser()
                .setSigningKey(SecurityConstant.SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
