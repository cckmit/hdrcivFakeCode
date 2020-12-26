package com.goodwill.hdr.civ.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 对应配置文件的实体类
 *
 * @author 余涛
 * @date 2020/12/25
 */
@TableName("civ_config")
public class CivConfig {

    /**
     * 利用hashmap做缓存，存储配置项
     */
    @TableField(exist = false)
    private static final Map<String, String> CONFIG_CACHE = new HashMap<String, String>();
    /**
     * 对应配置项的数据库id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 配置项的英文代码，如：CIV_VISIT_VALUE
     */
    @TableField
    private String configCode;
    /**
     * 配置项的中文名称，如：权限设置—就诊视图
     */
    @TableField
    private String configName;
    /**
     * 配置项的值
     */
    @TableField
    private String configValue;
    /**
     * 配置项的中文描述
     */
    @TableField
    private String configDescription;
    /**
     * 配置项的创建时间
     */
    @TableField
    private Date createTime;
    /**
     * 配置项的最后修改时间
     */
    @TableField
    private Date lastUpdateTime;
    /**
     * 配置项是否生效，true表示生效，false表示无效
     */
    @TableField
    private Boolean isActive;


    //自动生成常规属性的get、set方法
    public static Map<String, String> getConfigCache() {
        return CONFIG_CACHE;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConfigCode() {
        return configCode;
    }

    public void setConfigCode(String configCode) {
        this.configCode = configCode;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigDescription() {
        return configDescription;
    }

    public void setConfigDescription(String configDescription) {
        this.configDescription = configDescription;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

