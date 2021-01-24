package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.SpecialtyPowerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：患者专科视图权限设置
 * @author zhangsen
 * @Date 2019年4月11日
 * @modify
 * 修改记录：
 *
 */
public class SpecialtyPowerAction extends CIVAction{

	private static final long serialVersionUID = 1L;

	@Autowired
	private SpecialtyPowerService specialtyPowerService;


	/**
	 * @Description
	 * 获取用户科室列表
	 */
	public void getDeptList() {
		String userCode = getParameter("userCode");

		List<Map<String, String>> map=specialtyPowerService.getDeptList(userCode);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 获取用户科室权限
	 */
	public void getPowerConfigByDept() {
		String userCode = getParameter("userCode");
		String deptCode = getParameter("deptCode");
		List<Map<String, Object>> map=specialtyPowerService.getPowerConfigByDept(userCode,deptCode);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 修改用户科室权限
	 */
	public void updatePowerConfigByDept() {
		String userCode = getParameter("userCode");
		String deptCode = getParameter("deptCode");
		String Exam = getParameter("Exam");
		String labCodes = getParameter("labCodes");
		String labNames = getParameter("labNames");
		String drugCodes = getParameter("drugCodes");
		String drugNames = getParameter("drugNames");

		Map<String, String> map=new HashMap<String, String>();
		map.put("Exam", Exam);
		map.put("labCodes", labCodes);
		map.put("labNames", labNames);
		map.put("drugCodes", drugCodes);
		map.put("drugNames", drugNames);

		Map<String, String> rs=specialtyPowerService.updatePowerConfigByDept(userCode, deptCode, map);
		renderJson(JsonUtil.getJSONString(rs));
	}

}
