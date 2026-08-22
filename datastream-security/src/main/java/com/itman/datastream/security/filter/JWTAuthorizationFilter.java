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
package com.itman.datastream.security.filter;

import com.itman.datastream.security.constant.SecurityConstant;
import com.itman.datastream.security.jwt.DsJwtToken;
import com.itman.datastream.security.utils.DsResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
    private DsJwtToken dsJwtToken;

    public JWTAuthorizationFilter(AuthenticationManager authenticationManager, DsJwtToken dsJwtToken) {
        super(authenticationManager);
        this.dsJwtToken = dsJwtToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String tokenHeader = request.getHeader(SecurityConstant.TOKEN_HEADER);
        log.info("JWTAuthorizationFilter.doFilterInternal.token =====> {}", tokenHeader);

        // 如果登陆服务，且请求头中没有Authorization信息则直接放行了
        if (tokenHeader == null || !tokenHeader.startsWith(SecurityConstant.TOKEN_PREFIX)) {
                         String uri = request.getRequestURI();
             // 放行登录、H2控制台等无需认证的路径
             if (!StringUtils.isEmpty(uri) && 
                 (uri.indexOf("login") > -1 || 
                  uri.startsWith("/datastream/h2") ||
                  uri.startsWith("/h2") ||
                  uri.startsWith("/h2-console"))) {
                chain.doFilter(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                DsResponseUtils.write("-1", "该用户没有登陆！", response);
            }
            return;
        }
        // 如果请求头中有token，则进行解析，并且设置认证信息
        try {
            SecurityContextHolder.getContext().setAuthentication(getAuthentication(tokenHeader));
        } catch (Exception e) {
            log.error("JWTAuthorizationFilter.doFilterInternal.error: ", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            DsResponseUtils.write("-1", e.getMessage(), response);
            return;
        }
        super.doFilterInternal(request, response, chain);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(String tokenHeader) throws Exception {
        String token = tokenHeader.replace(SecurityConstant.TOKEN_PREFIX, "");
        if (dsJwtToken.isExpiration(token)) {
            throw new Exception("登陆超时了!");
        } else {
            String username = dsJwtToken.getUsername(token);
            if (!StringUtils.isEmpty(username)) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (String authority : dsJwtToken.getUserAuthorities(token)) {
                    if (!StringUtils.isEmpty(authority)) {
                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                }
                return new UsernamePasswordAuthenticationToken(username, null, authorities);
            }
        }
        return null;
    }
}
