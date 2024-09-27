package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/*
* 这里是没有加注释的,这只是自己创建的一个类,在mvcConfig里面需要new一个对象,才可以
* 在fmt项目里面直接加上的是@Configuration,才可以
* */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(UserHolder.getUser() == null)
        {
            /*
            * 401代表没有权限
            * */
            response.setStatus(401);
            return false;
        }
        return true;
    }

}
