package com.hmdp.controller;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private IUserService userService;
    @GetMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session)
    {
        return userService.sendCode(phone,session);
    }
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session)
    {
        return userService.login(loginForm,session);
    }

    /**
     * 查询当前登录用户的信息
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 用户登出功能
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        /*
         *
         * 把token从Redis中给拿掉
         * 把用户信息从thread里面拿开
         * */


        return userService.logout(request);
    }
}
