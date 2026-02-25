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
import com.itman.datastream.security.domain.SystemUser;
import com.itman.datastream.security.jwt.DsJwtToken;
import com.itman.datastream.security.jwt.DsJwtUser;
import com.itman.datastream.security.handler.UserDetailsServiceImpl;
import com.itman.datastream.security.utils.DsResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class JWTAuthLoginFilter extends UsernamePasswordAuthenticationFilter {
    private AuthenticationManager authenticationManager;
    private UserDetailsServiceImpl userDetailsService;
    private DsJwtToken dsJwtTokenUtils;

    public JWTAuthLoginFilter(AuthenticationManager authenticationManager, UserDetailsServiceImpl userDetailsService,
                              DsJwtToken dsJwtTokenUtils, String loginUrl) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.dsJwtTokenUtils = dsJwtTokenUtils;
        super.setFilterProcessesUrl(loginUrl);
    }

    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        // 如果不是登录请求或不是POST请求，直接返回null
                 String uri = request.getRequestURI();
         String method = request.getMethod();
         if (uri.startsWith("/datastream/h2") || uri.startsWith("/h2") || uri.startsWith("/h2-console") ||
             (uri.contains("login") && !"POST".equalsIgnoreCase(method))) {
            return null;
        }
        
        SystemUser loginUser = null;
        try {
            String ssoToken = request.getParameter(SecurityConstant.TOKEN_PARAMS_NAME);
            if (!StringUtils.isEmpty(ssoToken)) {
                log.debug("token[{}]登陆中...", ssoToken);
                userDetailsService.setSsoToken(ssoToken);
                return authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(ssoToken, SecurityConstant.ANONYMOUS_USERNAME, new ArrayList<>()));
            } else {
                loginUser = new ObjectMapper().readValue(request.getInputStream(), SystemUser.class);
                log.debug("用户[{}]登陆中...", loginUser.getSystemUserCode());
                userDetailsService.setLoginUser(loginUser);
                return authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginUser.getSystemUserCode(), loginUser.getPassword(), new ArrayList<>()));
            }
            //密码错误时抛出异常
        } catch (BadCredentialsException b) {
            if (loginUser != null) {
                log.error("用户[{}]输入密码错误。。。", loginUser.getSystemUserCode(), b);
            }

            try {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                DsResponseUtils.write("-1", "用户名或密码错误", response);
            } catch (IOException e) {
                log.error("attemptAuthentication.writer.response.error: ", e);
            }
        } catch (InternalAuthenticationServiceException i) {
            if (loginUser != null) {
                log.error("没有此用户[{}]", loginUser.getSystemUserCode(), i);
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            DsResponseUtils.write("-1", "用户名或密码错误", response);
        } catch (Exception e) {
            log.error("attemptAuthentication.error: ", e);
        }
        return null;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {
        DsJwtUser dsJwtUser = (DsJwtUser) authResult.getPrincipal();
        log.info("用户[{}]登陆成功！", dsJwtUser.getSystemUserInfo().getSystemUserCode());
        String role = "";
        Collection<? extends GrantedAuthority> authorities = dsJwtUser.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            role = authority.getAuthority();
        }
        String token = dsJwtTokenUtils.createToken(dsJwtUser.getSystemUserInfo().getSystemUserCode(), role);
        // 返回创建成功的token
        response.setHeader("token", SecurityConstant.TOKEN_PREFIX + token);
        // 将该用户的id进行返回了
        response.setIntHeader("id", dsJwtUser.getId().intValue());
        // 清空登录信息
        dsJwtUser.getSystemUserInfo().setUsername(null);
        dsJwtUser.getSystemUserInfo().setPassword(null);
        DsResponseUtils.write("0", "登陆成功！", dsJwtUser.getSystemUserInfo(), response);
    }

}
