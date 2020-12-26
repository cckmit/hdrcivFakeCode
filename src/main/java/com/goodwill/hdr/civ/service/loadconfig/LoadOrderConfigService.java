package com.goodwill.hdr.civ.service.loadconfig;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 加载医嘱配置项文件
 *
 * @author yutao
 * @date 2020/12/25
 */
@Service
public class LoadOrderConfigService {
    //利用hashmap做缓存，存储配置项
    private static Map<String, String> cache = new HashMap<String, String>();


}


