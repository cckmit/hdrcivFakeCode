package com.goodwill.hdr.civ.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goodwill.hdr.civ.entity.CivConfig;
import org.springframework.stereotype.Repository;

/**
 * 连接mysql数据库，加载配置项
 * @author yutao
 * @date 2020/12/25
 */
@Repository
public interface CivConfigMapper extends BaseMapper<CivConfig> {
}
