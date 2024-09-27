package com.hmdp.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 验证手机是否符合规则,这里很狗的是这里的校验是反着来的
        if(RegexUtils.isPhoneInvalid(phone))
        {
            return Result.fail("手机格式不正确");

        }
        //2. 生成短信验证码
        String code = RandomUtil.randomNumbers(6);
        //3.将验证码放到Redis中去
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
// 4. 打印
        log.debug("验证码发送成功: { }"+code);
        log.info("验证码发送成功: { }"+code);
        return Result.ok();

    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();

        //1. 验证手机号码,防止用户中间再改
        if(RegexUtils.isPhoneInvalid(phone))
        {
            return Result.fail("手机号码格式不对");
        }
        //2. 比较验证码
        String reidsCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+ phone);
        String code = loginForm.getCode();
        if(StringUtils.isEmpty(code))
        {
            return Result.fail("验证码不能为空");
        }
        if(reidsCode == null || !code.equals(reidsCode) )
        {
            return Result.fail("验证码错误");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone",phone);
        User user = userService.getOne(queryWrapper);
        /*
        * 3. 如果用户不存在就直接创建一个
        * */
        if(user == null)
        {
            user = createUserByphone(phone);
        }
        /*
        * 4. 将登录用户封装后,传入Redis
        * 这里传入true会UUID对象转换成字符串后连带下划线都加进去
        * */
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);


    }
    @Override
    public Result logout(HttpServletRequest request) {
        /*
         * UserDto 包含id nickname属性 ,和头像
         * */
        String name = UserHolder.getUser().getNickName();
        LocalDateTime localDateTime = LocalDateTime.now();
        /*
         * 获取当前用户携带请求所携带的token
         * */
        String token = request.getHeader("authorization");

        String loginToken = LOGIN_USER_KEY + token;
        /*
         * 1. 移除ThreadLocal中的用户信息
         * */
        UserHolder.removeUser();
        /*
         * 2. 移除Redis中的token
         * */
        stringRedisTemplate.delete(loginToken);
        log.info(localDateTime +"{"+name+"}"+"成功登出");
        return Result.ok();

    }

    private User createUserByphone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
