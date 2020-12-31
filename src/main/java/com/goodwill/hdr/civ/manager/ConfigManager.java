package com.goodwill.hdr.civ.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.goodwill.hdr.civ.entity.CivConfig;
import com.goodwill.hdr.civ.mapper.CivConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 操作配置缓存的工具类
 *
 * @author 余涛
 * @date 2020/12/25
 */
// TODO: 2020/12/27 思考事务处理 
@Component
public class ConfigManager {

    @Autowired
    private CivConfigMapper civConfigMapper;

    /**
     * 已知configCode,从缓存中查询配置
     * <p>
     * 如果缓存中没有配置项，则从mysql中查询配置项并放入缓存
     * </p>
     *
     * @param configCode 配置项的英文代码
     * @return 配置项的值
     */
    public String getConfigFromCache(String configCode) {
        //查询缓存中是否有配置项
        String value = CivConfig.getConfigCache().get(configCode);
        // 缓存为空时，从mysql中查询configCode对应的配置项
        if (value == null) {
            CivConfig civConfig = civConfigMapper.selectOne(new QueryWrapper<CivConfig>().
                    select("config_value").
                    eq("is_active", "Y").
                    eq("config_code", configCode));
            value = civConfig.getConfigValue();
            //将配置项写入缓存中
            CivConfig.getConfigCache().put(configCode, value);
        }
        return value;
    }

    /**
     * 增加配置
     *
     * @param key
     * @param value
     */
    // TODO: 2020/12/27 完善增加配置
    public void addConfigValue(String key, String value) {
        
    }

    /**
     * 重新读取mysql,刷新缓存
     */
// TODO: 2020/12/27  完善刷新缓存
    public static void reloadConfigCache() {
       
    }


}
