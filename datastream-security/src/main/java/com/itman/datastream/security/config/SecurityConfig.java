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
package com.itman.datastream.security.config;

import com.itman.datastream.security.jwt.DsJwtToken;
import com.itman.datastream.security.filter.JWTAuthLoginFilter;
import com.itman.datastream.security.filter.JWTAuthorizationFilter;
import com.itman.datastream.security.handler.LogoutSuccessHandlerImpl;
import com.itman.datastream.security.handler.UserDetailsServiceImpl;
import com.itman.datastream.security.utils.DsResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Value("${auth.sso.loginPath:}")
    private String loginPath;
    @Value("${auth.sso.logoutPath:}")
    private String logoutPath;

    @Value("${auth.sso.whiteList:}")
    private String[] whiteList;
    @Value("${auth.dataStreamPermit: false}")
    private boolean dataStreamPermit;
    @Resource
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Resource
    private DsJwtToken dsJwtToken;
    @Resource
    private LogoutSuccessHandlerImpl logoutSuccessHandlerImpl;

    private static class AjaxAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
            log.info("用户[{}]登陆成功!!！", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            DsResponseUtils.write("0", "登录成功！", response);
        }
    }

    private static class AjaxAuthFailHandler extends SimpleUrlAuthenticationFailureHandler {
        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
            log.info("用户[{}]登陆失败!!！", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            DsResponseUtils.write("-1", "请检查用户名或密码是否正确！", response);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsServiceImpl).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        if (this.dataStreamPermit) {
            http.csrf().disable().authorizeRequests().antMatchers("/**/**").permitAll().anyRequest().authenticated();
            return;
        }

        http.csrf()
                // 关闭csrf
                .disable()
                .authorizeRequests()
                // 明确放行 H2 控制台路径
                .antMatchers("/datastream/h2/**", "/h2/**").permitAll()
                .antMatchers(whiteList).permitAll()
                .anyRequest().authenticated()
                .and()
                // 允许 H2 控制台使用 iframe
                .headers().frameOptions().sameOrigin()
                .and()
                .addFilter(new JWTAuthLoginFilter(authenticationManager(), userDetailsServiceImpl, dsJwtToken, loginPath))
                .addFilter(new JWTAuthorizationFilter(authenticationManager(), dsJwtToken))
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 禁用session

        //登录配置
        http.formLogin()
                .successHandler(new AjaxAuthSuccessHandler())
                .failureHandler(new AjaxAuthFailHandler())
                .loginPage(loginPath)//设置自己的登录界面（如果不设置，将会是自带的登录界面这里我们使用自定义的登录界面）
//                .loginProcessingUrl(AuthConstant.LOGIN_URL)//表单提交的url
                .and()
                .logout()
                .logoutUrl(logoutPath)
                .logoutSuccessHandler(logoutSuccessHandlerImpl);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        return source;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/static/**", "/");
    }
}
