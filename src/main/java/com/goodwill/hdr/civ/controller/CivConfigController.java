package com.goodwill.hdr.civ.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goodwill.hdr.civ.entity.CivConfig;
import com.goodwill.hdr.civ.global.CivResult;
import com.goodwill.hdr.civ.service.CivConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 处理civ配置项相关请求
 *
 * @author 余涛
 * @date 2020/12/27
 */
@RestController
@RequestMapping("/config")
public class CivConfigController {
    @Autowired
    private CivConfigService civConfigService;

    /**
     * 获取配置项
     */
    public CivResult<Page<CivConfig>> getConfigs() {
        return null;

    }

    /**
     * 保存修改的配置
     */
    public CivResult saveConfigs() {
        return null;

    }

    /**
     * 获取配置项分组
     */
    public CivResult<List<String>> getConfigGroupName() {
        return null;
    }

}
