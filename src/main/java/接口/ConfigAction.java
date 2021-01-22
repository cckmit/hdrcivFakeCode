package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

/**
 * @author guozhenlei
 * @Description 类描述：
 * @modify 修改记录：
 */
public class ConfigAction extends CIVAction {
    @Autowired
    private ConfigService configService;

    /**
     * 获取配置项
     */
    public void getConfigs() {
        String keyWord = getParameter("keyWord");
        String configScopePage = getParameter("configScopePage");
        Page<Map<String,Object>> page  =  configService.queryConfigs(keyWord, configScopePage, pageNo, pageSize);
//        Map<String, List<ConfigEntity>> configsMap = configService.queryConfigs(keyWord, configScopePage, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 保存修改的配置
     */
    public void saveConfig() {
        String id = getParameter("id");
        String value = getParameter("value");
        Map<String,String> map =  configService.updateOrSaveConfig(id, value);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取配置项分类
     */
    public  void getConfigType(){
        Set<String> set  =  configService.queryAllConfigType();
        renderJson(JsonUtil.getJSONString(set));
    }





}
