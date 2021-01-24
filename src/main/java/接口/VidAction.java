package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.VidService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * 类描述：补充VID，解决VID缺失问题
 * @author zhaowenkai
 * @Date 2018年7月3日
 * @modify
 * 修改记录：
 *
 */
public class VidAction extends CIVAction {

	@Autowired
	private VidService vidService;

	/**
	 * @Description
	 * 补充VID，解决VID缺失问题
	 */
	public void vidProcess() {
		//参数：患者编号   就诊类型：01-门诊   02-住院
		Map<String, Object> result = new HashMap<String, Object>();
		//补充vid暂时注释掉
		// Map<String, Object> result = vidService.vidProcess(patientId, visitType);
		renderJson(JsonUtil.getJSONString(result));
	}
}
