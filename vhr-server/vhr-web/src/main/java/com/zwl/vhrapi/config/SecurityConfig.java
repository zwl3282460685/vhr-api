package com.zwl.vhrapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwl.vhrapi.filter.JwtFilter;
import com.zwl.vhrapi.filter.JwtLoginFilter;
import com.zwl.vhrapi.model.Hr;
import com.zwl.vhrapi.model.RespBean;
import com.zwl.vhrapi.service.HrService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Resource
    HrService hrService;
    @Resource
    CustomerFilterInvocationSecurityMetadataSource myFilter;
    @Resource
    CustomerUrlDecisionManager myDecisionManager;
    @Resource
    VerificationCodeFilter verificationCodeFilter;
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(hrService);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/swagger-ui.html",
                "/v2/api-docs", // swagger api json
                "/swagger-resources/**",
                "/webjars/**",  //补充路径，近期在搭建swagger接口文档时，通过浏览器控制台发现该/webjars路径下的文件被拦截，故加上此过滤条件即可。
                "/login",
                "/css/**",
                "/js/**", "/index.html", "/img/**",
                "/fonts/**", "/favicon.ico",
                "/verifyCode");
    }

    @Bean
    SessionRegistryImpl sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.addFilterBefore(verificationCodeFilter, UsernamePasswordAuthenticationFilter.class);
        http.authorizeRequests()
//                .anyRequest().authenticated()
            .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() { //用于判断用户是否具有访问权限
                @Override
                public <O extends FilterSecurityInterceptor> O postProcess(O o) {
                    o.setAccessDecisionManager(myDecisionManager);
                    o.setSecurityMetadataSource(myFilter);
                    return o;
                }
            })
            .and()
            .formLogin()
            .usernameParameter("username")
            .passwordParameter("password")
            .loginProcessingUrl("/doLogin") //设置的登录的访问接口
            .loginPage("/login")
            .successHandler(new AuthenticationSuccessHandler() {//登录成功的操作
                @Override
                public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                    Authentication authentication) throws IOException, ServletException {
                    response.setContentType("application/json;charset=utf-8");
                    PrintWriter out = response.getWriter();
                    Hr hr = (Hr) authentication.getPrincipal(); //登录成功的hr对象
                    hr.setPassword(null);
                    RespBean ok = RespBean.ok("登录成功！", hr);
                    out.write(new ObjectMapper().writeValueAsString(ok));
                    out.flush();
                    out.close();
                }
            })
            .failureHandler(new AuthenticationFailureHandler() {//登录失败的操作
                @Override
                public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse rep, AuthenticationException e) throws IOException, ServletException {
                    rep.setContentType("application/json;charset=utf-8");
                    PrintWriter out = rep.getWriter();
                    RespBean error = RespBean.error("登录失败!");
                    if(e instanceof LockedException){
                        error.setMsg("账户被锁定，请联系管理员");
                    }else if(e instanceof CredentialsExpiredException){
                        error.setMsg("密码过期，请联系管理员");
                    }else if(e instanceof AccountExpiredException){
                        error.setMsg("账户过期，请联系管理员");
                    }else if(e instanceof DisabledException){
                        error.setMsg("账户禁用，请联系管理员");
                    }else if(e instanceof BadCredentialsException){
                        error.setMsg("用户名或密码输入错误，请重新输入");
                    }
                    out.write(new ObjectMapper().writeValueAsString(error));
                    out.flush();
                    out.close();
                }
            })
            .permitAll()
            .and()
            .logout()
            .logoutSuccessHandler(new LogoutSuccessHandler() {//注销成功的操作
                @Override
                public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse rep, Authentication authentication) throws IOException, ServletException {
                    rep.setContentType("application/json;charset=utf-8");
                    PrintWriter out = rep.getWriter();
                    out.write(new ObjectMapper().writeValueAsString(RespBean.ok("注销成功!")));
                    out.flush();
                    out.close();
                }
            })
            .permitAll()
            .and()
            .csrf().disable()
             //.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .exceptionHandling()
            //请求失败时，在这里处理结果，不要重定向
            .authenticationEntryPoint((request, rep, e) -> {
                rep.setContentType("application/json;charset=utf-8");
                rep.setStatus(401);
                PrintWriter out = rep.getWriter();
                RespBean error = RespBean.error("访问失败!");
                if(e instanceof InsufficientAuthenticationException){
                    error.setMsg("请求失败，请联系管理员！");
                }
                out.write(new ObjectMapper().writeValueAsString(error));
                out.flush();
                out.close();
            });
//        http.addFilterBefore(new JwtLoginFilter("/doLogin", authenticationManager()), UsernamePasswordAuthenticationFilter.class);
//        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
