package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop")
public class ShopController {
    @Resource
    private IShopService iShopService;
    @GetMapping("/{id}")
    public Result  queryShopById(@PathVariable("id") Long id)
    {

        return iShopService.queryById(id);

    }

}
