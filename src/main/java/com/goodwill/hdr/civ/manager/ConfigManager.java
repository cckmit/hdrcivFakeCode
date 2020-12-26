package com.goodwill.hdr.civ.manager;

import com.goodwill.hdr.civ.entity.CivConfig;
import com.goodwill.hdr.civ.mapper.ConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 操作配置缓存的工具类
 *
 * @author 余涛
 * @date 2020/12/25
 */
@Component
public class ConfigManager {

    @Autowired
    private ConfigMapper configMapper;

    /**
     * 从缓存中查询配置
     * <p>
     * 如果缓存中没有配置项，则从mysql中查询配置项并放入缓存
     * </p>
     *
     * @param configCode 配置项的英文代码
     * @return 配置项的值
     */
    public  String getConfigFromCache(String configCode) {
        //查询缓存中是否有配置项
        String value = CivConfig.getConfigCache().get(configCode);
        if (value == null) {

        configMapper.selectOne()
        }
        return value;
    }

    /**
     * 向缓存中更新或增加配置
     *
     * @param key
     * @param value
     */
    public  void setConfigInCache(String key, String value) {
        configCache.put(key, value);
    }

    /**
     * 重新读取mysql,刷新配置
     */

    public static void reloadConfigCache() {
        List<ConfigEntity> list = ConfigManager.getConfigBycode(key);
        for (ConfigEntity entity : list) {
            value = entity.getConfigValue();
            if ("null" == value || null == value) {
                value = "";
            }
            setCache(key, value);
        }
    } else

    {
        return value;
    }


}
