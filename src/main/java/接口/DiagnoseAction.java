package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.DiagnoseService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：诊断
 * @modify
 * 修改记录：
 *
 */
public class DiagnoseAction  extends CIVAction {

	private static final long serialVersionUID = 1L;
	
	@Autowired
	private DiagnoseService diagnoseService;
	
	/**
	 * @Description
	 * 获得所有的诊断记录
	 */
	public void getDiagsList() {
		String visitType = getParameter("visitType");
		//患者编号  就诊次  就诊类型
		List<Map<String, Object>> result = diagnoseService.getDiagsList(patientId,visitType);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}
	
}
