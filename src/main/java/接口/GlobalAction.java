package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.GlobalService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @Description
 * 类描述：全局配置Action
 * @author zhaowenkai
 * @Date 2018年7月9日
 * @modify
 * 修改记录：
 *
 */
public class GlobalAction extends CIVAction {

	private static final long serialVersionUID = 1972056655268730996L;

	@Autowired
	private GlobalService globalService;

	/**
	 * @Description
	 * 是否启用医嘱闭环
	 */
	public void ifUseOCL() {
		Map<String, Object> result = globalService.ifUseOCL();
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 是否隐藏患者敏感信息
	 */
	public void ifHidePKM() {
		Map<String, Object> result = globalService.ifHidePKM();
		renderJson(JsonUtil.getJSONString(result));
	}


}
