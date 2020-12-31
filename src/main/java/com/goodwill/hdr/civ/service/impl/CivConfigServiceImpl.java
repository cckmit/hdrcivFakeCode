package com.goodwill.hdr.civ.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.goodwill.hdr.civ.entity.CivConfig;
import com.goodwill.hdr.civ.mapper.CivConfigMapper;
import com.goodwill.hdr.civ.service.CivConfigService;
import org.springframework.stereotype.Service;

/**
 * 处理配置项的相关
 * @author 余涛
 * @date 2020/12/27
 */

@Service
public class CivConfigServiceImpl extends ServiceImpl<CivConfigMapper,CivConfig> implements CivConfigService {

}
